package jp.takke.cpustats;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class PreviewActivity extends Activity {
    
    final Handler mHandler = new Handler();
    
    // 表示確認
    private boolean mIsForeground = false;
    
    // CPUクロック周波数(最小、最大)の文字列キャッシュ
    private int mMinFreq = 0;
    private String mMinFreqText = "";
    private int mMaxFreq = 0;
    private String mMaxFreqText = "";

    // 設定画面
    private final static int REQUEST_CONFIG_ACTIVITY = 0;
    
    // サービス実行フラグ(メニュー切り替え用。コールバックがあれば実行中と判定する)
    private boolean mServiceMaybeRunning = false;
    
    // サービスのIF
    private IUsageUpdateService mServiceIf = null;
    
    // コールバックIFの実装
    private IUsageUpdateCallback mCallback = new IUsageUpdateCallback.Stub() {
        
        /**
         * サービスからの通知受信メソッド
         */
        public void updateUsage(final int[] cpuUsages, final int currentFreq) throws RemoteException {
            
            mHandler.post(new Runnable() {
                
                public void run() {
                    
                    // サービス実行中フラグを立てておく
                    mServiceMaybeRunning = true;
                    
                    if (mIsForeground) {
                        // CPU使用率表示更新
                        updateCpuUsages(cpuUsages);
                        
                        // CPUクロック周波数表示更新
                        updateCpuFrequency(currentFreq);
                        
                        setTitle("CPU Stats  Freq: " + MyUtil.formatFreq(currentFreq) + "");
                    }
                }
            });
        }
    };

    // サービスのコネクション
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        
        public void onServiceConnected(ComponentName name, IBinder service) {
            
            // サービスのインターフェースを取得する
            mServiceIf = IUsageUpdateService.Stub.asInterface(service);
            
            // コールバック登録
            try {
                mServiceIf.registerCallback(mCallback);
            } catch (RemoteException e) {
                MyLog.e(e);
            }
        }
        
        public void onServiceDisconnected(ComponentName name) {
            mServiceIf = null;
        }
    };

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        
        MyLog.debugMode = false;
        MyLog.d("PreviewActivity.onCreate");
        
        // とりあえず全消去
        hideAllCoreFreqInfo();
        
        // CPU クロック更新
        mMinFreq = MyUtil.takeMinCpuFreq();
        mMinFreqText = MyUtil.formatFreq(mMinFreq);
        
        mMaxFreq = MyUtil.takeMaxCpuFreq();
        mMaxFreqText = MyUtil.formatFreq(mMaxFreq);
        updateCpuFrequency(MyUtil.takeCurrentCpuFreq());
        
        // アクションバーのアイコン変更
        setActionBarLogo(R.drawable.single000);
        
        // サービスへのバインド開始
        final Intent intent = new Intent(IUsageUpdateService.class.getName());
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void setActionBarLogo(int iconId) {
        
        if (Build.VERSION.SDK_INT >= 14) {
            // getActionBar().setLogo(R.drawable.single000);
            try {
                final Object actionBar = this.getClass().getMethod("getActionBar")
                    .invoke(this);
                actionBar.getClass().getMethod("setLogo", int.class)
                    .invoke(actionBar, iconId);
            } catch (Throwable th) {
                MyLog.e(th);
            }
        }
    }

    private void hideAllCoreFreqInfo() {
        
        // Core
        final int[] imageViews = new int[]{R.id.core1Image, R.id.core2Image, R.id.core3Image, R.id.core4Image};
        final int[] titleViews = new int[]{R.id.core1Title, R.id.core2Title, R.id.core3Title, R.id.core4Title};
        final int[] textViews = new int[]{R.id.core1Text, R.id.core2Text, R.id.core3Text, R.id.core4Text};
        
        for (int i=0; i<4; i++) {
            findViewById(imageViews[i]).setVisibility(View.GONE);
            findViewById(titleViews[i]).setVisibility(View.GONE);
            findViewById(textViews[i]).setVisibility(View.GONE);
        }
        
        // Freq
        findViewById(R.id.freqImage).setVisibility(View.GONE);
        findViewById(R.id.freqText1).setVisibility(View.GONE);
        findViewById(R.id.freqText2).setVisibility(View.GONE);
        findViewById(R.id.freqText3).setVisibility(View.GONE);
        findViewById(R.id.freqTitle).setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.activity_preview, menu);
        
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        
        // サービス状態によってmenu変更
        {
            final MenuItem item = menu.findItem(R.id.menu_start_service);
            if (item != null) {
                item.setVisible(!mServiceMaybeRunning);
            }
        }
        {
            final MenuItem item = menu.findItem(R.id.menu_stop_service);
            if (item != null) {
                item.setVisible(mServiceMaybeRunning);
            }
        }
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch (item.getItemId()) {
        case R.id.menu_start_service:
            // サービス開始
            if (mServiceIf != null) {
                try {
                    // 常駐開始
                    mServiceIf.startResident();
                } catch (RemoteException e) {
                    MyLog.e(e);
                }
            }
            break;
            
        case R.id.menu_stop_service:
            // サービス停止
            if (mServiceIf != null) {
                try {
                    // 常駐停止
                    mServiceIf.stopResident();
                    
                    // サービス実行中フラグを下ろしておく
                    mServiceMaybeRunning = false;
                    
                    // 表示初期化
                    hideAllCoreFreqInfo();
                    
                    // タイトル初期化
                    setTitle("CPU Stats");
                    
                    // アクションバーのアイコン初期化
                    setActionBarLogo(R.drawable.single000);
                    
                } catch (RemoteException e) {
                    MyLog.e(e);
                }
            }
            break;
            
        case R.id.menu_cpuinfo:
            startActivity(new Intent(this, CpuInfoActivity.class));
            break;

        case R.id.menu_settings:
            {
                final Intent intent = new Intent(this, ConfigActivity.class);
                startActivityForResult(intent, REQUEST_CONFIG_ACTIVITY);
            }
            break;

        case R.id.menu_exut:
            // 終了
            finish();
            break;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        switch (requestCode) {
        case REQUEST_CONFIG_ACTIVITY:
            {
                // 設定完了
                
                // 設定変更の内容をサービスに反映させる
                if (mServiceIf != null) {
                    try {
                        // 設定のリロード
                        mServiceIf.reloadSettings();
                    } catch (RemoteException e) {
                        MyLog.e(e);
                    }
                }
            }
            break;
        }
        
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        
        MyLog.d("PreviewActivity.onDestroy");
        
        cleanupServiceConnection();
        
        super.onDestroy();
    }

    private void cleanupServiceConnection() {
        
        // コールバック解除
        if (mServiceIf != null) {
            try {
                mServiceIf.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                MyLog.e(e);
            }
        }
        
        // サービスのbind解除
        unbindService(mServiceConnection);
    }
    
    @Override
    protected void onPause() {
        
        MyLog.d("onPause");
        
        mIsForeground = false;
        
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        
        MyLog.d("onResume");
        
        mIsForeground = true;
        
        super.onResume();
    }
    
    /**
     * CPU 使用率を画面に表示する
     * 
     * @param cpuUsages
     */
    private void updateCpuUsages(int[] cpuUsages) {
        
        MyLog.d("updateCpuUsages");
        
        // アクションバーのアイコン変更
        if (cpuUsages != null && cpuUsages.length >= 1) {
            final int id = ResourceUtil.getIconIdForCpuUsageSingle(cpuUsages[0]);
            setActionBarLogo(id);
        }
        
        // プレビュー画面に表示
        final int[] imageViews = new int[]{R.id.core1Image, R.id.core2Image, R.id.core3Image, R.id.core4Image};
        final int[] titleViews = new int[]{R.id.core1Title, R.id.core2Title, R.id.core3Title, R.id.core4Title};
        final int[] textViews = new int[]{R.id.core1Text, R.id.core2Text, R.id.core3Text, R.id.core4Text};
        
        final int coreCount = MyUtil.calcCpuCoreCount();
        for (int i=0; i<4; i++) {
            
            if (coreCount <= i) {
                // Coreが少ないので消去
                findViewById(imageViews[i]).setVisibility(View.GONE);
                findViewById(titleViews[i]).setVisibility(View.GONE);
                findViewById(textViews[i]).setVisibility(View.GONE);
            } else {
                int cpuUsage = 0;
                if (cpuUsages != null && cpuUsages.length > i+1) {
                    cpuUsage = cpuUsages[i+1];
                }

                final TextView textView = (TextView) findViewById(textViews[i]);
                textView.setText("CPU Usage: " + cpuUsage + "%");
                textView.setVisibility(View.VISIBLE);
                
                findViewById(titleViews[i]).setVisibility(View.VISIBLE);
                
                final int id = ResourceUtil.getIconIdForCpuUsageSingle(cpuUsage);
                final ImageView imageView = (ImageView) findViewById(imageViews[i]);
                imageView.setImageResource(id);
                imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * CPU クロック周波数を画面に表示する
     * 
     * @param currentFreq
     */
    private void updateCpuFrequency(final int currentFreq) {
        
        MyLog.d("updateCpuFrequency");
        
        final TextView textView1 = (TextView) findViewById(R.id.freqText1);
        textView1.setText("Freq: " + MyUtil.formatFreq(currentFreq));
        textView1.setVisibility(View.VISIBLE);
        
        final TextView textView2 = (TextView) findViewById(R.id.freqText2);
        textView2.setText("Max: " + mMaxFreqText);
        textView2.setVisibility(View.VISIBLE);
        
        final TextView textView3 = (TextView) findViewById(R.id.freqText3);
        textView3.setText("Min: " + mMinFreqText);
        textView3.setVisibility(View.VISIBLE);
        
        findViewById(R.id.freqTitle).setVisibility(View.VISIBLE);
        
//      final int clockPercent = mMaxFreq >= 0 ? ((currentFreq - mMinFreq) * 100 / (mMaxFreq - mMinFreq)) : 0;
//      MyLog.i(" clock[" + currentFreq + "] => " + clockPercent + "%");
        
        final int id = ResourceUtil.getIconIdForCpuFreq(currentFreq);
        final ImageView imageView = (ImageView) findViewById(R.id.freqImage);
        imageView.setImageResource(id);
        imageView.setVisibility(View.VISIBLE);
    }
    
}
