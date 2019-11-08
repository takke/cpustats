package jp.takke.cpustats

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.RemoteException
import android.os.SystemClock
import java.util.*


class UsageUpdateService : Service() {

    // 設定値
    private val mConfig = MyConfig()

    // 通知管理クラス
    private val mNotificationPresenter = NotificationPresenter(this, mConfig)

    // 常駐停止フラグ
    private var mStopResident = false

    // (Android 8.0 以降用) BOOT_COMPLETED から startForeground を実行するためのフラグ
    private var mRequestForeground = false

    // スリープ中フラグ
    private var mSleeping = false

    // 前回の CPU クロック周波数
    private var mLastCpuClock = -1

    // 前回の収集データ
    private var mLastCpuUsageSnapshot: ArrayList<OneCpuInfo>? = null

    // 前回のCPU使用率
    private var mLastCpuUsages: IntArray? = null

    // CPU使用率を各コアの周波数から算出するモード(Android 8.0以降用)
    private var mUseFreqForCpuUsage = false

    // コールバック一覧
    private val mCallbackList = RemoteCallbackList<IUsageUpdateCallback>()

    // コールバック数キャッシュ
    private var mCallbackListSize = 0

    private var mLastExecTask = System.currentTimeMillis()

    // 通信量取得スレッド管理
    private var mThread: GatherThread? = null
    private var mThreadActive = false

    // サービスの実装
    private val mBinder = object : IUsageUpdateService.Stub() {

        @Throws(RemoteException::class)
        override fun registerCallback(callback: IUsageUpdateCallback) {

            // コールバックリスト登録
            mCallbackList.register(callback)

            mCallbackListSize++
        }

        @Throws(RemoteException::class)
        override fun unregisterCallback(callback: IUsageUpdateCallback) {

            // コールバックリスト解除
            mCallbackList.unregister(callback)

            if (mCallbackListSize > 0) {
                mCallbackListSize--
            }
        }

        @Throws(RemoteException::class)
        override fun stopResident() {

            // 常駐停止
            this@UsageUpdateService.stopResident()
        }

        @Throws(RemoteException::class)
        override fun startResident() {

            // 常駐停止フラグを解除
            mStopResident = false

            // すぐに次の onStart を呼ぶ
            this@UsageUpdateService.scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC.toLong())

            if (mThread == null) {
                startThread()
            }
        }

