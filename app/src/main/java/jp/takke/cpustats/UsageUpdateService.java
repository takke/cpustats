package jp.takke.cpustats;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;

class OneCpuInfo {
    long idle = 0;
    long total = 0;
}

class CpuNotificationData {
    public int[] cpuUsages;
    public int coreNoStart;
    public int coreNoEnd;
}


public class UsageUpdateService extends Service {

    // 通知のID
    private static final int MY_USAGE_NOTIFICATION_ID1 = 10;
    private static final int MY_USAGE_NOTIFICATION_ID2 = 11;
    private static final int MY_FREQ_NOTIFICATION_ID = 20;

    // 更新間隔
    private long mIntervalMs = C.PREF_DEFAULT_UPDATE_INTERVAL_SEC * 1000;
    
    // 通知
    private boolean mShowUsageNotification = true;
    private boolean mShowFrequencyNotification = false;

    // CPU使用率通知のアイコンモード
    private int mCoreDistributionMode = C.CORE_DISTRIBUTION_MODE_2ICONS;

    // 常駐停止フラグ
    private boolean mStopResident = false;

    // スリープ中フラグ
    private boolean mSleeping = false;
    
    // CPU クロック周波数のmin/max
    private int mMinFreq = -1;
    private String mMinFreqText = "";
    private int mMaxFreq = -1;
    private String mMaxFreqText = "";
    
    // 前回の CPU クロック周波数
    private int mLastCpuClock = -1;
    
    // 前回の収集データ
    private ArrayList<OneCpuInfo> mLastInfo = null;
    
    // 前回のデータ
    private int[] mLastCpuUsages = null;
    
    // コールバック一覧
    private final RemoteCallbackList<IUsageUpdateCallback> mCallbackList = new RemoteCallbackList<>();
    
    // コールバック数キャッシュ
    private int mCallbackListSize = 0;

    private long mLastExecTask = System.currentTimeMillis();

    // 通信量取得スレッド管理
    private GatherThread mThread = null;
    private boolean mThreadActive = false;

    // サービスの実装
    private final IUsageUpdateService.Stub mBinder = new IUsageUpdateService.Stub() {
                
        public void registerCallback(IUsageUpdateCallback callback) throws RemoteException {
            
            // コールバックリスト登録
            mCallbackList.register(callback);
            
            mCallbackListSize ++;
        }
        
        public void unregisterCallback(IUsageUpdateCallback callback) throws RemoteException {
            
            // コールバックリスト解除
            mCallbackList.unregister(callback);
            
            if (mCallbackListSize > 0) {
                mCallbackListSize --;
            }
        }

        public void stopResident() throws RemoteException {
            
            // 常駐停止
            UsageUpdateService.this.stopResident();
        }

        public void startResident() throws RemoteException {

            // 常駐停止フラグを解除
            mStopResident = false;
            
            // すぐに次の onStart を呼ぶ
            UsageUpdateService.this.scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC);

            if (mThread == null) {
                startThread();
            }
        }

