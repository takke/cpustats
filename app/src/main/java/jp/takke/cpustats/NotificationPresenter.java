package jp.takke.cpustats;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import java.lang.ref.WeakReference;
import java.util.Arrays;

class NotificationPresenter {

    // 通知のID
    private static final int MY_USAGE_NOTIFICATION_ID1 = 10;
    private static final int MY_USAGE_NOTIFICATION_ID2 = 11;
    private static final int MY_FREQ_NOTIFICATION_ID = 20;

    private final WeakReference<Context> mContextRef;
    private final MyConfig mConfig;

    // 通知時刻[ms]
    private long mNotificationTime = 0;

    // 通知時刻の非更新時刻(この時間になるまで更新しない。スリープ復帰時にはすぐに更新するとステータスバーの順序がずれてうざいので)[ms]
    long mNotificationTimeKeep = 0;

    NotificationPresenter(Context context, MyConfig config) {
        mContextRef = new WeakReference<>(context);
        mConfig = config;
    }

    /**
     * ステータスバー通知の設定
     */
    @SuppressWarnings("deprecation")
    void updateNotifications(int[] cpuUsages, int currentCpuClock, int minFreq, int maxFreq) {

        final Context context = mContextRef.get();
        if (context == null) {
            return;
        }

        // N分おきに通知時刻を更新する
        final long now = System.currentTimeMillis();
        if (now > mNotificationTime + 3*60*1000 && now > mNotificationTimeKeep) {
            mNotificationTime = now;
        }

        final NotificationManager nm = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        assert nm != null;

        // 通知ウインドウをクリックした際に起動するインテント
        final Intent intent = new Intent(context, PreviewActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        if (cpuUsages != null && mConfig.showUsageNotification) {

            // cpuUsagesを各通知アイコンのデータに振り分ける
            final CpuNotificationData[] data = distributeNotificationData(cpuUsages, mConfig.coreDistributionMode);

            if (MyLog.debugMode) {
                dumpCpuUsagesForDebug(cpuUsages, data);
            }

            // Notification(icon1)
            if (data.length >= 1) {
                nm.notify(MY_USAGE_NOTIFICATION_ID1, makeUsageNotification(data[0], pendingIntent).build());
            } else {
                nm.cancel(MY_USAGE_NOTIFICATION_ID1);
            }
            // Notification(icon2)
            if (data.length >= 2) {
                nm.notify(MY_USAGE_NOTIFICATION_ID2, makeUsageNotification(data[1], pendingIntent).build());
            } else {
                nm.cancel(MY_USAGE_NOTIFICATION_ID2);
            }
        }

        if (currentCpuClock > 0 && mConfig.showFrequencyNotification) {

            // 周波数通知
            nm.notify(MY_FREQ_NOTIFICATION_ID, makeFrequencyNotification(minFreq, maxFreq, currentCpuClock, pendingIntent).build());
        }
    }

    public void cancelNotifications() {

        final Context context = mContextRef.get();
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

    public static CpuNotificationData[] distributeNotificationData(int[] cpuUsages, int coreDistributionMode) {

        // cpuUsages の index=0 は「全CPU使用率の平均」
        final int coreCount = cpuUsages.length - 1;

        switch (coreDistributionMode) {
        case C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED:
            return distributeNotificationData_1IconUnsorted(coreCount, cpuUsages);

        case C.CORE_DISTRIBUTION_MODE_1ICON_SORTED:
            return distributeNotificationData_1IconSorted(coreCount, cpuUsages);

        case C.CORE_DISTRIBUTION_MODE_2ICONS:
        default:
            return distributeNotificationData_2Icons(coreCount, cpuUsages);
        }
    }

    private static CpuNotificationData[] distributeNotificationData_1IconUnsorted(int coreCount, int[] cpuUsages) {

        if (coreCount <= 4) {
            // 4コア以下
            final CpuNotificationData data[] = new CpuNotificationData[1];

            // icon1
            data[0] = new CpuNotificationData();
            data[0].cpuUsages = cpuUsages;
            data[0].coreNoStart = 1;
            data[0].coreNoEnd = coreCount;

            return data;
        }

        {
            // 5コア以上
            final CpuNotificationData data[] = new CpuNotificationData[1];

            // icon1
            data[0] = new CpuNotificationData();
            data[0].cpuUsages = new int[1+4];
            System.arraycopy(cpuUsages, 0, data[0].cpuUsages, 0, 5);

            data[0].coreNoStart = 1;
            data[0].coreNoEnd = 4;

            return data;
        }
    }

    private static CpuNotificationData[] distributeNotificationData_1IconSorted(int coreCount, int[] cpuUsages) {

        final CpuNotificationData data[] = new CpuNotificationData[1];

        // icon1
        data[0] = new CpuNotificationData();
        data[0].cpuUsages = new int[1+min(coreCount, 4)];
        data[0].cpuUsages[0] = cpuUsages[0];

        // index=0 は無視してソートする
        Arrays.sort(cpuUsages, 1, cpuUsages.length);

        // 降順に選択する
        for (int i=0; i<min(coreCount, 4); i++) {
            data[0].cpuUsages[i+1] = cpuUsages[cpuUsages.length-i-1];
        }

        data[0].coreNoStart = 1;
        data[0].coreNoEnd = min(coreCount, 4);

        return data;
    }

    @SuppressWarnings("SameParameterValue")
    private static int min(int a, int b) {
        return a < b ? a : b;
    }

    private static CpuNotificationData[] distributeNotificationData_2Icons(int coreCount, int[] cpuUsages) {

        if (coreCount <= 4) {
            // 4コア以下
            final CpuNotificationData data[] = new CpuNotificationData[1];

            // icon1
            data[0] = new CpuNotificationData();
            data[0].cpuUsages = cpuUsages;
            data[0].coreNoStart = 1;
            data[0].coreNoEnd = coreCount;

            return data;
        }

        if (coreCount == 6) {
            // 6コアなので3つずつに分割する
            final CpuNotificationData data[] = new CpuNotificationData[2];

            // icon1
            data[0] = new CpuNotificationData();
            data[0].cpuUsages = new int[3 + 1];
            data[0].coreNoStart = 1;
            data[0].coreNoEnd = 3;
            System.arraycopy(cpuUsages, 0, data[0].cpuUsages, 0, 4);

            // icon2
            data[1] = new CpuNotificationData();
            data[1].cpuUsages = new int[3 + 1];
            data[1].coreNoStart = 4;
            data[1].coreNoEnd = 6;
            // icon2のindex=0 も「全CPU使用率の平均」とする
            data[1].cpuUsages[0] = cpuUsages[0];
            System.arraycopy(cpuUsages, 4, data[1].cpuUsages, 1, 3);

            return data;
        }

        {
            // 4コア以上(6コアを除く)

            // 2つに分割する
            final CpuNotificationData data[] = new CpuNotificationData[2];

            // icon1
            data[0] = new CpuNotificationData();
            data[0].cpuUsages = new int[5];
            data[0].coreNoStart = 1;
            data[0].coreNoEnd = 4;
            System.arraycopy(cpuUsages, 0, data[0].cpuUsages, 0, 5);

            // icon2
            data[1] = new CpuNotificationData();
            data[1].cpuUsages = new int[1 + coreCount - 4];
            data[1].coreNoStart = 5;
            data[1].coreNoEnd = coreCount;
            // icon2のindex=0 も「全CPU使用率の平均」とする
            data[1].cpuUsages[0] = cpuUsages[0];
            System.arraycopy(cpuUsages, 5, data[1].cpuUsages, 1, coreCount - 4);

            return data;
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
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContextRef.get());

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

        final KeyguardManager km = (KeyguardManager) mContextRef.get().getSystemService(Context.KEYGUARD_SERVICE);
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
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContextRef.get());

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
