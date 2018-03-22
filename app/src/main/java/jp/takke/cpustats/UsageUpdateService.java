package jp.takke.cpustats;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.Nullable;

import java.util.ArrayList;


public class UsageUpdateService extends Service {

    // 設定値
    private MyConfig mConfig = new MyConfig();

    // 通知管理クラス
    private NotificationPresenter mNotificationPresenter = new NotificationPresenter(this, mConfig);

    // 常駐停止フラグ
    private boolean mStopResident = false;

    // (Android 8.0 以降用) BOOT_COMPLETED から startForeground を実行するためのフラグ
    private boolean mRequestForeground = false;

    // スリープ中フラグ
    private boolean mSleeping = false;

    // 前回の CPU クロック周波数
    private int mLastCpuClock = -1;
    
    // 前回の収集データ
    @Nullable
    private ArrayList<OneCpuInfo> mLastCpuUsageSnapshot = null;
    
    // 前回のCPU使用率
    private int[] mLastCpuUsages = null;

    // CPU使用率を各コアの周波数から算出するモード(Android 8.0以降用)
    private boolean mUseFreqForCpuUsage = false;

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

            MyLog.d("reloadSettings");
            mConfig.loadSettings(UsageUpdateService.this);
            
            // 設定で消された通知を消去
            mNotificationPresenter.cancelNotifications();
        }
    };


    /**
     * スリープ状態(SCREEN_ON/OFF)の検出用レシーバ
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                
                MyLog.d("screen on");
                
                // 停止していれば再開する
                mSleeping = false;

                // 次の onStart を呼ぶ
                // 但し、「通知時刻」はすぐに更新するとステータスバーの順序がいきなり変わってしまってうざいので少し遅延させる
                mNotificationPresenter.mNotificationTimeKeep = System.currentTimeMillis() + 30*1000;
                
                UsageUpdateService.this.scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC);

                // スレッド開始
                if (!mStopResident) {
                    startThread();
                }
                
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                
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

        MyLog.i("UsageUpdateService.onBind");

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
        
        MyLog.i("UsageUpdateService.onCreate");
        
        // 設定のロード
        mConfig.loadSettings(this);
        
        if (mLastCpuUsageSnapshot == null) {
            mLastCpuUsageSnapshot = CpuInfoCollector.takeCpuUsageSnapshot();
        }
        
        // スリープ状態のレシーバ登録
        getApplicationContext().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        getApplicationContext().registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        
        // 定期処理開始
        scheduleNextTime(C.ALARM_STARTUP_DELAY_MSEC);

        // スレッド開始
        startThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final int result = super.onStartCommand(intent, flags, startId);

        mRequestForeground = intent.getBooleanExtra("FOREGROUND_REQUEST", false);
        MyLog.i("UsageUpdateService.onStartCommand[" + mRequestForeground + "]");

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
        final AllCoreFrequencyInfo fi = new AllCoreFrequencyInfo(CpuInfoCollector.calcCpuCoreCount());
        CpuInfoCollector.takeAllCoreFreqs(fi);

        final int activeCoreIndex = MyUtil.getActiveCoreIndex(fi.freqs);
        final int currentCpuClock = fi.freqs[activeCoreIndex];

        // CPU クロック周波数のmin/max
        final int minFreq = fi.minFreqs[activeCoreIndex];
        final int maxFreq = fi.maxFreqs[activeCoreIndex];

        if (MyLog.debugMode) {
            MyLog.d("* CPU: " + currentCpuClock + " [" + minFreq + "," + maxFreq + "] [" + (System.currentTimeMillis() - mLastExecTask) + "ms]");
        }
        
        
        //-------------------------------------------------
        // CPU 使用率の取得
        //-------------------------------------------------
        // CPU 使用率の snapshot 取得
        int[] cpuUsages = null;
        if (!mUseFreqForCpuUsage) {
            final ArrayList<OneCpuInfo> currentCpuUsageSnapshot = CpuInfoCollector.takeCpuUsageSnapshot();
            if (currentCpuUsageSnapshot != null) {
                // CPU 使用率の算出
                cpuUsages = MyUtil.calcCpuUsages(currentCpuUsageSnapshot, mLastCpuUsageSnapshot);
                // 今回の snapshot を保存
                mLastCpuUsageSnapshot = currentCpuUsageSnapshot;
            } else {
                // 取得できないので fallback する
                mUseFreqForCpuUsage = true;
            }
        }
        if (cpuUsages == null) {
            // fallbackモード(Android 8.0以降では /proc/stat にアクセスできないのでコアの周波数からCPU使用率を算出する)
            cpuUsages = MyUtil.calcCpuUsagesByCoreFrequencies(fi);
        }


        //-------------------------------------------------
        // 通知判定
        //-------------------------------------------------
        // 前回と同じ CPU 使用率なら通知しない
        // ※通知アイコンだけなら丸めた値で比較すればいいんだけど通知テキストにCPU使用率%が出るので完全一致で比較する
        final boolean updated = isUpdated(currentCpuClock, cpuUsages);
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
            mNotificationPresenter.updateNotifications(cpuUsages, currentCpuClock, minFreq, maxFreq, mRequestForeground);
            mRequestForeground = false;
            
            // コールバック経由で通知
            distributeToCallbacks(cpuUsages, fi);
        }
        
        mLastExecTask = System.currentTimeMillis();
    }

    private boolean isUpdated(int currentCpuClock, int[] cpuUsages) {

        if (mLastCpuClock != currentCpuClock) {
            return true;
        } else if (mLastCpuUsages == null || cpuUsages == null) {
            return true;
        } else if (cpuUsages.length != mLastCpuUsages.length) {
            // コア数変動のケア(Galaxy S II等でよくあるらしい)
            return true;
        } else {
            // 同一でない値があれば更新する
            final int n = cpuUsages.length;
            for (int i=0; i<n; i++) {
                if (cpuUsages[i] != mLastCpuUsages[i]) {
                    return true;
                }
            }
        }
        return false;
    }

    private void distributeToCallbacks(int[] cpuUsages, AllCoreFrequencyInfo fi) {

        if (mCallbackListSize >= 1) {
            final int n = mCallbackList.beginBroadcast();

            // コールバック数を念のため更新しておく
            mCallbackListSize = n;

//                if (MyLog.debugMode) {
//                    MyLog.d("- broadcast:" + n);
//                }

            // 全コアのCPU周波数を収集する
            for (int i=0; i<n; i++) {
                try {
                    mCallbackList.getBroadcastItem(i).updateUsage(cpuUsages,
                            fi.freqs, fi.minFreqs, fi.maxFreqs);
                } catch (RemoteException e) {
//                      MyLog.e(e);
                }
            }
            mCallbackList.finishBroadcast();
        }
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
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert nm != null;
        nm.cancelAll();

        super.onDestroy();
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
        assert am != null;

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
        final NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert nm != null;
        nm.cancelAll();

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
        assert am != null;
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

                SystemClock.sleep(mConfig.intervalMs);

                if (mThreadActive && !mStopResident) {
                    execTask();
                }
            }

            MyLog.d("UsageUpdateService$GatherThread: done");
        }
    }
}