        /**
         * 設定のリロード
         */
        public void reloadSettings() throws RemoteException {
            
            loadSettings();
            
            // 設定で消された通知を消去
            if (!mShowUsageNotification) {
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MY_USAGE_NOTIFICATION_ID1);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MY_USAGE_NOTIFICATION_ID2);
            }
            if (!mShowFrequencyNotification) {
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MY_FREQ_NOTIFICATION_ID);
            }
        }
    };


    /**
     * スリープ状態(SCREEN_ON/OFF)の検出用レシーバ
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                
                MyLog.d("screen on");
                
                // 停止していれば再開する
                mSleeping = false;

                // 次の onStart を呼ぶ
                // 但し、「通知時刻」はすぐに更新するとステータスバーの順序がいきなり変わってしまってうざいので少し遅延させる
                mNotificationTimeKeep = System.currentTimeMillis() + 30*1000;
                
                UsageUpdateService.this.scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC);

                // スレッド開始
                if (!mStopResident) {
                    startThread();
                }
                
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                
                MyLog.d("screen off");
                
                // 停止する
                mSleeping = true;
                
                // アラーム停止
                stopAlarm();

                // スレッド停止
                stopThread();
            }
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        
        if (IUsageUpdateService.class.getName().equals(intent.getAction())) {
            return mBinder;
        }

        // スレッド開始
        startThread();

        return null;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        
        MyLog.d("UsageUpdateService.onCreate");
        
        // 設定のロード
        loadSettings();
        
        if (mLastInfo == null) {
            mLastInfo = MyUtil.takeCpuUsageSnapshot();
        }
        
        // スリープ状態のレシーバ登録
        getApplicationContext().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        getApplicationContext().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        
        // 定期処理開始
        scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC);

        // スレッド開始
        startThread();
    }


    /**
     * 設定のロード
     */
    private void loadSettings() {

        MyLog.i("load settings");
        
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        
        // 更新間隔
        final String updateIntervalSec = pref.getString(C.PREF_KEY_UPDATE_INTERVAL_SEC, ""+C.PREF_DEFAULT_UPDATE_INTERVAL_SEC);
        try {
            mIntervalMs = (int)(Double.parseDouble(updateIntervalSec) * 1000.0);
            MyLog.i(" interval[" + mIntervalMs + "ms]");
        } catch (NumberFormatException e) {
            MyLog.e(e);
        }
        
        // CPU使用率通知
        mShowUsageNotification = pref.getBoolean(C.PREF_KEY_SHOW_USAGE_NOTIFICATION, true);

        // クロック周波数通知
        mShowFrequencyNotification = pref.getBoolean(C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION, false);

        // CPU使用率通知のアイコンモード
        try {
            final String s = pref.getString(C.PREF_KEY_CORE_DISTRIBUTION_MODE, ""+C.CORE_DISTRIBUTION_MODE_2ICONS);
            mCoreDistributionMode = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            MyLog.e(e);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int result = super.onStartCommand(intent, flags, startId);

        MyLog.d("UsageUpdateService.onStartCommand");

        // 通信量取得スレッド開始
        if (mThread == null) {
            startThread();
        }

        // Alarmループ続行
        scheduleNextTime(C.ALARM_INTERVAL_MSEC);

        return result;
    }


    private void execTask() {

        //-------------------------------------------------
        // CPU クロック周波数の取得
        //-------------------------------------------------
        final int currentCpuClock = MyUtil.takeCurrentCpuFreq();
        if (mMinFreq < 0) {
            mMinFreq = MyUtil.takeMinCpuFreq();
            mMinFreqText = MyUtil.formatFreq(mMinFreq);
        }
        if (mMaxFreq < 0) {
            mMaxFreq = MyUtil.takeMaxCpuFreq();
            mMaxFreqText = MyUtil.formatFreq(mMaxFreq);
        }
        if (MyLog.debugMode) {
            MyLog.d("* CPU: " + currentCpuClock + " [" + mMinFreq + "," + mMaxFreq + "] [" + (System.currentTimeMillis() - mLastExecTask) + "ms]");
        }
        
        
        //-------------------------------------------------
        // CPU 使用率の取得
        //-------------------------------------------------
        // CPU 使用率の snapshot 取得
        final ArrayList<OneCpuInfo> currentInfo = MyUtil.takeCpuUsageSnapshot();
        
        // CPU 使用率の算出
        final int[] cpuUsages = MyUtil.calcCpuUsages(currentInfo, mLastInfo);
        

        //-------------------------------------------------
        // 通知判定
        //-------------------------------------------------
        // 前回と同じ CPU 使用率なら通知しない
        // ※通知アイコンだけなら丸めた値で比較すればいいんだけど通知テキストにCPU使用率%が出るので完全一致で比較する
        boolean updated = false;
        
        if (mLastCpuClock != currentCpuClock) {
            updated = true;
        } else if (mLastCpuUsages == null || cpuUsages == null) {
            updated = true;
        } else if (cpuUsages.length != mLastCpuUsages.length) {
            // コア数変動のケア(Galaxy S II等でよくあるらしい)
            updated = true;
        } else {
            // 同一でない値があれば更新する
            final int n = cpuUsages.length;
            for (int i=0; i<n; i++) {
                if (cpuUsages[i] != mLastCpuUsages[i]) {
                    updated = true;
                    break;
                }
            }
        }
        mLastCpuUsages = cpuUsages;
        mLastCpuClock = currentCpuClock;
        
        //-------------------------------------------------
        // 通知
        //-------------------------------------------------
        if (!updated) {
            if (MyLog.debugMode) {
                MyLog.d("- skipping caused by no diff.");
            }
        } else {
            // ステータスバーの通知
            updateCpuUsageNotifications(cpuUsages, currentCpuClock);
            
            // コールバック経由で通知
            if (mCallbackListSize >= 1) {
                final int n = mCallbackList.beginBroadcast();
                
                // コールバック数を念のため更新しておく
                mCallbackListSize = n;
                
//                if (MyLog.debugMode) {
//                    MyLog.d("- broadcast:" + n);
//                }
                for (int i=0; i<n; i++) {
                    try {
                        mCallbackList.getBroadcastItem(i).updateUsage(cpuUsages, currentCpuClock);
                    } catch (RemoteException e) {
//                      MyLog.e(e);
                    }
                }
                mCallbackList.finishBroadcast();
            }
        }
        
        // 今回の snapshot を保存
        mLastInfo = currentInfo;
        
        mLastExecTask = System.currentTimeMillis();
    }

    
    @Override
    public void onDestroy() {
        
        MyLog.d("UsageUpdateService.onDestroy");

        stopAlarm();

        // 通信量取得スレッド停止
        stopThread();

        // スリープ状態のレシーバ解除
        getApplicationContext().unregisterReceiver(mReceiver);
        
        // 通知を消す
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();

        super.onDestroy();
    }


    // 通知時刻[ms]
    private long mNotificationTime = 0;
    
    // 通知時刻の非更新時刻(この時間になるまで更新しない。スリープ復帰時にはすぐに更新するとステータスバーの順序がずれてうざいので)[ms]
    protected long mNotificationTimeKeep = 0;
    
    
    /**
     * ステータスバー通知の設定
     */
    @SuppressWarnings("deprecation")
    private void updateCpuUsageNotifications(int[] cpuUsages, int currentCpuClock) {
        
        // N分おきに通知時刻を更新する
        final long now = System.currentTimeMillis();
        if (now > mNotificationTime + 3*60*1000 && now > mNotificationTimeKeep) {
            mNotificationTime = now;
        }
        
        final NotificationManager nm = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));

        // 通知ウインドウをクリックした際に起動するインテント
        final Intent intent = new Intent(this, PreviewActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        
        if (cpuUsages != null && mShowUsageNotification) {

            // cpuUsagesを各通知アイコンのデータに振り分ける
            final CpuNotificationData[] data = distributeNotificationData(cpuUsages, mCoreDistributionMode);

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
        
        if (currentCpuClock > 0 && mShowFrequencyNotification) {

            // 周波数通知
            nm.notify(MY_FREQ_NOTIFICATION_ID, makeFrequencyNotification(currentCpuClock, pendingIntent).build());
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


    @NonNull
    private NotificationCompat.Builder makeFrequencyNotification(int currentCpuClock, PendingIntent pendingIntent) {

        // 通知ウインドウのメッセージ
        final String notificationTitle0 = "CPU Frequency";

        // Notification.Builder は API Level11 以降からなので旧方式で作成する
        final int iconId = ResourceUtil.getIconIdForCpuFreq(currentCpuClock);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(iconId);
        builder.setTicker(notificationTitle0);
        builder.setWhen(mNotificationTime);

        // 消えないようにする
        builder.setOngoing(true);

        // Lollipop:ロックスクリーンには表示しない
        setPriorityForKeyguardOnLollipop(builder);

        // 通知文字列の生成
        final String notificationTitle = "CPU Frequency " + MyUtil.formatFreq(currentCpuClock);
        final String notificationContent = "Max Freq " + mMaxFreqText + " Min Freq " + mMinFreqText;

        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationContent);
        builder.setContentIntent(pendingIntent);
        return builder;
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
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

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

        final KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            MyLog.d("set notification priority: min");
            final int Notification_PRIORITY_MIN = -2;   // Notification.PRIORITY_MIN
            builder.setPriority(Notification_PRIORITY_MIN);
        }
    }


    /**
     * サービスの次回の起動を予約
     */
    public void scheduleNextTime(long intervalMs) {

        // サービス終了の指示が出ていたら，次回の予約はしない。
        if (mStopResident) {
            return;
        }
        if (mSleeping) {
            return;
        }

        final long now = System.currentTimeMillis();

        // アラームをセット
        final Intent intent = new Intent(this, this.getClass());
        final PendingIntent alarmSender = PendingIntent.getService(
            this,
            0,
            intent,
            0
        );
        // ※onStartCommandが呼ばれるように設定する
        
        final AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        
        am.set(AlarmManager.RTC, now + intervalMs, alarmSender);
        
        MyLog.d("-- scheduled[" + intervalMs + "ms]");
    }


    /**
     * 常駐停止
     */
    public void stopResident() {
        
        mStopResident = true;

        // スレッド停止
        stopThread();

        // アラーム停止
        stopAlarm();
        
        // 通知を消す
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();

        // 常駐停止
        stopSelf();
    }


    /**
     * アラームの停止
     */
    private void stopAlarm() {
        
        // サービス名を指定
        final Intent intent = new Intent(this, this.getClass());

        // アラームを解除
        final PendingIntent pendingIntent = PendingIntent.getService(
            this,
            0, // ここを-1にすると解除に成功しない
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        final AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
        // @see http://creadorgranoeste.blogspot.com/2011/06/alarmmanager.html
    }


    private void startThread() {

        if (mThread == null) {
            mThread = new GatherThread();
            mThreadActive = true;
            mThread.start();
            MyLog.d("UsageUpdateService.startThread: thread start");
        } else {
            MyLog.d("UsageUpdateService.startThread: already running");
        }
    }


    private void stopThread() {

        if (mThreadActive && mThread != null) {
            MyLog.d("UsageUpdateService.stopThread");

            mThreadActive = false;
            while (true) {
                try {
                    mThread.join();
                    break;
                } catch (InterruptedException ignored) {
                    MyLog.e(ignored);
                }
            }
            mThread = null;
        } else {
            MyLog.d("UsageUpdateService.stopThread: no thread");
        }
    }


    /**
     * 通信量取得スレッド
     */
    private class GatherThread extends Thread {

        @Override
        public void run() {

            MyLog.d("UsageUpdateService$GatherThread: start");

            while (mThread != null && mThreadActive) {

                SystemClock.sleep(mIntervalMs);

                if (mThreadActive && !mStopResident) {
                    execTask();
                }
            }

            MyLog.d("UsageUpdateService$GatherThread: done");
        }
    }
}
