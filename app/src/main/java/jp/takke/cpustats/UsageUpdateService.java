package jp.takke.cpustats;

import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;

class OneCpuInfo {
    long idle = 0;
    long total = 0;
}

public class UsageUpdateService extends Service {

    // 通知のID
    private static final int MY_USAGE_NOTIFICATION_ID = 1;
    private static final int MY_FREQ_NOTIFICATION_ID = 2;

    // 更新間隔
    private long mIntervalMs = C.PREF_DEFAULT_UPDATE_INTERVAL_SEC * 1000;
    
    // 通知
    private boolean mShowUsageNotification = true;
    private boolean mShowFrequencyNotification = false;
    
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
    private final RemoteCallbackList<IUsageUpdateCallback> mCallbackList = new RemoteCallbackList<IUsageUpdateCallback>();
    
    // コールバック数キャッシュ
    private int mCallbackListSize = 0;
    
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
            UsageUpdateService.this.scheduleNextTime(100);
        }

        /**
         * 設定のリロード
         */
        public void reloadSettings() throws RemoteException {
            
            loadSettings();
            
            // 設定で消された通知を消去
            if (!mShowUsageNotification) {
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(MY_USAGE_NOTIFICATION_ID);
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
                
                UsageUpdateService.this.scheduleNextTime(mIntervalMs);
                
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                
                MyLog.d("screen off");
                
                // 停止する
                mSleeping = true;
                
                // アラーム停止
                stopAlerm();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        
        if (IUsageUpdateService.class.getName().equals(intent.getAction())) {
            return mBinder;
        }
        
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
        scheduleNextTime(mIntervalMs);
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
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        
        MyLog.d("UsageUpdateService.onStart");
        
        // TODO 遅いと怒られるのでTaskにすべき
        execTask();
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
            MyLog.d("CPU:" + currentCpuClock + " [" + mMinFreq + "," + mMaxFreq + "]");
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
            // 各値が同一なら更新する
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
                MyLog.d(" skipping caused by no diff.");
            }
        } else {
            // ステータスバーの通知
            updateCpuUsageNotifications(cpuUsages, currentCpuClock);
            
            // コールバック経由で通知
            if (mCallbackListSize >= 1) {
                final int n = mCallbackList.beginBroadcast();
                
                // コールバック数を念のため更新しておく
                mCallbackListSize = n;
                
                if (MyLog.debugMode) {
                    MyLog.d(" broadcast:" + n);
                }
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
        
        // 次回の実行予約
        scheduleNextTime(mIntervalMs);
    }

    
    @Override
    public void onDestroy() {
        
        MyLog.d("UsageUpdateService.onDestroy");
        
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
     * 
     * @param cpuUsages
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
                Intent.FLAG_ACTIVITY_NEW_TASK | PendingIntent.FLAG_CANCEL_CURRENT);
        
        if (cpuUsages != null && mShowUsageNotification) {
            // 通知ウインドウのメッセージ
            final String notificationTitle0 = "CPU Usage";
    
            // Notification.Builder は API Level11 以降からなので旧方式で作成する
            final int iconId = ResourceUtil.getIconIdForCpuUsage(cpuUsages);
            final Notification notification = new Notification(iconId, notificationTitle0, mNotificationTime);
            
            // 消えないようにする
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            
            // 通知文字列の生成
            final StringBuilder sb = new StringBuilder(128);
            // 各コア分
            if (cpuUsages.length >= 3) {
                sb.append("Cores: ");
                for (int i=1; i<cpuUsages.length; i++) {
                    if (i>=2) {
                        sb.append(" ");
                    }
                    sb.append(cpuUsages[i] + "%");
                }
            }
            final String notificationContent = sb.toString();
            if (MyLog.debugMode) {
                MyLog.d(" " + notificationContent);
            }
            
            final String notificationTitle = "CPU Usage " + cpuUsages[0] + "%";
            notification.setLatestEventInfo(this, notificationTitle, notificationContent, pendingIntent);

            // ノーティフィケーション通知
            nm.notify(MY_USAGE_NOTIFICATION_ID, notification);
        }
        
        if (currentCpuClock > 0 && mShowFrequencyNotification) {
            // 通知ウインドウのメッセージ
            final String notificationTitle0 = "CPU Frequency";

            // Notification.Builder は API Level11 以降からなので旧方式で作成する
            final int iconId = ResourceUtil.getIconIdForCpuFreq(currentCpuClock);
            final Notification notification = new Notification(iconId, notificationTitle0, mNotificationTime);
            
            // 消えないようにする
            notification.flags = Notification.FLAG_ONGOING_EVENT;
            
            // 通知文字列の生成
            final String notificationTitle = "CPU Frequency " + MyUtil.formatFreq(currentCpuClock);
            final String notificationContent = "Max Freq " + mMaxFreqText + " Min Freq " + mMinFreqText;
            
            notification.setLatestEventInfo(this, notificationTitle, notificationContent, pendingIntent);

            // ノーティフィケーション通知
            nm.notify(MY_FREQ_NOTIFICATION_ID, notification);
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
        // ※onStartが呼ばれるように設定する
        
        final AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        
        am.set(AlarmManager.RTC, now + intervalMs, alarmSender);
        
        MyLog.d(" scheduled[" + intervalMs + "]");
    }
    
    /**
     * 常駐停止
     */
    public void stopResident() {
        
        mStopResident = true;
        
        // アラーム停止
        stopAlerm();
        
        // 通知を消す
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        
        // 常駐停止
        stopSelf();
    }

    /**
     * アラームの停止
     */
    private void stopAlerm() {
        
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
}
