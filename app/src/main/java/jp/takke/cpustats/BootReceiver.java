package jp.takke.cpustats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        
        MyLog.i("BootReceiver.onReceive");

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // 端末起動時の処理
            
            // 自動起動の確認
            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean startOnBoot = pref.getBoolean(C.PREF_KEY_START_ON_BOOT, false);
            
            MyLog.i("start on boot[" + (startOnBoot ? "YES" : "NO") + "]");
            
            if (startOnBoot) {
                // サービス起動
                final Intent serviceIntent = new Intent(context, UsageUpdateService.class);
                if (Build.VERSION.SDK_INT >= 26) {
                    serviceIntent.putExtra("FOREGROUND_REQUEST", true);
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
        
    }
}
