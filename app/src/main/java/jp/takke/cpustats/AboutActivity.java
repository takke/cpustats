package jp.takke.cpustats;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);
        

        // ライセンス情報
        {
            final TextView textView = (TextView) findViewById(R.id.license_info);

            // リンク有効化
            final MovementMethod movementmethod = LinkMovementMethod.getInstance();
            textView.setMovementMethod(movementmethod);

            // HTML設定
            final CharSequence spanned = makeLicenceInfoText();
            textView.setText(spanned);
        }
        
        // バージョン情報
        {
            final TextView textView = (TextView) findViewById(R.id.app_version);
            
            // Version, Version code取得
            PackageInfo pinfo = null;
            try {
                pinfo = getPackageManager().getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                MyLog.e(e);
            }
            String versionCode = "?";
            String version = "x.x.x";
            if (pinfo != null) {
                versionCode = String.valueOf(pinfo.versionCode);
                version = pinfo.versionName;
            }

            final String versionString = getString(R.string.app_version)
                    .replace("{VERSION}", version)
                    .replace("{REVISION}", versionCode);
            textView.setText(versionString);
        }

        // アイコン長押しで内部情報ダンプ
        {
            final ImageView imageView = (ImageView) findViewById(R.id.icon);
            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    showInternalInfo();

                    return true;
                }
            });
        }
    }

    private CharSequence makeLicenceInfoText() {
        final String html =
            "<p>" +
            "Source code : <a href=\"https://github.com/takke/cpustats\">github/takke/cpustats</a>" +
            "</p>" +
            "<p>" +
            "Using cpuid code from " +
            "<a href=\"http://android-er.blogspot.in/2009/09/read-android-cpu-info.html\">" +
            "http://android-er.blogspot.in/2009/09/read-android-cpu-info.html" +
            "</a>" +
            "</p>" +
            "<b>Android CPU Info.:</b><br />" +
            "<pre>" + getCpuInfoText() + "</pre>";
        return Html.fromHtml(html);
    }

    /**
     * http://android-er.blogspot.in/2009/09/read-android-cpu-info.html
     */
    private String getCpuInfoText() {

        String result = "";

        try {
            final String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            final ProcessBuilder cmd = new ProcessBuilder(args);

            final Process process = cmd.start();
            final InputStream in = process.getInputStream();
            final InputStreamReader isr = new InputStreamReader(in);
            final BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                result += line + "<br />";
            }
            br.close();
        } catch (IOException ex) {
            MyLog.e(ex);
        }

        return result;
    }

    /**
     * 内部情報のダンプを取得し、表示する
     */
    private void showInternalInfo() {

        // dump
        final ArrayList<String> targets = new ArrayList<>();

        for (int i = 0; i < CpuInfoCollector.calcCpuCoreCount(); i++) {

            targets.add("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
            targets.add("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_min_freq");
            targets.add("/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq");
            targets.add("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_available_frequencies");
        }
        targets.add("/proc/stat");

        final StringBuilder sb = new StringBuilder();
        for (String target : targets) {
            readFileInfoAsHtml(target, sb);
        }

        // 表示
        final TextView textView = (TextView) findViewById(R.id.license_info);
        final SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(makeLicenceInfoText());
        ssb.append(Html.fromHtml(sb.toString()));
        textView.setText(ssb);

        Toast.makeText(this, "updated", Toast.LENGTH_SHORT).show();
    }

    private void readFileInfoAsHtml(String filepath, StringBuilder sb) {

        sb.append("----<br>");
        sb.append("<b>").append(filepath).append("</b><br />");
        sb.append("<pre>");
        try {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filepath)), C.READ_BUFFER_SIZE);

            String line;
            while ((line=reader.readLine()) != null) {
                sb.append(line).append("<br />");
            }

            reader.close();
        } catch (Exception ex) {
            MyLog.e(ex);
            sb.append(ex.getMessage());
        }
        sb.append("</pre>");
    }

}
