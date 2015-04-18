package jp.takke.cpustats;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
            final CharSequence spanned = Html.fromHtml(html);
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
}
