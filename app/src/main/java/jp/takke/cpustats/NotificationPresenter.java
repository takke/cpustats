package jp.takke.cpustats;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import java.lang.ref.WeakReference;

class NotificationPresenter {

    // 通知のID
    private static final int MY_USAGE_NOTIFICATION_ID1 = 10;
    private static final int MY_USAGE_NOTIFICATION_ID2 = 11;
    private static final int MY_FREQ_NOTIFICATION_ID = 20;
    private static final String CHANNEL_ID_CPU_USAGE = "CPU Usage";
    private static final String CHANNEL_ID_CPU_FREQUENCY = "CPU Frequency";

    private final WeakReference<Service> mServiceRef;
    private final MyConfig mConfig;

    // 通知時刻[ms]
    private long mNotificationTime = 0;

    // 通知時刻の非更新時刻(この時間になるまで更新しない。スリープ復帰時にはすぐに更新するとステータスバーの順序がずれてうざいので)[ms]
    long mNotificationTimeKeep = 0;

    /*package*/ NotificationPresenter(Service service, MyConfig config) {
        mServiceRef = new WeakReference<>(service);
        mConfig = config;
    }

    /**
     * ステータスバー通知の設定
     */
    @SuppressWarnings("deprecation")
    /*package*/ void updateNotifications(int[] cpuUsages, int currentCpuClock, int minFreq, int maxFreq, boolean requestForeground) {

        final Service service = mServiceRef.get();
        if (service == null) {
            return;
        }

        // N分おきに通知時刻を更新する
        final long now = System.currentTimeMillis();
        if (now > mNotificationTime + 3*60*1000 && now > mNotificationTimeKeep) {
            mNotificationTime = now;
        }

        final NotificationManager nm = ((NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE));
        assert nm != null;

        // 通知ウインドウをクリックした際に起動するインテント
        final Intent intent = new Intent(service, PreviewActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        //--------------------------------------------------
        // CPU使用率通知
        //--------------------------------------------------
        if (cpuUsages != null && mConfig.showUsageNotification) {

            // cpuUsagesを各通知アイコンのデータに振り分ける
            final CpuNotificationData[] data = CpuNotificationDataDistributor.distributeNotificationData(cpuUsages, mConfig.coreDistributionMode);

            if (MyLog.debugMode) {
                dumpCpuUsagesForDebug(cpuUsages, data);
            }

            // Android O (8.0) 以降では通知チャンネルが必要
            createNotificationChannel(service, CHANNEL_ID_CPU_USAGE, "CPU Usage");

            // Notification(icon1)
            if (data.length >= 1) {
                doNotify(MY_USAGE_NOTIFICATION_ID1, makeUsageNotification(data[0], pendingIntent).build(), service, nm, requestForeground);
            } else {
                nm.cancel(MY_USAGE_NOTIFICATION_ID1);
            }
            // Notification(icon2)
            if (data.length >= 2) {
                doNotify(MY_USAGE_NOTIFICATION_ID2, makeUsageNotification(data[1], pendingIntent).build(), service, nm, requestForeground);
            } else {
                nm.cancel(MY_USAGE_NOTIFICATION_ID2);
            }
        }

        //--------------------------------------------------
        // 周波数通知
        //--------------------------------------------------
        if (currentCpuClock > 0 && mConfig.showFrequencyNotification) {

            // Android O (8.0) 以降では通知チャンネルが必要
            createNotificationChannel(service, CHANNEL_ID_CPU_FREQUENCY, "CPU Frequency");

            doNotify(MY_FREQ_NOTIFICATION_ID, makeFrequencyNotification(minFreq, maxFreq, currentCpuClock, pendingIntent).build(),
                    service, nm, requestForeground);
        }
    }

    private void doNotify(int notificationId, Notification notification,
                          Service service, NotificationManager nm, boolean requestForeground) {
        if (requestForeground) {
            service.startForeground(notificationId, notification);
        } else {
            nm.notify(notificationId, notification);
        }
    }

    private void createNotificationChannel(Context context, String channelId, String channelName) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final NotificationManager manager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            if (manager.getNotificationChannel(channelId) == null) {
                _createNotificationChannel(manager, channelId, channelName);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void _createNotificationChannel(NotificationManager manager, String channelId, String channelName) {

        final NotificationChannel channel = new NotificationChannel(
                channelId,
                // ユーザが「設定」アプリで見ることになるチャンネル名
                channelName,
                NotificationManager.IMPORTANCE_LOW
        );

        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        manager.createNotificationChannel(channel);

    }

    /*package*/  void cancelNotifications() {

        final Context context = mServiceRef.get();
        if (context == null) {
            return;
        }

        final NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert nm != null;
        if (!mConfig.showUsageNotification) {
            nm.cancel(MY_USAGE_NOTIFICATION_ID1);
            nm.cancel(MY_USAGE_NOTIFICATION_ID2);
        }
        if (!mConfig.showFrequencyNotification) {
            nm.cancel(MY_FREQ_NOTIFICATION_ID);
        }
    }

    private static void dumpCpuUsagesForDebug(int[] cpuUsages, CpuNotificationData[] data) {

        final StringBuilder sb = new StringBuilder();
        sb.append("org: ");
        for (int cpuUsage : cpuUsages) {
            sb.append(cpuUsage).append("% ");
        }
        sb.append("\nusage1: ");
        for (int cpuUsage : data[0].cpuUsages) {
            sb.append(cpuUsage).append("% ");
        }
        sb.append("\nusage2: ");
        for (int cpuUsage : data[1].cpuUsages) {
            sb.append(cpuUsage).append("% ");
        }
        MyLog.d(sb.toString());
    }

    @NonNull
    private NotificationCompat.Builder makeUsageNotification(CpuNotificationData data, PendingIntent pendingIntent) {

        String notificationTitle0 = "CPU Usage";

        final int iconId = ResourceUtil.getIconIdForCpuUsage(data.cpuUsages);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mServiceRef.get(), CHANNEL_ID_CPU_USAGE);

        builder.setSmallIcon(iconId);
        builder.setTicker(notificationTitle0);
        builder.setWhen(mNotificationTime);

        // 消えないようにする
        builder.setOngoing(true);

        // Lollipop:ロックスクリーンには表示しない
        setPriorityForKeyguardOnLollipop(builder);

        // 通知文字列の生成
        final StringBuilder sb = new StringBuilder(128);
        // 各コア分
        if (data.cpuUsages.length >= 3) {    // 2コアの場合length=3になるので。

//            sb.append("Cores: ");
            sb.append("Core").append(data.coreNoStart).append("-Core").append(data.coreNoEnd).append(": ");

            for (int i=1; i<data.cpuUsages.length; i++) {
                if (i>=2) {
                    sb.append(" ");
                }
                sb.append(data.cpuUsages[i]).append("%");
            }
        }
        final String notificationContent = sb.toString();
        if (MyLog.debugMode) {
            MyLog.d("- " + notificationContent);
        }

        final String notificationTitle = notificationTitle0 + " " + data.cpuUsages[0] + "%";
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationContent);
        builder.setContentIntent(pendingIntent);
        return builder;
    }

