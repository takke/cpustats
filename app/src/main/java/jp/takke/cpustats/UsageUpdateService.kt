package jp.takke.cpustats

import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import jp.takke.cpustats.util.MyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList


class UsageUpdateService : Service() {

  // 設定値
  private val mConfig = MyConfig()

  // 通知管理クラス
  private val mNotificationPresenter = NotificationPresenter(this, mConfig)

  // 常駐停止フラグ
  private var mStopResident = false

  // (Android 8.0 以降用) BOOT_COMPLETED から startForeground を実行するためのフラグ
  private var mRequestForeground = false

  // 前回の CPU クロック周波数
  private var mLastCpuClock = -1

  // 前回のバッテリー温度(0.1℃単位)
  private var mLastBatteryTemperature = 0

  // 前回の収集データ
  private var mLastCpuUsageSnapshot: ArrayList<OneCpuInfo>? = null

  // 前回のCPU使用率
  private var mLastCpuUsages: IntArray? = null

  // CPU使用率を各コアの周波数から算出するモード(Android 8.0以降用)
  private var mUseFreqForCpuUsage = false

  // コールバック一覧
  private val mCallbacks = CopyOnWriteArrayList<UsageUpdateCallback>()

  private var mLastExecTask = System.currentTimeMillis()

  // 収集ループ管理(ジョブの起動・停止はメインスレッドからのみ行う)
  private val mServiceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var mGatherJob: Job? = null

  // ローカルバインダ(同一プロセス内なので Service インスタンスを直接渡す)
  inner class LocalBinder : Binder() {
    val service: UsageUpdateService
      get() = this@UsageUpdateService
  }

  private val mBinder = LocalBinder()

  fun registerCallback(callback: UsageUpdateCallback) {

    // コールバックリスト登録
    mCallbacks.addIfAbsent(callback)
  }

  fun unregisterCallback(callback: UsageUpdateCallback) {

    // コールバックリスト解除
    mCallbacks.remove(callback)
  }

  /**
   * 常駐開始
   */
  fun startResident() {

    // 常駐停止フラグを解除
    mStopResident = false

    // 収集ループ開始
    startGatherLoop()
  }

  /**
   * 設定のリロード
   */
  fun reloadSettings() {

    MyLog.d("reloadSettings")
    mConfig.loadSettings(this)

    // 設定で消された通知を消去
    mNotificationPresenter.cancelNotifications()
  }

  /**
   * スリープ状態(SCREEN_ON/OFF)の検出用レシーバ
   */
  private val mReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val action = intent.action ?: return

