package jp.takke.cpustats

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import jp.takke.cpustats.util.EdgeToEdgeUtil
import jp.takke.cpustats.util.MyUtil
import jp.takke.cpustats.util.ResourceUtil
import jp.takke.cpustats.view.SparklineView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

  // 表示確認
  private var mIsForeground = false

  // サービス実行フラグ(メニュー切り替え用。コールバックがあれば実行中と判定する)
  private var mServiceMaybeRunning = false

  // サービス
  private var mService: UsageUpdateService? = null

  // bindService 成功フラグ(bind していない状態で unbindService すると IllegalArgumentException になるため)
  private var mBound = false

  private val mConfigActivityLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      // 設定完了

      // 設定変更の内容をサービスに反映させる
      if (mService != null) {
        // 設定のリロード
        MyLog.d("request reloadSettings")
        mService!!.reloadSettings()
      } else {
        MyLog.w("cannot reloadSettings (no service i.f.)")
        // 接続後に再ロードする
      }
    }

  // コールバックの実装(サービスの収集スレッドから呼ばれるのでメインスレッドに切り替える)
  private val mCallback =
    UsageUpdateCallback { cpuUsages, freqs, minFreqs, maxFreqs, systemStats ->

      lifecycleScope.launch {

        // サービス実行中フラグを立てておく
        mServiceMaybeRunning = true

        if (mIsForeground) {
          // CPUクロック周波数表示
          showCpuFrequency(freqs, minFreqs, maxFreqs, systemStats)

          // CPU使用率表示更新
          showCpuUsages(cpuUsages, freqs, minFreqs, maxFreqs)

          // 履歴グラフ更新
          updateSparkline()
        }
      }
    }

  // サービスのコネクション
  private val mServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(name: ComponentName, service: IBinder) {

      MyLog.i("MainActivity.onServiceConnected")

      // サービスのインスタンスを取得する
      mService = (service as UsageUpdateService.LocalBinder).service

      // コールバック登録
      mService?.registerCallback(mCallback)

      // onActivityResult 後に設定がロードされていないかもしれないのでここで強制再ロードする
      mService?.reloadSettings()

      // 履歴グラフの復元(再表示時に収集済みの履歴を即座に反映する)
      updateSparkline()
    }

    override fun onServiceDisconnected(name: ComponentName) {

      MyLog.i("MainActivity.onServiceDisconnected")

      mService = null
    }
  }

  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
      if (isGranted) {
        // 通知許可リクエストで許可を選択
        MyLog.d("MainActivity: POST_NOTIFICATION: 通知許可")

      } else {
        // 通知許可リクエストで許可しないを選択
        MyLog.i("MainActivity: POST_NOTIFICATION: 通知許可しない")
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
          Toast.makeText(this, R.string.require_notification_permission, Toast.LENGTH_LONG).show()
        } else {
          // 権限を永続的に許可しない状態
          MyLog.i("MainActivity: POST_NOTIFICATION: 通知許可しない(永続的)")

          NotificationPermissionUtil.showNotificationPermissionRationaleDialog(
            this,
            onOk = {
              // OK
              MyLog.d("通知権限: OK")
            },
            onCancel = {
              // Cancel
              MyLog.d("通知権限: キャンセル")
            }
          )
        }
      }
    }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun checkNotificationPermission() {
    val notificationPermission =
      ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
    val notificationRationale =
      shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

    if (notificationPermission == PackageManager.PERMISSION_GRANTED) {
      // 既に通知許可済み

      MyLog.d("MainActivity: POST_NOTIFICATION: 通知許可済み")
      // TODO 続行すること
    } else {
      if (notificationRationale) {
        // 以前に「許可をしない」を選択済み
        MyLog.i("MainActivity: POST_NOTIFICATION: 以前に「許可をしない」を選択済み")

        NotificationPermissionUtil.showNotificationPermissionRationaleDialog(
          this,
          onOk = {
            // OK
            MyLog.d("通知権限: OK")
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          },
          onCancel = {
            // Cancel
            MyLog.d("通知権限: キャンセル")
          }
        )
      } else {
        // 通知の許可をリクエストする
        MyLog.d("MainActivity: POST_NOTIFICATION: 通知許可リクエスト")
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  public override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Edge to Edge 対応
    EdgeToEdgeUtil.optimizeEdgeToEdge(findViewById(R.id.root))

    // 通知権限の要求(Android 13 以上)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      checkNotificationPermission()
    }


    MyLog.d("MainActivity.onCreate")

    // とりあえず全消去
    hideAllCoreFreqInfo()

    // CPU クロック更新
    val coreCount = CpuInfoCollector.calcCpuCoreCount()
    val fi = AllCoreFrequencyInfo(coreCount)
    showCpuFrequency(fi.freqs, fi.minFreqs, fi.maxFreqs, SystemStats())

    // CPU使用率表示更新
    val dummyCpuUsages = IntArray(coreCount)
    for (i in dummyCpuUsages.indices) {
      dummyCpuUsages[i] = 0
    }
    CpuInfoCollector.takeAllCoreFreqs(fi)
    // CPU周波数が100%になってしまうので最初はゼロにしておく
    System.arraycopy(fi.minFreqs, 0, fi.freqs, 0, fi.freqs.size)
    showCpuUsages(dummyCpuUsages, fi.freqs, fi.minFreqs, fi.maxFreqs)

    // Toolbar初期化
    setSupportActionBar(findViewById(R.id.my_toolbar))

    // Toolbarのアイコン変更
    setActionBarLogo(R.drawable.color_single000)

    // サービス開始
    doBindService()
  }

  private fun doBindService() {

    // サービスへのバインド開始
    val serviceIntent = Intent(this, UsageUpdateService::class.java)

    // start
    MyLog.d("MainActivity: startService of UsageUpdateService")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      serviceIntent.putExtra("FOREGROUND_REQUEST", true)
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }

    // bind
    mBound = bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
  }

  private fun setActionBarLogo(iconId: Int) {

    val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
    toolbar?.setNavigationIcon(iconId)
  }

  /**
   * CPU使用率履歴グラフの更新
   */
  private fun updateSparkline() {

    val sparkline = findViewById<SparklineView?>(R.id.sparkline) ?: return
    val history = mService?.getUsageHistory() ?: IntArray(0)
    sparkline.setHistory(history, C.USAGE_HISTORY_CAPACITY)
  }

  private fun hideAllCoreFreqInfo() {

    // Core
    val cores = intArrayOf(
      R.id.core1,
      R.id.core2,
      R.id.core3,
      R.id.core4,
      R.id.core5,
      R.id.core6,
      R.id.core7,
      R.id.core8
    )
    for (i in 0..7) {
      findViewById<View>(cores[i]).visibility = View.GONE

    }

    // Freq
    findViewById<View>(R.id.freqImage).visibility = View.GONE
    findViewById<View>(R.id.freqText1).visibility = View.GONE
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {

    menuInflater.inflate(R.menu.activity_preview, menu)

    // 設定アイコンはダーク背景用の淡色画像なので、テーマの文字色で tint して両テーマで見えるようにする
    val tv = TypedValue()
    theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
    val iconColor = ContextCompat.getColor(this, tv.resourceId)
    menu.findItem(R.id.menu_settings)?.icon?.mutate()?.setTint(iconColor)

    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {

    // サービス状態によってmenu変更
    run {
      val item = menu.findItem(R.id.menu_start_service)
      if (item != null) {
        item.isVisible = !mServiceMaybeRunning
      }
    }
    run {
      val item = menu.findItem(R.id.menu_stop_service)
      if (item != null) {
        item.isVisible = mServiceMaybeRunning
      }
    }

    return super.onPrepareOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {

    when (item.itemId) {
      R.id.menu_start_service ->
        // サービス開始
        if (mService != null) {
          MyLog.i("MainActivity: start")

          // 常駐開始
          mService!!.startResident()
        }

      R.id.menu_stop_service ->
        // サービス停止
        if (mService != null) {
          MyLog.i("MainActivity: stop")

          // 常駐停止
          mService!!.stopResident()

          // サービス実行中フラグを下ろしておく
          mServiceMaybeRunning = false

          // 表示初期化
          hideAllCoreFreqInfo()

          // タイトル初期化
          title = getString(R.string.app_name)

          // Toolbarのアイコン初期化
          setActionBarLogo(R.drawable.color_single000)
        }

      R.id.menu_settings -> {
        val intent = Intent(this, ConfigActivity::class.java)
        mConfigActivityLauncher.launch(intent)
      }

      R.id.menu_history -> startActivity(Intent(this, HistoryActivity::class.java))

      R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))

      R.id.menu_exit ->
        // 終了
        finish()
    }

    return super.onOptionsItemSelected(item)
  }

  override fun onDestroy() {

    MyLog.d("MainActivity.onDestroy")

    cleanupServiceConnection()

    super.onDestroy()
  }

  private fun cleanupServiceConnection() {

    // コールバック解除
    mService?.unregisterCallback(mCallback)

    // サービスのbind解除
    if (mBound) {
      unbindService(mServiceConnection)
      mBound = false
    }
  }

  override fun onPause() {

    MyLog.d("onPause")

    mIsForeground = false

    super.onPause()
  }

  override fun onResume() {

    MyLog.d("onResume")

    mIsForeground = true

    super.onResume()
  }

  /**
   * CPU クロック周波数を画面に表示する
   */
  @SuppressLint("SetTextI18n")
  private fun showCpuFrequency(
    freqs: IntArray,
    minFreqs: IntArray,
    maxFreqs: IntArray,
    systemStats: SystemStats
  ) {

    // 全コアのうち最適なクロック周波数を探す
    val activeCoreIndex = MyUtil.getActiveCoreIndex(freqs)
    val currentFreq = freqs[activeCoreIndex]
    val minFreq = minFreqs[activeCoreIndex]
    val maxFreq = maxFreqs[activeCoreIndex]

    val textView1 = findViewById<View>(R.id.freqText1) as TextView
    val ssb = SpannableStringBuilder()

    ssb.append(MyUtil.formatFreq(currentFreq))

    // アクティブなコア番号と(周波数による)負荷
    run {
      val start = ssb.length
      val clockPercent = MyUtil.getClockPercent(currentFreq, minFreq, maxFreq)
      ssb.append("  [Core " + (activeCoreIndex + 1) + ": " + clockPercent + "%]")
      ssb.setSpan(RelativeSizeSpan(0.8f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      //          MyLog.i(" clock[" + currentFreq + "] => " + clockPercent + "%");
    }

    // 周波数の min/max
    run {
      ssb.append("\n")
      val minFreqText = MyUtil.formatFreq(minFreq)
      val maxFreqText = MyUtil.formatFreq(maxFreq)
      val start = ssb.length
      ssb.append(" ($minFreqText - $maxFreqText)")
      ssb.setSpan(RelativeSizeSpan(0.8f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // バッテリー温度
    if (systemStats.batteryTemperature != 0) {
      ssb.append("\n")
      val start = ssb.length
      ssb.append(" Battery: ${MyUtil.formatTemperature(systemStats.batteryTemperature)}")
      ssb.setSpan(RelativeSizeSpan(0.8f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    // メモリ使用量
    if (systemStats.memTotalMb > 0) {
      ssb.append("\n")
      val start = ssb.length
      val usedText = MyUtil.formatMemSize(systemStats.memUsedMb)
      val totalText = MyUtil.formatMemSize(systemStats.memTotalMb)
      val percent = systemStats.memUsedMb * 100 / systemStats.memTotalMb
      ssb.append(" Memory: $usedText / $totalText ($percent%)")
      ssb.setSpan(RelativeSizeSpan(0.8f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    textView1.text = ssb
    textView1.visibility = View.VISIBLE


    val id = ResourceUtil.getIconIdForCpuFreq(currentFreq)
    val imageView = findViewById<View>(R.id.freqImage) as ImageView
    imageView.setImageResource(id)
    imageView.visibility = View.VISIBLE
  }

  /**
   * CPU 使用率を画面に表示する
   */
  @SuppressLint("SetTextI18n")
  private fun showCpuUsages(
    cpuUsages: IntArray?,
    freqs: IntArray,
    minFreqs: IntArray,
    maxFreqs: IntArray
  ) {

//        MyLog.d("MainActivity.updateCpuUsages");

    // Toolbarのアイコン変更
    if (cpuUsages != null && cpuUsages.isNotEmpty()) {
      val id = ResourceUtil.getIconIdForCpuUsageSingleColor(cpuUsages[0])
      setActionBarLogo(id)
    }

    // プレビュー画面に表示
    val cores = intArrayOf(
      R.id.core1,
      R.id.core2,
      R.id.core3,
      R.id.core4,
      R.id.core5,
      R.id.core6,
      R.id.core7,
      R.id.core8
    )

    val coreCount = CpuInfoCollector.calcCpuCoreCount()
    for (i in 0..7) {

      if (coreCount <= i) {
        // Coreが少ないので消去
        findViewById<View>(cores[i]).visibility = View.GONE
        continue
      }


      val coreView = findViewById<View>(cores[i])
      coreView.visibility = View.VISIBLE

      var cpuUsage = 0
      if (cpuUsages != null && cpuUsages.size > i + 1) {
        cpuUsage = cpuUsages[i + 1]
      }

      //--------------------------------------------------
      // アイコン設定
      //--------------------------------------------------
      val id = ResourceUtil.getIconIdForCpuUsageSingleColor(cpuUsage)
      val imageView = coreView.findViewById<View>(R.id.coreImage) as ImageView
      imageView.setImageResource(id)
      imageView.visibility = View.VISIBLE

      //--------------------------------------------------
      // テキスト設定
      //--------------------------------------------------
      val textView = coreView.findViewById<View>(R.id.coreText) as TextView
      val ssb = SpannableStringBuilder()
      ssb.append("Core" + (i + 1) + ": " + cpuUsage + "%")

      // 周波数
      val freqText = MyUtil.formatFreq(freqs[i])
      ssb.append("\n")
      ssb.append(" $freqText")

      // 周波数による負荷
      val clockPercent = MyUtil.getClockPercent(freqs[i], minFreqs[i], maxFreqs[i])
      run {
        val start = ssb.length
        ssb.append(" [$clockPercent%]")
        ssb.setSpan(RelativeSizeSpan(0.8f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }

      // 周波数の min/max
      run {
        ssb.append("\n")
        val start = ssb.length
        val minFreqText = MyUtil.formatFreq(minFreqs[i])
        val maxFreqText = MyUtil.formatFreq(maxFreqs[i])
        ssb.append(" ($minFreqText - $maxFreqText)")
        ssb.setSpan(RelativeSizeSpan(0.8f), start, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }

      textView.text = ssb
      textView.visibility = View.VISIBLE

      // アクティブコアの背景色を変える
      val color = ResourceUtil.getBackgroundColor(this, clockPercent)
      coreView.setBackgroundColor(color)
    }
  }

}
