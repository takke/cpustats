package jp.takke.cpustats

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

class AboutActivity : AppCompatActivity() {

    /**
     * http://android-er.blogspot.in/2009/09/read-android-cpu-info.html
     */
    private val cpuInfoText: String
        get() {

            var result = ""

            try {
                val args = arrayOf("/system/bin/cat", "/proc/cpuinfo")
                val cmd = ProcessBuilder(*args)

                val process = cmd.start()
                val `in` = process.inputStream
                val isr = InputStreamReader(`in`)
                val br = BufferedReader(isr)

                br.forEachLine {
                    result += "$it<br />"
                }
                br.close()
            } catch (ex: IOException) {
                MyLog.e(ex)
            }

            return result
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)


        // ライセンス情報
        run {
            val textView = findViewById<View>(R.id.license_info) as TextView

            // リンク有効化
            val movementMethod = LinkMovementMethod.getInstance()
            textView.movementMethod = movementMethod

            // HTML設定
            val spanned = makeLicenceInfoText()
            textView.text = spanned
        }

        // バージョン情報
        run {
            val textView = findViewById<View>(R.id.app_version) as TextView

            // Version, Version code取得
            var pinfo: PackageInfo? = null
            try {
                pinfo = packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_META_DATA)
            } catch (e: PackageManager.NameNotFoundException) {
                MyLog.e(e)
            }

            var versionCode = "?"
            var version = "x.x.x"
            if (pinfo != null) {
                @Suppress("DEPRECATION")
                versionCode = pinfo.versionCode.toString()
                version = pinfo.versionName
            }

            val versionString = getString(R.string.app_version)
                    .replace("{VERSION}", version)
                    .replace("{REVISION}", versionCode)
            textView.text = versionString
        }

        // アイコン長押しで内部情報ダンプ
        run {
            val imageView = findViewById<View>(R.id.icon) as ImageView
            imageView.setOnLongClickListener {
                showInternalInfo()

                true
            }
        }
    }

    private fun makeLicenceInfoText(): CharSequence {
        val html = """
<p>Source code : <a href="https://github.com/takke/cpustats">github/takke/cpustats</a></p>

<p>Using cpuid code from
 <a href="http://android-er.blogspot.in/2009/09/read-android-cpu-info.html">http://android-er.blogspot.in/2009/09/read-android-cpu-info.html</a>
</p>

<b>Android CPU Info.:</b><br />
<pre>$cpuInfoText</pre>
""".trimIndent()

        @Suppress("DEPRECATION")
        return Html.fromHtml(html)
    }

    /**
     * 内部情報のダンプを取得し、表示する
     */
    private fun showInternalInfo() {

        // dump
        val targets = ArrayList<String>()

        for (i in 0 until CpuInfoCollector.calcCpuCoreCount()) {

            targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
            targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_available_frequencies")
        }
        targets.add("/proc/stat")

        val sb = StringBuilder()
        for (target in targets) {
            readFileInfoAsHtml(target, sb)
        }

        // 表示
        val textView = findViewById<View>(R.id.license_info) as TextView
        val ssb = SpannableStringBuilder()
        ssb.append(makeLicenceInfoText())
        @Suppress("DEPRECATION")
        ssb.append(Html.fromHtml(sb.toString()))
        textView.text = ssb

        Toast.makeText(this, "updated", Toast.LENGTH_SHORT).show()
    }

    private fun readFileInfoAsHtml(filepath: String, sb: StringBuilder) {

        sb.append("----<br>")
        sb.append("<b>").append(filepath).append("</b><br />")
        sb.append("<pre>")
        try {
            val reader = BufferedReader(
                    InputStreamReader(FileInputStream(filepath)), C.READ_BUFFER_SIZE)

            reader.forEachLine {
                sb.append(it).append("<br />")
            }

            reader.close()
        } catch (ex: Exception) {
            MyLog.e(ex)
            sb.append(ex.message)
        }

        sb.append("</pre>")
    }

}
