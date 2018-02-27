package jp.takke.cpustats;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class PreviewActivity extends Activity {
    
    // 設定画面
    private final static int REQUEST_CONFIG_ACTIVITY = 0;

    final Handler mHandler = new Handler();

    // 表示確認
    private boolean mIsForeground = false;

    // サービス実行フラグ(メニュー切り替え用。コールバックがあれば実行中と判定する)
    private boolean mServiceMaybeRunning = false;
    
    // サービスのIF
    private IUsageUpdateService mServiceIf = null;
    
    // コールバックIFの実装
    private IUsageUpdateCallback mCallback = new IUsageUpdateCallback.Stub() {

        /**
         * サービスからの通知受信メソッド
         */
        @Override
        public void updateUsage(final int[] cpuUsages,
                                int[] freqs, int[] minFreqs, int[] maxFreqs) throws RemoteException {

            mHandler.post(() -> {

                // サービス実行中フラグを立てておく
                mServiceMaybeRunning = true;

                if (mIsForeground) {
                    // CPUクロック周波数表示
                    showCpuFrequency(freqs, minFreqs, maxFreqs);

                    // CPU使用率表示更新
                    showCpuUsages(cpuUsages, freqs, minFreqs, maxFreqs);

//                    setTitle(getString(R.string.app_name) + "  Freq: " + MyUtil.formatFreq(currentFreq) + "");
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
        
//        MyLog.debugMode = true;
        MyLog.d("PreviewActivity.onCreate");
        
        // とりあえず全消去
        hideAllCoreFreqInfo();
        
        // CPU クロック更新
        final int coreCount = CpuInfoCollector.calcCpuCoreCount();
        final AllCoreFrequencyInfo fi = new AllCoreFrequencyInfo(coreCount);
        showCpuFrequency(fi.freqs, fi.minFreqs, fi.maxFreqs);

        // CPU使用率表示更新
        final int[] dummyCpuUsages = new int[coreCount];
        for (int i = 0; i < dummyCpuUsages.length; i++) {
            dummyCpuUsages[i] = 0;
        }
        CpuInfoCollector.takeAllCoreFreqs(fi);
        showCpuUsages(dummyCpuUsages, fi.freqs, fi.minFreqs, fi.maxFreqs);

        // アクションバーのアイコン変更
        setActionBarLogo(R.drawable.single000);
        
        // サービスへのバインド開始
        final Intent intent = new Intent(IUsageUpdateService.class.getName());
        intent.setPackage(getPackageName());
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
        final int[] cores = new int[]{R.id.core1, R.id.core2, R.id.core3, R.id.core4, R.id.core5, R.id.core6, R.id.core7, R.id.core8};
        for (int i=0; i<8; i++) {
            findViewById(cores[i]).setVisibility(View.GONE);

        }

        // Freq
        findViewById(R.id.freqImage).setVisibility(View.GONE);
        findViewById(R.id.freqText1).setVisibility(View.GONE);
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
                MyLog.i("PreviewActivity: start");
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
                MyLog.i("PreviewActivity: stop");
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
            
        case R.id.menu_settings:
            {
                final Intent intent = new Intent(this, ConfigActivity.class);
                startActivityForResult(intent, REQUEST_CONFIG_ACTIVITY);
            }
            break;

        case R.id.menu_about:
            startActivity(new Intent(this, AboutActivity.class));
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
     * CPU クロック周波数を画面に表示する
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    @SuppressLint("SetTextI18n")
    private void showCpuFrequency(final int[] freqs, int[] minFreqs, int[] maxFreqs) {

        // 全コアのうち最適なクロック周波数を探す
        final int activeCoreIndex = MyUtil.getActiveCoreIndex(freqs);
        final int currentFreq = freqs[activeCoreIndex];
        final int minFreq = minFreqs[activeCoreIndex];
        final int maxFreq = maxFreqs[activeCoreIndex];

        final TextView textView1 = (TextView) findViewById(R.id.freqText1);
        final SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append(MyUtil.formatFreq(currentFreq));

        // アクティブなコア番号と(周波数による)負荷
        {
            final int start = ssb.length();
            final int clockPercent = MyUtil.getClockPercent(currentFreq, minFreq, maxFreq);
            ssb.append("  [Core " + (activeCoreIndex+1) + ": " + clockPercent + "%]");
            ssb.setSpan(new RelativeSizeSpan(0.8f), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//          MyLog.i(" clock[" + currentFreq + "] => " + clockPercent + "%");
        }

        // 周波数の min/max
        {
            ssb.append("\n");
            final String minFreqText = MyUtil.formatFreq(minFreq);
            final String maxFreqText = MyUtil.formatFreq(maxFreq);
            final int start = ssb.length();
            ssb.append(" (" + minFreqText + " - " + maxFreqText + ")");
            ssb.setSpan(new RelativeSizeSpan(0.8f), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        textView1.setText(ssb);
        textView1.setVisibility(View.VISIBLE);


        final int id = ResourceUtil.getIconIdForCpuFreq(currentFreq);
        final ImageView imageView = (ImageView) findViewById(R.id.freqImage);
        imageView.setImageResource(id);
        imageView.setVisibility(View.VISIBLE);
    }

    /**
     * CPU 使用率を画面に表示する
     */
    @SuppressWarnings("StringConcatenationInsideStringBufferAppend")
    @SuppressLint("SetTextI18n")
    private void showCpuUsages(int[] cpuUsages, int[] freqs, int[] minFreqs, int[] maxFreqs) {
        
//        MyLog.d("PreviewActivity.updateCpuUsages");
        
        // アクションバーのアイコン変更
        if (cpuUsages != null && cpuUsages.length >= 1) {
            final int id = ResourceUtil.getIconIdForCpuUsageSingle(cpuUsages[0]);
            setActionBarLogo(id);
        }
        
        // プレビュー画面に表示
        final int[] cores = new int[]{R.id.core1, R.id.core2, R.id.core3, R.id.core4, R.id.core5, R.id.core6, R.id.core7, R.id.core8};

        final int coreCount = CpuInfoCollector.calcCpuCoreCount();
        for (int i=0; i<8; i++) {
            
            if (coreCount <= i) {
                // Coreが少ないので消去
                findViewById(cores[i]).setVisibility(View.GONE);
                continue;
            }


            final View coreView = findViewById(cores[i]);
            coreView.setVisibility(View.VISIBLE);

            int cpuUsage = 0;
            if (cpuUsages != null && cpuUsages.length > i+1) {
                cpuUsage = cpuUsages[i+1];
            }

            //--------------------------------------------------
            // アイコン設定
            //--------------------------------------------------
            final int id = ResourceUtil.getIconIdForCpuUsageSingle(cpuUsage);
            final ImageView imageView = (ImageView) coreView.findViewById(R.id.coreImage);
            imageView.setImageResource(id);
            imageView.setVisibility(View.VISIBLE);

            //--------------------------------------------------
            // テキスト設定
            //--------------------------------------------------
            final TextView textView = (TextView) coreView.findViewById(R.id.coreText);
            final SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append("Core" + (i + 1) + ": " + cpuUsage + "%");

            // 周波数
            String freqText = MyUtil.formatFreq(freqs[i]);
            ssb.append("\n");
            ssb.append(" " + freqText);

            // 周波数による負荷
            final int clockPercent = MyUtil.getClockPercent(freqs[i], minFreqs[i], maxFreqs[i]);
            {
                final int start = ssb.length();
                ssb.append(" [" + clockPercent + "%]");
                ssb.setSpan(new RelativeSizeSpan(0.8f), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // 周波数の min/max
            {
                ssb.append("\n");
                final int start = ssb.length();
                final String minFreqText = MyUtil.formatFreq(minFreqs[i]);
                final String maxFreqText = MyUtil.formatFreq(maxFreqs[i]);
                ssb.append(" (" + minFreqText + " - " + maxFreqText + ")");
                ssb.setSpan(new RelativeSizeSpan(0.8f), start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            textView.setText(ssb);
            textView.setVisibility(View.VISIBLE);

            // アクティブコアの背景色を変える
            if (clockPercent > 0) {
                coreView.setBackgroundColor(0xff333333);
            } else {
                coreView.setBackgroundColor(0xff222222);
            }
        }
    }

}
