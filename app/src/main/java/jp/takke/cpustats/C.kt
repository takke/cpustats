package jp.takke.cpustats

object C {

    val LOG_NAME = "CpuStats"

    // Preference keys
    val PREF_KEY_START_ON_BOOT = "StartOnBoot"

    val PREF_KEY_UPDATE_INTERVAL_SEC = "UpdateIntervalSec"
    val PREF_DEFAULT_UPDATE_INTERVAL_SEC = 5

    val PREF_KEY_SHOW_FREQUENCY_NOTIFICATION = "ShowFrequencyNotification"
    val PREF_KEY_SHOW_USAGE_NOTIFICATION = "ShowUsageNotification"

    // 初期Alarmの遅延時間[ms]
    val ALARM_STARTUP_DELAY_MSEC = 1000

    // Service維持のためのAlarmの更新間隔[ms]
    val ALARM_INTERVAL_MSEC = 60 * 1000

    // CPU使用率通知のアイコンモード
    val PREF_KEY_CORE_DISTRIBUTION_MODE = "CoreDistributionMode"
    val CORE_DISTRIBUTION_MODE_2ICONS = 0          // 最大2アイコン(デフォルト)
    val CORE_DISTRIBUTION_MODE_1ICON_UNSORTED = 1  // 1アイコン+非ソート
    val CORE_DISTRIBUTION_MODE_1ICON_SORTED = 2    // 1アイコン+ソート

    val READ_BUFFER_SIZE = 1024
}