        /**
         * 設定のリロード
         */
        @Throws(RemoteException::class)
        override fun reloadSettings() {

            MyLog.d("reloadSettings")
            mConfig.loadSettings(this@UsageUpdateService)

            // 設定で消された通知を消去
            mNotificationPresenter.cancelNotifications()
        }
    }

    /**
     * スリープ状態(SCREEN_ON/OFF)の検出用レシーバ
     */
    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return

            if (action == Intent.ACTION_SCREEN_ON) {

                MyLog.d("screen on")

                // 停止していれば再開する
                mSleeping = false

                // 次の onStart を呼ぶ
                // 但し、「通知時刻」はすぐに更新するとステータスバーの順序がいきなり変わってしまってうざいので少し遅延させる
                mNotificationPresenter.mNotificationTimeKeep = System.currentTimeMillis() + 30 * 1000

                this@UsageUpdateService.scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC.toLong())

                // スレッド開始
                if (!mStopResident) {
                    startThread()
                }

            } else if (action == Intent.ACTION_SCREEN_OFF) {

                MyLog.d("screen off")

                // 停止する
                mSleeping = true

                // アラーム停止
                stopAlarm()

                // スレッド停止
                stopThread()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {

        MyLog.i("UsageUpdateService.onBind")

        // スレッド開始
        startThread()

        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {

        MyLog.i("UsageUpdateService.onUnbind")

        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {

        MyLog.i("UsageUpdateService.onRebind")

        super.onRebind(intent)
    }

    override fun onCreate() {
        super.onCreate()

        MyLog.i("UsageUpdateService.onCreate")

        // 設定のロード
        mConfig.loadSettings(this)

        if (mLastCpuUsageSnapshot == null) {
            mLastCpuUsageSnapshot = CpuInfoCollector.takeCpuUsageSnapshot()
        }

        // スリープ状態のレシーバ登録
        applicationContext.registerReceiver(mReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        applicationContext.registerReceiver(mReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        // 定期処理開始
        scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC.toLong())

        // スレッド開始
        startThread()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        mRequestForeground = intent != null && intent.getBooleanExtra("FOREGROUND_REQUEST", false)
        MyLog.i("UsageUpdateService.onStartCommand[$mRequestForeground]")

        // 通信量取得スレッド開始
        if (mThread == null) {
            startThread()
        }

        if (mRequestForeground) {
            // 5秒以内に setForeground を実行する必要があるので早めに実行する
            // (すぐ実行すると PreviewActivity の bind に間に合わないのでちょっとだけ遅延する)
            Handler().postDelayed({
                execTask()
            }, C.ALARM_STARTUP_DELAY_MSEC.toLong())
        }

        // Alarmループ続行
        scheduleNextTime(C.ALARM_INTERVAL_MSEC.toLong())

        return START_STICKY
    }

    private fun execTask() {

        //-------------------------------------------------
        // CPU クロック周波数の取得
        //-------------------------------------------------
        val fi = AllCoreFrequencyInfo(CpuInfoCollector.calcCpuCoreCount())
        CpuInfoCollector.takeAllCoreFreqs(fi)

        val activeCoreIndex = MyUtil.getActiveCoreIndex(fi.freqs)
        val currentCpuClock = fi.freqs[activeCoreIndex]

        // CPU クロック周波数のmin/max
        val minFreq = fi.minFreqs[activeCoreIndex]
        val maxFreq = fi.maxFreqs[activeCoreIndex]

        if (MyLog.debugMode) {
            MyLog.d("* CPU: " + currentCpuClock + " [" + minFreq + "," + maxFreq + "] [" + (System.currentTimeMillis() - mLastExecTask) + "ms]")
        }


        //-------------------------------------------------
        // CPU 使用率の取得
        //-------------------------------------------------
        // CPU 使用率の snapshot 取得
        var cpuUsages: IntArray? = null
        if (!mUseFreqForCpuUsage) {
            val currentCpuUsageSnapshot = CpuInfoCollector.takeCpuUsageSnapshot()
            if (currentCpuUsageSnapshot != null) {
                // CPU 使用率の算出
                cpuUsages = MyUtil.calcCpuUsages(currentCpuUsageSnapshot, mLastCpuUsageSnapshot)
                // 今回の snapshot を保存
                mLastCpuUsageSnapshot = currentCpuUsageSnapshot
            } else {
                // 取得できないので fallback する
                mUseFreqForCpuUsage = true
            }
        }
        if (cpuUsages == null) {
            // fallbackモード(Android 8.0以降では /proc/stat にアクセスできないのでコアの周波数からCPU使用率を算出する)
            cpuUsages = MyUtil.calcCpuUsagesByCoreFrequencies(fi)
        }


        //-------------------------------------------------
        // 通知判定
        //-------------------------------------------------
        // 前回と同じ CPU 使用率なら通知しない
        // ※通知アイコンだけなら丸めた値で比較すればいいんだけど通知テキストにCPU使用率%が出るので完全一致で比較する
        val updated = isUpdated(currentCpuClock, cpuUsages)
        mLastCpuUsages = cpuUsages
        mLastCpuClock = currentCpuClock

        //-------------------------------------------------
        // 通知
        //-------------------------------------------------
        if (!updated) {
            if (MyLog.debugMode) {
                MyLog.d("- skipping caused by no diff.")
            }
        } else {
            // ステータスバーの通知
            mNotificationPresenter.updateNotifications(cpuUsages, currentCpuClock, minFreq, maxFreq, mRequestForeground)
            mRequestForeground = false

            // コールバック経由で通知
            distributeToCallbacks(cpuUsages, fi)
        }

        mLastExecTask = System.currentTimeMillis()
    }

    private fun isUpdated(currentCpuClock: Int, cpuUsages: IntArray?): Boolean {

        if (mLastCpuClock != currentCpuClock) {
            return true
        } else if (mLastCpuUsages == null || cpuUsages == null) {
            return true
        } else if (cpuUsages.size != mLastCpuUsages!!.size) {
            // コア数変動のケア(Galaxy S II等でよくあるらしい)
            return true
        } else {
            // 同一でない値があれば更新する
            val n = cpuUsages.size
            for (i in 0 until n) {
                if (cpuUsages[i] != mLastCpuUsages!![i]) {
                    return true
                }
            }
        }
        return false
    }

    private fun distributeToCallbacks(cpuUsages: IntArray, fi: AllCoreFrequencyInfo) {

        if (mCallbackListSize >= 1) {
            try {
                val n = mCallbackList.beginBroadcast()

                // コールバック数を念のため更新しておく
                mCallbackListSize = n

//              if (MyLog.debugMode) {
//                  MyLog.d("- broadcast:" + n);
//              }

                // 全コアのCPU周波数を収集する
                for (i in 0 until n) {
                    try {
                        mCallbackList.getBroadcastItem(i).updateUsage(cpuUsages,
                                fi.freqs, fi.minFreqs, fi.maxFreqs)
                    } catch (e: RemoteException) {
//                      MyLog.e(e);
                    }

                }
                mCallbackList.finishBroadcast()
            } catch (e: IllegalStateException) {
                MyLog.e(e)
            }
        }
    }

    override fun onDestroy() {

        MyLog.i("UsageUpdateService.onDestroy")

        stopAlarm()

        // 通信量取得スレッド停止
        stopThread()

        // スリープ状態のレシーバ解除
        applicationContext.unregisterReceiver(mReceiver)

        // 通知を消す
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()

        super.onDestroy()
    }

    /**
     * サービスの次回の起動を予約
     */
    fun scheduleNextTime(intervalMs: Long) {

        if (mStopResident) {
            MyLog.d("scheduleNextTime: サービス終了の指示が出ているので次回の予約はしない")
            return
        }
        if (mSleeping) {
            MyLog.d("scheduleNextTime: sleeping")
            return
        }

        val now = System.currentTimeMillis()

        // アラームをセット
        val intent = Intent(this, this.javaClass)
        val alarmSender = PendingIntent.getService(
                this,
                0,
                intent,
                0
        )
        // ※onStartCommandが呼ばれるように設定する

        val am = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        am.set(AlarmManager.RTC, now + intervalMs, alarmSender)

        MyLog.d("-- scheduled[" + intervalMs + "ms]")
    }

    /**
     * 常駐停止
     */
    fun stopResident() {

        mStopResident = true

        // スレッド停止
        stopThread()

        // アラーム停止
        stopAlarm()

        // 通知を消す
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancelAll()

        // 常駐停止
        stopSelf()
    }

    /**
     * アラームの停止
     */
    private fun stopAlarm() {

        // サービス名を指定
        val intent = Intent(this, this.javaClass)

        // アラームを解除
        val pendingIntent = PendingIntent.getService(
                this,
                0, // ここを-1にすると解除に成功しない
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent)
        // @see http://creadorgranoeste.blogspot.com/2011/06/alarmmanager.html
    }

    private fun startThread() {

        if (mThread == null) {
            mThread = GatherThread()
            mThreadActive = true
            mThread?.start()
            MyLog.i("UsageUpdateService.startThread: thread start")
        } else {
            MyLog.i("UsageUpdateService.startThread: already running")
        }
    }

    private fun stopThread() {

        if (mThreadActive && mThread != null) {
            MyLog.d("UsageUpdateService.stopThread")

            mThreadActive = false
            while (true) {
                try {
                    mThread!!.join()
                    break
                } catch (ignored: InterruptedException) {
                    MyLog.e(ignored)
                }

            }
            mThread = null
        } else {
            MyLog.d("UsageUpdateService.stopThread: no thread")
        }
    }

    /**
     * 通信量取得スレッド
     */
    private inner class GatherThread : Thread() {

        override fun run() {

            MyLog.i("UsageUpdateService.GatherThread: start")

            while (mThread != null && mThreadActive) {

                SystemClock.sleep(mConfig.intervalMs)

                if (mThreadActive && !mStopResident) {
                    execTask()
                }
            }

            MyLog.i("UsageUpdateService.GatherThread: done")
        }
    }
}