      if (action == Intent.ACTION_SCREEN_ON) {

        MyLog.d("screen on")

        // 「通知時刻」はすぐに更新するとステータスバーの順序がいきなり変わってしまってうざいので少し遅延させる
        mNotificationPresenter.mNotificationTimeKeep = System.currentTimeMillis() + 30 * 1000

        // 収集ループ再開
        if (!mStopResident) {
          startGatherLoop()
        }

      } else if (action == Intent.ACTION_SCREEN_OFF) {

        MyLog.d("screen off")

        // 収集ループ停止
        stopGatherLoop()
      }
    }
  }

  override fun onBind(intent: Intent): IBinder? {

    MyLog.i("UsageUpdateService.onBind")

    // 収集ループ開始
    startGatherLoop()

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

    // 収集ループ開始
    startGatherLoop()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    super.onStartCommand(intent, flags, startId)

    mRequestForeground = intent != null && intent.getBooleanExtra("FOREGROUND_REQUEST", false)
    MyLog.i("UsageUpdateService.onStartCommand[$mRequestForeground]")

    // 収集ループ開始
    startGatherLoop()

    if (mRequestForeground) {
      // 5秒以内に setForeground を実行する必要があるので早めに実行する
      // (すぐ実行すると MainActivity の bind に間に合わないのでちょっとだけ遅延する)
      mServiceScope.launch {
        delay(C.STARTUP_DELAY_MSEC.toLong())
        execTask()
      }
    }

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
    // バッテリー温度の取得
    //-------------------------------------------------
    val batteryTemperature = getBatteryTemperature()

    //-------------------------------------------------
    // 通知判定
    //-------------------------------------------------
    // 前回と同じ CPU 使用率なら通知しない
    // ※通知アイコンだけなら丸めた値で比較すればいいんだけど通知テキストにCPU使用率%が出るので完全一致で比較する
    val updated = isUpdated(currentCpuClock, cpuUsages, batteryTemperature)
    mLastCpuUsages = cpuUsages
    mLastCpuClock = currentCpuClock
    mLastBatteryTemperature = batteryTemperature

    //-------------------------------------------------
    // 通知
    //-------------------------------------------------
    if (!updated) {
      if (MyLog.debugMode) {
        MyLog.d("- skipping caused by no diff.")
      }
    } else {
      // ステータスバーの通知
      mNotificationPresenter.updateNotifications(
        cpuUsages,
        currentCpuClock,
        minFreq,
        maxFreq,
        batteryTemperature,
        mRequestForeground
      )
      mRequestForeground = false

      // コールバック経由で通知
      distributeToCallbacks(cpuUsages, fi, batteryTemperature)
    }

    mLastExecTask = System.currentTimeMillis()
  }

  /**
   * バッテリー温度を取得する
   *
   * @return 温度(0.1℃単位、取得できない場合は0)
   */
  private fun getBatteryTemperature(): Int {

    // ACTION_BATTERY_CHANGED は sticky broadcast なので受信登録なしで現在値を取得できる
    val intent = applicationContext.registerReceiver(
      null,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
    return intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
  }

  private fun isUpdated(
    currentCpuClock: Int,
    cpuUsages: IntArray?,
    batteryTemperature: Int
  ): Boolean {

    if (mLastCpuClock != currentCpuClock) {
      return true
    } else if (mLastBatteryTemperature != batteryTemperature) {
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

  private fun distributeToCallbacks(
    cpuUsages: IntArray,
    fi: AllCoreFrequencyInfo,
    batteryTemperature: Int
  ) {

    for (callback in mCallbacks) {
      callback.onUsageUpdated(cpuUsages, fi.freqs, fi.minFreqs, fi.maxFreqs, batteryTemperature)
    }
  }

  override fun onDestroy() {

    MyLog.i("UsageUpdateService.onDestroy")

    // 収集ループ停止
    stopGatherLoop()

    // スコープ全体をキャンセル(onStartCommand の遅延 execTask などを含む)
    mServiceScope.cancel()

    // スリープ状態のレシーバ解除
    applicationContext.unregisterReceiver(mReceiver)

    // 通知を消す
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancelAll()

    super.onDestroy()
  }

  /**
   * 常駐停止
   */
  fun stopResident() {

    mStopResident = true

    // 収集ループ停止
    stopGatherLoop()

    // 通知を消す
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.cancelAll()

    // 常駐停止
    stopSelf()
  }

  /**
   * 収集ループの開始
   */
  private fun startGatherLoop() {

    if (mGatherJob?.isActive == true) {
      MyLog.i("UsageUpdateService.startGatherLoop: already running")
      return
    }

    MyLog.i("UsageUpdateService.startGatherLoop: start")
    mGatherJob = mServiceScope.launch {
      while (isActive) {

        delay(mConfig.intervalMs)

        if (!mStopResident) {
          execTask()
        }
      }
    }
  }

  /**
   * 収集ループの停止
   */
  private fun stopGatherLoop() {

    val job = mGatherJob
    if (job == null) {
      MyLog.d("UsageUpdateService.stopGatherLoop: no job")
      return
    }

    MyLog.d("UsageUpdateService.stopGatherLoop")

    // 実行中の execTask を完了させてから戻る
    // (停止直後の nm.cancelAll() より後に通知が再表示されるのを防ぐため)
    runBlocking {
      job.cancelAndJoin()
    }
    mGatherJob = null
  }
}
