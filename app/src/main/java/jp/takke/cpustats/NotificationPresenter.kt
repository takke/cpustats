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
import java.lang.ref.WeakReference

internal class NotificationPresenter(service: Service, private val mConfig: MyConfig) {

    private val mServiceRef: WeakReference<Service> = WeakReference(service)

    // 通知時刻[ms]
    private var mNotificationTime: Long = 0

    // 通知時刻の非更新時刻(この時間になるまで更新しない。スリープ復帰時にはすぐに更新するとステータスバーの順序がずれてうざいので)[ms]
    var mNotificationTimeKeep: Long = 0

    /**
     * ステータスバー通知の設定
     */
    fun updateNotifications(cpuUsages: IntArray?, currentCpuClock: Int, minFreq: Int, maxFreq: Int, requestForeground: Boolean) {

        val service = mServiceRef.get() ?: return

        // N分おきに通知時刻を更新する
        val now = System.currentTimeMillis()
        if (now > mNotificationTime + 3 * 60 * 1000 && now > mNotificationTimeKeep) {
            mNotificationTime = now
        }

        val nm = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 通知ウインドウをクリックした際に起動するインテント
        val intent = Intent(service, PreviewActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(service, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT)

        //--------------------------------------------------
        // CPU使用率通知
        //--------------------------------------------------
        if (cpuUsages != null && mConfig.showUsageNotification) {

            // cpuUsagesを各通知アイコンのデータに振り分ける
            val data = CpuNotificationDataDistributor.distributeNotificationData(cpuUsages, mConfig.coreDistributionMode)

            if (MyLog.debugMode) {
                dumpCpuUsagesForDebug(cpuUsages, data)
            }

            // Android O (8.0) 以降では通知チャンネルが必要
            createNotificationChannel(service, CHANNEL_ID_CPU_USAGE, "CPU Usage")

            // Notification(icon1)
            if (data.isNotEmpty()) {
                doNotify(MY_USAGE_NOTIFICATION_ID1, makeUsageNotification(data[0], pendingIntent).build(), service, nm, requestForeground)
            } else {
                nm.cancel(MY_USAGE_NOTIFICATION_ID1)
            }
            // Notification(icon2)
            if (data.size >= 2) {
                doNotify(MY_USAGE_NOTIFICATION_ID2, makeUsageNotification(data[1], pendingIntent).build(), service, nm, requestForeground)
            } else {
                nm.cancel(MY_USAGE_NOTIFICATION_ID2)
            }
        }

        //--------------------------------------------------
        // 周波数通知
        //--------------------------------------------------
        if (currentCpuClock > 0 && mConfig.showFrequencyNotification) {

            // Android O (8.0) 以降では通知チャンネルが必要
            createNotificationChannel(service, CHANNEL_ID_CPU_FREQUENCY, "CPU Frequency")

            doNotify(MY_FREQ_NOTIFICATION_ID, makeFrequencyNotification(minFreq, maxFreq, currentCpuClock, pendingIntent).build(),
                    service, nm, requestForeground)
        }
    }

    private fun doNotify(notificationId: Int, notification: Notification,
                         service: Service, nm: NotificationManager, requestForeground: Boolean) {

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

    fun cancelNotifications() {

        val context = mServiceRef.get() ?: return

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!mConfig.showUsageNotification) {
            nm.cancel(MY_USAGE_NOTIFICATION_ID1)
            nm.cancel(MY_USAGE_NOTIFICATION_ID2)
        }
        if (!mConfig.showFrequencyNotification) {
            nm.cancel(MY_FREQ_NOTIFICATION_ID)
        }
    }

    private fun makeUsageNotification(data: CpuNotificationData, pendingIntent: PendingIntent): NotificationCompat.Builder {

        val notificationTitle0 = "CPU Usage"

        val cpuUsages = data.cpuUsages
        val totalUsage = if (cpuUsages == null) 0 else cpuUsages[0]
        val iconId = ResourceUtil.getIconIdForCpuUsage(cpuUsages)
        val context = mServiceRef.get()
        val builder = NotificationCompat.Builder(context!!, CHANNEL_ID_CPU_USAGE)

        builder.setSmallIcon(iconId)
        builder.color = ResourceUtil.getNotificationIconColorFromUsage(totalUsage)
        builder.setTicker(notificationTitle0)
        builder.setWhen(mNotificationTime)

        // 消えないようにする
        builder.setOngoing(true)

        // Lollipop:ロックスクリーンには表示しない
        setPriorityForKeyguardOnLollipop(builder)

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
        return builder
    }

    /**
     * ロックスクリーンであれば非表示にする
     */
    private fun setPriorityForKeyguardOnLollipop(builder: NotificationCompat.Builder) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }

        val km = mServiceRef.get()!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            MyLog.d("set notification priority: min")
            @Suppress("DEPRECATION")
            builder.priority = Notification.PRIORITY_MIN
        }
    }

    private fun makeFrequencyNotification(minFreq: Int, maxFreq: Int,
                                          currentCpuClock: Int, pendingIntent: PendingIntent): NotificationCompat.Builder {

        // 通知ウインドウのメッセージ
        val notificationTitle0 = "CPU Frequency"

        // Notification.Builder は API Level11 以降からなので旧方式で作成する
        val iconId = ResourceUtil.getIconIdForCpuFreq(currentCpuClock)
        val builder = NotificationCompat.Builder(mServiceRef.get()!!, CHANNEL_ID_CPU_FREQUENCY)

        builder.setSmallIcon(iconId)
        builder.setTicker(notificationTitle0)
        builder.setWhen(mNotificationTime)

        // 消えないようにする
        builder.setOngoing(true)

        // Lollipop:ロックスクリーンには表示しない
        setPriorityForKeyguardOnLollipop(builder)

        // 通知文字列の生成
        val notificationTitle = "CPU Frequency " + MyUtil.formatFreq(currentCpuClock)
        val notificationContent = "Max Freq " + MyUtil.formatFreq(maxFreq) + " Min Freq " + MyUtil.formatFreq(minFreq)

        builder.setContentTitle(notificationTitle)
        builder.setContentText(notificationContent)
        builder.setContentIntent(pendingIntent)
        return builder
    }

    companion object {

        // 通知のID
        private const val MY_USAGE_NOTIFICATION_ID1 = 10
        private const val MY_USAGE_NOTIFICATION_ID2 = 11
        private const val MY_FREQ_NOTIFICATION_ID = 20
        private const val CHANNEL_ID_CPU_USAGE = "CPU Usage"
        private const val CHANNEL_ID_CPU_FREQUENCY = "CPU Frequency"

        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun createNotificationChannel(manager: NotificationManager, channelId: String, channelName: String) {

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
