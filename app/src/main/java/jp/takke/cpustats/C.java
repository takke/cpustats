package jp.takke.cpustats;

public class C {

    public static final String LOG_NAME = "CpuStats";
    
    // Preference keys
    public static final String PREF_KEY_START_ON_BOOT = "StartOnBoot";

    public static final String PREF_KEY_UPDATE_INTERVAL_SEC = "UpdateIntervalSec";
    public static final int PREF_DEFAULT_UPDATE_INTERVAL_SEC = 5;

    public static final String PREF_KEY_SHOW_FREQUENCY_NOTIFICATION = "ShowFrequencyNotification";
    public static final String PREF_KEY_SHOW_USAGE_NOTIFICATION = "ShowUsageNotification";

    // 初期Alarmの遅延時間[ms]
    public static final int ALARM_STARTUP_DELAY_MSEC = 1000;

    // Service維持のためのAlarmの更新間隔[ms]
    public static final int ALARM_INTERVAL_MSEC = 60 * 1000;

    // CPU使用率通知のアイコンモード
    public static final String PREF_KEY_CORE_DISTRIBUTION_MODE = "CoreDistributionMode";
    public static final int CORE_DISTRIBUTION_MODE_2ICONS = 0;          // 最大2アイコン(デフォルト)
    public static final int CORE_DISTRIBUTION_MODE_1ICON_UNSORTED = 1;  // 1アイコン+非ソート
    public static final int CORE_DISTRIBUTION_MODE_1ICON_SORTED = 2;    // 1アイコン+ソート

}
