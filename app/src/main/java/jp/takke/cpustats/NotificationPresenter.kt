package jp.takke.cpustats

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import jp.takke.cpustats.util.MyUtil
import jp.takke.cpustats.util.ResourceUtil
import java.lang.ref.WeakReference

internal class NotificationPresenter(service: Service) {

  private val mServiceRef: WeakReference<Service> = WeakReference(service)

  // 通知時刻[ms]
  private var mNotificationTime: Long = 0

  // 通知時刻の非更新時刻(この時間になるまで更新しない。スリープ復帰時にはすぐに更新するとステータスバーの順序がずれてうざいので)[ms]
  var mNotificationTimeKeep: Long = 0

  /**
   * ステータスバー通知の設定
   */
  fun updateNotifications(
    cpuUsages: IntArray?,
    fi: AllCoreFrequencyInfo,
    systemStats: SystemStats,
    requestForeground: Boolean,
    config: MyConfig
  ) {

    val service = mServiceRef.get() ?: return

    // 最もアクティブなコアのクロック情報
    val activeCoreIndex = MyUtil.getActiveCoreIndex(fi.freqs)
    val currentCpuClock = fi.freqs[activeCoreIndex]
    val minFreq = fi.minFreqs[activeCoreIndex]
    val maxFreq = fi.maxFreqs[activeCoreIndex]

    // N分おきに通知時刻を更新する
    val now = System.currentTimeMillis()
    if (now > mNotificationTime + 3 * 60 * 1000 && now > mNotificationTimeKeep) {
      mNotificationTime = now
    }

    val nm = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // 通知ウインドウをクリックした際に起動するインテント
    val intent = Intent(service, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      service, 0, intent,
      PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    //--------------------------------------------------
    // CPU使用率通知
    //--------------------------------------------------
    if (cpuUsages != null && config.showUsageNotification) {

      // cpuUsagesを各通知アイコンのデータに振り分ける
      val data = CpuNotificationDataDistributor.distributeNotificationData(
        cpuUsages,
        config.coreDistributionMode
      )

      if (MyLog.debugMode) {
        dumpCpuUsagesForDebug(cpuUsages, data)
      }

      // Android O (8.0) 以降では通知チャンネルが必要
      createNotificationChannel(service, CHANNEL_ID_CPU_USAGE, "CPU Usage")

      // Notification(icon1)
      if (data.isNotEmpty()) {
        doNotify(
          MY_USAGE_NOTIFICATION_ID1,
          makeUsageNotification(data[0], pendingIntent, cpuUsages, fi, systemStats).build(),
          service,
          nm,
          requestForeground
        )
      } else {
        nm.cancel(MY_USAGE_NOTIFICATION_ID1)
      }
      // Notification(icon2)
      if (data.size >= 2) {
        doNotify(
          MY_USAGE_NOTIFICATION_ID2,
          makeUsageNotification(data[1], pendingIntent, cpuUsages, fi, systemStats).build(),
          service,
          nm,
          requestForeground
        )
      } else {
        nm.cancel(MY_USAGE_NOTIFICATION_ID2)
      }
    }

    //--------------------------------------------------
    // 周波数通知
    //--------------------------------------------------
    if (currentCpuClock > 0 && config.showFrequencyNotification) {

      // Android O (8.0) 以降では通知チャンネルが必要
      createNotificationChannel(service, CHANNEL_ID_CPU_FREQUENCY, "CPU Frequency")

      doNotify(
        MY_FREQ_NOTIFICATION_ID,
        makeFrequencyNotification(
          minFreq, maxFreq, currentCpuClock, systemStats.batteryTemperature, pendingIntent, fi
        ).build(),
        service,
        nm,
        requestForeground
      )
    }
  }

  /**
   * 通知アクション(常駐停止・設定)の追加
   */
  private fun addCommonActions(builder: NotificationCompat.Builder, service: Service) {

    // 常駐停止
    val stopIntent = Intent(service, UsageUpdateService::class.java)
      .setAction(UsageUpdateService.ACTION_STOP_RESIDENT)
    val stopPendingIntent = PendingIntent.getService(
      service, REQUEST_CODE_STOP, stopIntent, PendingIntent.FLAG_IMMUTABLE
    )
    builder.addAction(0, service.getString(R.string.menu_stop_service), stopPendingIntent)

    // 設定
    val configIntent = Intent(service, ConfigActivity::class.java)
    val configPendingIntent = PendingIntent.getActivity(
      service, REQUEST_CODE_CONFIG, configIntent, PendingIntent.FLAG_IMMUTABLE
    )
    builder.addAction(0, service.getString(R.string.title_activity_config), configPendingIntent)
  }

  private fun doNotify(
    notificationId: Int, notification: Notification,
    service: Service, nm: NotificationManager, requestForeground: Boolean
  ) {

//        if (MyLog.debugMode) {
//            MyLog.d("doNotify[$notificationId], ticker[${notification.tickerText}], usage[${mConfig.showUsageNotification}], frequency[${mConfig.showFrequencyNotification}]")
//        }
    if (requestForeground) {
      service.startForeground(notificationId, notification)
    } else {
      nm.notify(notificationId, notification)
    }
  }

  private fun createNotificationChannel(context: Context, channelId: String, channelName: String) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(channelId) == null) {
        createNotificationChannel(manager, channelId, channelName)
      }
    }
  }

  fun cancelNotifications(config: MyConfig) {

    val context = mServiceRef.get() ?: return

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (!config.showUsageNotification) {
      nm.cancel(MY_USAGE_NOTIFICATION_ID1)
      nm.cancel(MY_USAGE_NOTIFICATION_ID2)
    }
    if (!config.showFrequencyNotification) {
      nm.cancel(MY_FREQ_NOTIFICATION_ID)
    }
  }

  private fun makeUsageNotification(
    data: CpuNotificationData,
    pendingIntent: PendingIntent,
    allCpuUsages: IntArray,
    fi: AllCoreFrequencyInfo,
    systemStats: SystemStats
  ): NotificationCompat.Builder {

    val notificationTitle0 = "CPU Usage"

    val cpuUsages = data.cpuUsages
    val totalUsage = if (cpuUsages == null) 0 else cpuUsages[0]
    val iconId = ResourceUtil.getIconIdForCpuUsage(cpuUsages)
    val service = mServiceRef.get()!!
    val builder = NotificationCompat.Builder(service, CHANNEL_ID_CPU_USAGE)

    builder.setSmallIcon(iconId)
    builder.color = ResourceUtil.getNotificationIconColorFromUsage(totalUsage)
    builder.setTicker(notificationTitle0)
    builder.setWhen(mNotificationTime)

    // 消えないようにする
    builder.setOngoing(true)

    // ロックスクリーンには表示しない
    setPriorityForKeyguard(builder)

    // 通知文字列の生成
    val sb = StringBuilder(128)
    // 各コア分
    if (cpuUsages != null && cpuUsages.size >= 3) {    // 2コアの場合length=3になるので。

//            sb.append("Cores: ");
      sb.append("Core").append(data.coreNoStart).append("-Core").append(data.coreNoEnd).append(": ")

      for (i in 1 until cpuUsages.size) {
        if (i >= 2) {
          sb.append(" ")
        }
        sb.append(cpuUsages[i]).append("%")
      }
    }
    val notificationContent = sb.toString()
    if (MyLog.debugMode) {
      MyLog.d("- $notificationContent")
    }

    val notificationTitle = "$notificationTitle0 $totalUsage%"
    builder.setContentTitle(notificationTitle)
    builder.setContentText(notificationContent)
    builder.setContentIntent(pendingIntent)

    // 展開時は全コアの使用率 + 現在周波数を表示する
    builder.setStyle(
      NotificationCompat.BigTextStyle().bigText(
        makeUsageBigText(allCpuUsages, fi, systemStats)
      )
    )

    // アクション(常駐停止・設定)
    addCommonActions(builder, service)

    return builder
  }

  /**
   * 使用率通知の展開時テキストを生成する
   */
  private fun makeUsageBigText(
    allCpuUsages: IntArray,
    fi: AllCoreFrequencyInfo,
    systemStats: SystemStats
  ): String {

    val sb = StringBuilder(256)

    val coreCount = allCpuUsages.size - 1
    for (i in 1..coreCount) {
      sb.append("Core").append(i).append(": ").append(allCpuUsages[i]).append("%")

      val freq = fi.freqs.getOrNull(i - 1) ?: 0
      if (freq > 0) {
        sb.append(" (").append(MyUtil.formatFreq(freq)).append(")")
      }
      sb.append('\n')
    }

    // バッテリー温度・メモリのサマリ
    val summary = StringBuilder()
    if (systemStats.batteryTemperature != 0) {
      summary.append("Battery ").append(MyUtil.formatTemperature(systemStats.batteryTemperature))
    }
    if (systemStats.memTotalMb > 0) {
      if (summary.isNotEmpty()) {
        summary.append("  ")
      }
      summary.append("Memory ")
        .append(MyUtil.formatMemSize(systemStats.memUsedMb))
        .append(" / ")
        .append(MyUtil.formatMemSize(systemStats.memTotalMb))
    }
    if (summary.isNotEmpty()) {
      sb.append(summary)
    }

    return sb.toString().trimEnd()
  }

  /**
   * ロックスクリーンであれば非表示にする
   */
  private fun setPriorityForKeyguard(builder: NotificationCompat.Builder) {

    val km = mServiceRef.get()!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    if (km.isKeyguardLocked) {
      MyLog.d("set notification priority: min")
      @Suppress("DEPRECATION")
      builder.priority = Notification.PRIORITY_MIN
    }
  }

  private fun makeFrequencyNotification(
    minFreq: Int, maxFreq: Int,
    currentCpuClock: Int, batteryTemperature: Int, pendingIntent: PendingIntent,
    fi: AllCoreFrequencyInfo
  ): NotificationCompat.Builder {

    // 通知ウインドウのメッセージ
    val notificationTitle0 = "CPU Frequency"

    val iconId = ResourceUtil.getIconIdForCpuFreq(currentCpuClock)
    val service = mServiceRef.get()!!
    val builder = NotificationCompat.Builder(service, CHANNEL_ID_CPU_FREQUENCY)

    builder.setSmallIcon(iconId)
    builder.setTicker(notificationTitle0)
    builder.setWhen(mNotificationTime)

    // 消えないようにする
    builder.setOngoing(true)

    // ロックスクリーンには表示しない
    setPriorityForKeyguard(builder)

    // 通知文字列の生成
    val notificationTitle = "CPU Frequency " + MyUtil.formatFreq(currentCpuClock)
    var notificationContent =
      "Max Freq " + MyUtil.formatFreq(maxFreq) + " Min Freq " + MyUtil.formatFreq(minFreq)
    if (batteryTemperature != 0) {
      notificationContent += "  Battery " + MyUtil.formatTemperature(batteryTemperature)
    }

    builder.setContentTitle(notificationTitle)
    builder.setContentText(notificationContent)
    builder.setContentIntent(pendingIntent)

    // 展開時はクラスタ構成 + 全コアの現在周波数を表示する
    builder.setStyle(
      NotificationCompat.BigTextStyle().bigText(makeFrequencyBigText(fi))
    )

    // アクション(常駐停止・設定)
    addCommonActions(builder, service)

    return builder
  }

  /**
   * 周波数通知の展開時テキストを生成する
   */
  private fun makeFrequencyBigText(fi: AllCoreFrequencyInfo): String {

    val sb = StringBuilder(256)

    // クラスタ構成(big.LITTLE)
    val clusters = MyUtil.formatCpuClusters(fi.maxFreqs)
    if (clusters.isNotEmpty()) {
      sb.append(clusters).append('\n')
    }

    // 各コアの現在周波数
    for (i in fi.freqs.indices) {
      sb.append("Core").append(i + 1).append(": ")
      val freq = fi.freqs[i]
      if (freq > 0) {
        sb.append(MyUtil.formatFreq(freq))
      } else {
        sb.append("-")
      }
      sb.append('\n')
    }

    return sb.toString().trimEnd()
  }

  companion object {

    // 通知のID
    private const val MY_USAGE_NOTIFICATION_ID1 = 10
    private const val MY_USAGE_NOTIFICATION_ID2 = 11
    private const val MY_FREQ_NOTIFICATION_ID = 20
    private const val CHANNEL_ID_CPU_USAGE = "CPU Usage"
    private const val CHANNEL_ID_CPU_FREQUENCY = "CPU Frequency"

    // PendingIntent の request code(通知タップ用の 0 と衝突しないこと)
    private const val REQUEST_CODE_STOP = 1
    private const val REQUEST_CODE_CONFIG = 2

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(
      manager: NotificationManager,
      channelId: String,
      channelName: String
    ) {

      val channel = NotificationChannel(
        channelId,
        // ユーザが「設定」アプリで見ることになるチャンネル名
        channelName,
        NotificationManager.IMPORTANCE_LOW
      )

      channel.enableLights(false)
      channel.enableVibration(false)
      channel.setSound(null, null)
      channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

      manager.createNotificationChannel(channel)

    }

    private fun dumpCpuUsagesForDebug(cpuUsages: IntArray, data: Array<CpuNotificationData>) {

      val sb = StringBuilder()
      sb.append("org: ")
      for (cpuUsage in cpuUsages) {
        sb.append(cpuUsage).append("% ")
      }

      if (data.isNotEmpty()) {
        sb.append("\nusage1: ")
        data[0].cpuUsages?.forEach {
          sb.append(it).append("% ")
        }
      }

      if (data.size >= 2) {
        sb.append("\nusage2: ")
        data[1].cpuUsages?.forEach {
          sb.append(it).append("% ")
        }
      }

      MyLog.d(sb.toString())
    }
  }
}
