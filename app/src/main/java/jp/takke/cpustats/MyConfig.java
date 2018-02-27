package jp.takke.cpustats;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

@SuppressWarnings("WeakerAccess")
class MyConfig {

    // 更新間隔
    public long intervalMs = C.PREF_DEFAULT_UPDATE_INTERVAL_SEC * 1000;

    // 通知
    public boolean showUsageNotification = true;
    public boolean showFrequencyNotification = false;

    // CPU使用率通知のアイコンモード
    public int coreDistributionMode = C.CORE_DISTRIBUTION_MODE_2ICONS;

    /**
     * 設定のロード
     */
    public void loadSettings(Context context) {

        MyLog.i("load settings");

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        // 更新間隔
        final String updateIntervalSec = pref.getString(C.PREF_KEY_UPDATE_INTERVAL_SEC, ""+C.PREF_DEFAULT_UPDATE_INTERVAL_SEC);
        try {
            intervalMs = (int)(Double.parseDouble(updateIntervalSec) * 1000.0);
            MyLog.i(" interval[" + intervalMs + "ms]");
        } catch (NumberFormatException e) {
            MyLog.e(e);
        }

        // CPU使用率通知
        showUsageNotification = pref.getBoolean(C.PREF_KEY_SHOW_USAGE_NOTIFICATION, true);

        // クロック周波数通知
        showFrequencyNotification = pref.getBoolean(C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION, false);

        // CPU使用率通知のアイコンモード
        try {
            final String s = pref.getString(C.PREF_KEY_CORE_DISTRIBUTION_MODE, ""+C.CORE_DISTRIBUTION_MODE_2ICONS);
            coreDistributionMode = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            MyLog.e(e);
        }
    }


}
