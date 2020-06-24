package jp.takke.cpustats

import android.content.Context
import android.preference.PreferenceManager

internal class MyConfig {

    // 更新間隔
    var intervalMs = (C.PREF_DEFAULT_UPDATE_INTERVAL_SEC * 1000).toLong()

    // 通知
    var showUsageNotification = true
    var showFrequencyNotification = false

    // CPU使用率通知のアイコンモード
    var coreDistributionMode = C.CORE_DISTRIBUTION_MODE_2ICONS

    /**
     * 設定のロード
     */
    fun loadSettings(context: Context) {

        MyLog.i("load settings")

        val pref = PreferenceManager.getDefaultSharedPreferences(context)

        // 更新間隔
        val updateIntervalSec = pref.getString(C.PREF_KEY_UPDATE_INTERVAL_SEC, "" + C.PREF_DEFAULT_UPDATE_INTERVAL_SEC)
        try {
            intervalMs = (java.lang.Double.parseDouble(updateIntervalSec!!) * 1000.0).toInt().toLong()
            MyLog.i(" interval[" + intervalMs + "ms]")
        } catch (e: NumberFormatException) {
            MyLog.e(e)
        }

        // CPU使用率通知
        showUsageNotification = pref.getBoolean(C.PREF_KEY_SHOW_USAGE_NOTIFICATION, true)

        // クロック周波数通知
        showFrequencyNotification = pref.getBoolean(C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION, false)

        // CPU使用率通知のアイコンモード
        try {
            val s = pref.getString(C.PREF_KEY_CORE_DISTRIBUTION_MODE, "" + C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
            coreDistributionMode = s!!.toInt()
        } catch (e: NumberFormatException) {
            MyLog.e(e)
        }

    }


}
