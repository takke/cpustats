package jp.takke.cpustats

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class PreviewActivity : AppCompatActivity() {

    internal val mHandler = Handler()

    // 表示確認
    private var mIsForeground = false

    // サービス実行フラグ(メニュー切り替え用。コールバックがあれば実行中と判定する)
    private var mServiceMaybeRunning = false

    // サービスのIF
    private var mServiceIf: IUsageUpdateService? = null

    // コールバックIFの実装
    private val mCallback = object : IUsageUpdateCallback.Stub() {

        /**
         * サービスからの通知受信メソッド
         */
        @Throws(RemoteException::class)
        override fun updateUsage(cpuUsages: IntArray,
                                 freqs: IntArray, minFreqs: IntArray, maxFreqs: IntArray) {

            mHandler.post {

                // サービス実行中フラグを立てておく
                mServiceMaybeRunning = true

                if (mIsForeground) {
                    // CPUクロック周波数表示
                    showCpuFrequency(freqs, minFreqs, maxFreqs)

                    // CPU使用率表示更新
                    showCpuUsages(cpuUsages, freqs, minFreqs, maxFreqs)

//                    setTitle(getString(R.string.app_name) + "  Freq: " + MyUtil.formatFreq(currentFreq) + "");
                }
            }
        }
    }

    // サービスのコネクション
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {

            MyLog.i("PreviewActivity.onServiceConnected")

            // サービスのインターフェースを取得する
            mServiceIf = IUsageUpdateService.Stub.asInterface(service)

            try {
                // コールバック登録
                mServiceIf?.registerCallback(mCallback)

                // onActivityResult 後に設定がロードされていないかもしれないのでここで強制再ロードする
                mServiceIf?.reloadSettings()
            } catch (e: RemoteException) {
                MyLog.e(e)
            }

        }

        override fun onServiceDisconnected(name: ComponentName) {

            MyLog.i("PreviewActivity.onServiceDisconnected")

            mServiceIf = null
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)

        MyLog.d("PreviewActivity.onCreate")

        // とりあえず全消去
        hideAllCoreFreqInfo()

        // CPU クロック更新
        val coreCount = CpuInfoCollector.calcCpuCoreCount()
        val fi = AllCoreFrequencyInfo(coreCount)
        showCpuFrequency(fi.freqs, fi.minFreqs, fi.maxFreqs)

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
        setActionBarLogo(R.drawable.single000)

        // サービス開始
        doBindService()
    }

    private fun doBindService() {

        // サービスへのバインド開始
        val serviceIntent = Intent(this, UsageUpdateService::class.java)

        // start
        MyLog.d("PreviewActivity: startService of UsageUpdateService")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceIntent.putExtra("FOREGROUND_REQUEST", true)
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        // bind
        bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun setActionBarLogo(iconId: Int) {

//        val actionBar = supportActionBar
//        actionBar?.setLogo(iconId)

        val toolbar = findViewById<Toolbar?>(R.id.my_toolbar)
        toolbar?.setNavigationIcon(iconId)
//        toolbar.setLogo(iconId)
    }

    private fun hideAllCoreFreqInfo() {

        // Core
        val cores = intArrayOf(R.id.core1, R.id.core2, R.id.core3, R.id.core4, R.id.core5, R.id.core6, R.id.core7, R.id.core8)
        for (i in 0..7) {
            findViewById<View>(cores[i]).visibility = View.GONE

        }

        // Freq
        findViewById<View>(R.id.freqImage).visibility = View.GONE
        findViewById<View>(R.id.freqText1).visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        menuInflater.inflate(R.menu.activity_preview, menu)

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
                if (mServiceIf != null) {
                    MyLog.i("PreviewActivity: start")
                    try {
                        // 常駐開始
                        mServiceIf!!.startResident()
                    } catch (e: RemoteException) {
                        MyLog.e(e)
                    }

                }

            R.id.menu_stop_service ->
                // サービス停止
                if (mServiceIf != null) {
                    MyLog.i("PreviewActivity: stop")
                    try {
                        // 常駐停止
                        mServiceIf!!.stopResident()

                        // サービス実行中フラグを下ろしておく
                        mServiceMaybeRunning = false

                        // 表示初期化
                        hideAllCoreFreqInfo()

                        // タイトル初期化
                        title = "CPU Stats"

                        // Toolbarのアイコン初期化
                        setActionBarLogo(R.drawable.single000)

                    } catch (e: RemoteException) {
                        MyLog.e(e)
                    }

                }

            R.id.menu_settings -> {
                val intent = Intent(this, ConfigActivity::class.java)
                startActivityForResult(intent, REQUEST_CONFIG_ACTIVITY)
            }

            R.id.menu_about -> startActivity(Intent(this, AboutActivity::class.java))

            R.id.menu_exut ->
                // 終了
                finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            REQUEST_CONFIG_ACTIVITY -> {
                // 設定完了

                // 設定変更の内容をサービスに反映させる
                if (mServiceIf != null) {
                    try {
                        // 設定のリロード
                        MyLog.d("request reloadSettings")
                        mServiceIf!!.reloadSettings()
                    } catch (e: RemoteException) {
                        MyLog.e(e)
                    }
                } else {
                    MyLog.w("cannot reloadSettings (no service i.f.)")
                    // 接続後に再ロードする
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {

        MyLog.d("PreviewActivity.onDestroy")

        cleanupServiceConnection()

        super.onDestroy()
    }

    private fun cleanupServiceConnection() {

        // コールバック解除
        if (mServiceIf != null) {
            try {
                mServiceIf!!.unregisterCallback(mCallback)
            } catch (e: RemoteException) {
                MyLog.e(e)
            }

        }

        // サービスのbind解除
        unbindService(mServiceConnection)
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
    private fun showCpuFrequency(freqs: IntArray, minFreqs: IntArray, maxFreqs: IntArray) {

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
    private fun showCpuUsages(cpuUsages: IntArray?, freqs: IntArray, minFreqs: IntArray, maxFreqs: IntArray) {

//        MyLog.d("PreviewActivity.updateCpuUsages");

        // Toolbarのアイコン変更
        if (cpuUsages != null && cpuUsages.isNotEmpty()) {
            val id = ResourceUtil.getIconIdForCpuUsageSingleColor(cpuUsages[0])
            setActionBarLogo(id)
        }

        // プレビュー画面に表示
        val cores = intArrayOf(R.id.core1, R.id.core2, R.id.core3, R.id.core4, R.id.core5, R.id.core6, R.id.core7, R.id.core8)

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
//            MyLog.i("disp core[" + i + "] = " + cpuUsage + "% (max=" + maxFreqs[i] + ")");

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
            val color = ResourceUtil.getBackgroundColor(clockPercent)
            coreView.setBackgroundColor(color)
        }
    }

    companion object {

        // 設定画面
        private const val REQUEST_CONFIG_ACTIVITY = 0
    }

}
