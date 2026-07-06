package jp.takke.cpustats

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Binder
import android.os.IBinder
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import jp.takke.cpustats.util.MyUtil
import jp.takke.cpustats.widget.CpuWidget
import jp.takke.cpustats.widget.WidgetData
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

  companion object {

    // 常駐停止指示のアクション(クイック設定タイル等から使用)
    const val ACTION_STOP_RESIDENT = "jp.takke.cpustats.action.STOP_RESIDENT"

    // 設定再読込指示のアクション(通知アクションから ConfigActivity を開いた場合等に使用)
    const val ACTION_RELOAD_SETTINGS = "jp.takke.cpustats.action.RELOAD_SETTINGS"

    // 常駐中フラグ(クイック設定タイルの状態表示用。同一プロセス内でのみ参照可能)
    @Volatile
    var isResidentRunning = false
      private set
  }

  // 設定値(reloadSettings で新しいインスタンスに差し替える)
  @Volatile
  private var mConfig = MyConfig()

  // 通知管理クラス
  private val mNotificationPresenter = NotificationPresenter(this)

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

  // CPU使用率(全体平均)の履歴(スパークライン用)
  private val mUsageHistory = UsageHistoryBuffer()

  // 履歴記録の前回記録時刻[ms]
  private var mLastRecordTime = 0L

  // ホーム画面ウィジェットの前回更新時刻[ms]
  private var mLastWidgetUpdateTime = 0L

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
   * CPU使用率(全体平均)の履歴を取得する(古い→新しい順)
   */
  fun getUsageHistory(): IntArray = mUsageHistory.toArray()

  /**
   * 常駐開始
   */
  fun startResident() {

    // 常駐停止フラグを解除
    mStopResident = false
    isResidentRunning = true

    // 収集ループ開始
    startGatherLoop()
  }

  /**
   * 設定のリロード
   */
  fun reloadSettings() {

    MyLog.d("reloadSettings")
    mConfig = MyConfig.load(this)

    // 設定で消された通知を消去
    mNotificationPresenter.cancelNotifications(mConfig)
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

    isResidentRunning = true

    // 設定のロード
    mConfig = MyConfig.load(this)

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

    // クイック設定タイル等からの常駐停止指示
    if (intent?.action == ACTION_STOP_RESIDENT) {
      MyLog.i("UsageUpdateService.onStartCommand[stop resident]")
      stopResident()
      return START_NOT_STICKY
    }

    // 設定再読込指示(常駐状態には影響させない)
    if (intent?.action == ACTION_RELOAD_SETTINGS) {
      MyLog.i("UsageUpdateService.onStartCommand[reload settings]")
      reloadSettings()
      return START_STICKY
    }

    // 常駐開始(停止後の再開を含む)
    mStopResident = false
    isResidentRunning = true

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

    // 履歴に追加(通知更新の有無に関わらず毎回記録する)
    mUsageHistory.add(cpuUsages[0])


    //-------------------------------------------------
    // バッテリー温度・メモリ使用量の取得
    //-------------------------------------------------
    val systemStats = getSystemStats()

    //-------------------------------------------------
    // 履歴記録(間引きあり)
    //-------------------------------------------------
    recordUsageLogIfNeeded(cpuUsages[0], currentCpuClock, systemStats)

    //-------------------------------------------------
    // ホーム画面ウィジェットの更新(間引きあり)
    //-------------------------------------------------
    updateWidgetIfNeeded(cpuUsages[0], currentCpuClock, systemStats.batteryTemperature)

    //-------------------------------------------------
    // 通知判定
    //-------------------------------------------------
    // 前回と同じ CPU 使用率なら通知しない
    // ※通知アイコンだけなら丸めた値で比較すればいいんだけど通知テキストにCPU使用率%が出るので完全一致で比較する
    // ※メモリ使用量は変動が細かすぎるので通知更新判定には含めない
    val updated = isUpdated(currentCpuClock, cpuUsages, systemStats.batteryTemperature)
    mLastCpuUsages = cpuUsages
    mLastCpuClock = currentCpuClock
    mLastBatteryTemperature = systemStats.batteryTemperature

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
        fi,
        systemStats,
        mRequestForeground,
        mConfig
      )
      mRequestForeground = false

      // コールバック経由で通知
      distributeToCallbacks(cpuUsages, fi, systemStats)
    }

    mLastExecTask = System.currentTimeMillis()
  }

  /**
   * ホーム画面ウィジェットを更新する(前回更新から一定時間経過し、表示値に変化がある場合のみ)
   *
   * WidgetData は Compose snapshot state なので、Glance セッションが生きている間は
   * update() の書き込みだけで再コンポーズされる。updateAll() はセッションが閉じている
   * 場合の再起動用(生きている場合は軽量なイベント送信で済む)。
   */
  private fun updateWidgetIfNeeded(cpuUsage: Int, cpuClock: Int, batteryTemperature: Int) {

    val now = System.currentTimeMillis()
    if (now - mLastWidgetUpdateTime < C.WIDGET_UPDATE_MIN_INTERVAL_MS) {
      return
    }

    // 表示値に変化がなければ更新しない
    val newSnapshot = WidgetData.Snapshot(cpuUsage, cpuClock, batteryTemperature)
    if (newSnapshot == WidgetData.snapshot()) {
      return
    }

    mLastWidgetUpdateTime = now
    WidgetData.update(cpuUsage, cpuClock, batteryTemperature)

    mServiceScope.launch {
      try {
        val manager = GlanceAppWidgetManager(this@UsageUpdateService)
        if (manager.getGlanceIds(CpuWidget::class.java).isNotEmpty()) {
          CpuWidget().updateAll(this@UsageUpdateService)
        }
      } catch (e: Exception) {
        MyLog.e(e)
      }
    }
  }

  /**
   * 履歴を記録する(前回記録から一定時間経過している場合のみ)
   */
  private fun recordUsageLogIfNeeded(
    cpuUsage: Int,
    currentCpuClock: Int,
    systemStats: SystemStats
  ) {

    if (!mConfig.recordHistory) {
      return
    }

    val now = System.currentTimeMillis()
    if (now - mLastRecordTime < C.HISTORY_RECORD_INTERVAL_MS) {
      return
    }
    mLastRecordTime = now

    UsageLogDatabase.getInstance(this).insertAndPurge(
      UsageLogRow(
        recordedAt = now,
        cpuUsage = cpuUsage,
        cpuClock = currentCpuClock,
        batteryTemperature = systemStats.batteryTemperature,
        memUsedMb = systemStats.memUsedMb,
      )
    )
  }

  /**
   * バッテリー温度・メモリ使用量を取得する
   */
  private fun getSystemStats(): SystemStats {

    // ACTION_BATTERY_CHANGED は sticky broadcast なので受信登録なしで現在値を取得できる
    val batteryIntent = applicationContext.registerReceiver(
      null,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
    val batteryTemperature =
      batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0

    // メモリ使用量
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo()
    am.getMemoryInfo(mi)

    return SystemStats(
      batteryTemperature = batteryTemperature,
      memUsedMb = ((mi.totalMem - mi.availMem) / 1024 / 1024).toInt(),
      memTotalMb = (mi.totalMem / 1024 / 1024).toInt(),
    )
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
    systemStats: SystemStats
  ) {

    for (callback in mCallbacks) {
      callback.onUsageUpdated(cpuUsages, fi.freqs, fi.minFreqs, fi.maxFreqs, systemStats)
    }
  }

  override fun onDestroy() {

    MyLog.i("UsageUpdateService.onDestroy")

    isResidentRunning = false

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
    isResidentRunning = false

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
