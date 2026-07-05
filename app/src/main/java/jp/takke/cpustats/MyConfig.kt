package jp.takke.cpustats

import android.content.Context
import androidx.preference.PreferenceManager
import jp.takke.cpustats.MyConfig.Companion.load

/**
 * 設定値(immutable)
 *
 * 設定変更時は [load] で新しいインスタンスを生成して差し替えること
 */
internal data class MyConfig(
  // 更新間隔
  val intervalMs: Long = (C.PREF_DEFAULT_UPDATE_INTERVAL_SEC * 1000).toLong(),
  // CPU使用率通知
  val showUsageNotification: Boolean = true,
  // クロック周波数通知
  val showFrequencyNotification: Boolean = false,
  // CPU使用率通知のアイコンモード
  val coreDistributionMode: Int = C.CORE_DISTRIBUTION_MODE_2ICONS,
) {

  companion object {

    /**
     * 設定のロード
     */
    fun load(context: Context): MyConfig {

      MyLog.i("load settings")

      val pref = PreferenceManager.getDefaultSharedPreferences(context)

      // 更新間隔
      var intervalMs = (C.PREF_DEFAULT_UPDATE_INTERVAL_SEC * 1000).toLong()
      val updateIntervalSec =
        pref.getString(C.PREF_KEY_UPDATE_INTERVAL_SEC, "" + C.PREF_DEFAULT_UPDATE_INTERVAL_SEC)
      try {
        intervalMs = (java.lang.Double.parseDouble(updateIntervalSec!!) * 1000.0).toInt().toLong()
        MyLog.i(" interval[" + intervalMs + "ms]")
      } catch (e: NumberFormatException) {
        MyLog.e(e)
      }

      // CPU使用率通知のアイコンモード
      var coreDistributionMode = C.CORE_DISTRIBUTION_MODE_2ICONS
      try {
        val s = pref.getString(
          C.PREF_KEY_CORE_DISTRIBUTION_MODE,
          "" + C.CORE_DISTRIBUTION_MODE_1ICON_SORTED
        )
        coreDistributionMode = s!!.toInt()
      } catch (e: NumberFormatException) {
        MyLog.e(e)
      }

      return MyConfig(
        intervalMs = intervalMs,
        showUsageNotification = pref.getBoolean(C.PREF_KEY_SHOW_USAGE_NOTIFICATION, true),
        showFrequencyNotification = pref.getBoolean(
          C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION, false
        ),
        coreDistributionMode = coreDistributionMode,
      )
    }
  }
}