    /**
     * ロックスクリーンであれば非表示にする
     */
    private void setPriorityForKeyguardOnLollipop(NotificationCompat.Builder builder) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        final KeyguardManager km = (KeyguardManager) mServiceRef.get().getSystemService(Context.KEYGUARD_SERVICE);
        assert km != null;
        if (km.inKeyguardRestrictedInputMode()) {
            MyLog.d("set notification priority: min");
            final int Notification_PRIORITY_MIN = -2;   // Notification.PRIORITY_MIN
            builder.setPriority(Notification_PRIORITY_MIN);
        }
    }

    @NonNull
    private NotificationCompat.Builder makeFrequencyNotification(int minFreq, int maxFreq,
                                                                 int currentCpuClock, PendingIntent pendingIntent) {

        // 通知ウインドウのメッセージ
        final String notificationTitle0 = "CPU Frequency";

        // Notification.Builder は API Level11 以降からなので旧方式で作成する
        final int iconId = ResourceUtil.getIconIdForCpuFreq(currentCpuClock);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mServiceRef.get(), CHANNEL_ID_CPU_FREQUENCY);

        builder.setSmallIcon(iconId);
        builder.setTicker(notificationTitle0);
        builder.setWhen(mNotificationTime);

        // 消えないようにする
        builder.setOngoing(true);

        // Lollipop:ロックスクリーンには表示しない
        setPriorityForKeyguardOnLollipop(builder);

        // 通知文字列の生成
        final String notificationTitle = "CPU Frequency " + MyUtil.formatFreq(currentCpuClock);
        final String notificationContent = "Max Freq " + MyUtil.formatFreq(maxFreq) + " Min Freq " + MyUtil.formatFreq(minFreq);

        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationContent);
        builder.setContentIntent(pendingIntent);
        return builder;
    }
}
