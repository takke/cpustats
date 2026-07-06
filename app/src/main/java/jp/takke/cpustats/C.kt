package jp.takke.cpustats

object C {

  const val LOG_NAME = "CpuStats"

  // Preference keys
  const val PREF_KEY_START_ON_BOOT = "StartOnBoot"

  const val PREF_KEY_UPDATE_INTERVAL_SEC = "UpdateIntervalSec"
  const val PREF_DEFAULT_UPDATE_INTERVAL_SEC = 5

  const val PREF_KEY_SHOW_FREQUENCY_NOTIFICATION = "ShowFrequencyNotification"
  const val PREF_KEY_SHOW_USAGE_NOTIFICATION = "ShowUsageNotification"

  // サービス起動直後の初回収集までの遅延時間[ms]
  const val STARTUP_DELAY_MSEC = 1000

  // CPU使用率履歴(スパークライン用)の保持件数
  const val USAGE_HISTORY_CAPACITY = 120

  // 履歴記録の設定キー
  const val PREF_KEY_RECORD_HISTORY = "RecordHistory"

  // 履歴記録の間引き間隔[ms](更新間隔が短くてもこれ以上の頻度では記録しない)
  const val HISTORY_RECORD_INTERVAL_MS = 10 * 1000L

  // 履歴の保持期間[ms]
  const val HISTORY_RETENTION_MS = 24 * 60 * 60 * 1000L

  // ホーム画面ウィジェットの最小更新間隔[ms](更新間隔 0.5 秒設定などでランチャーに負荷をかけないための下限)
  const val WIDGET_UPDATE_MIN_INTERVAL_MS = 5 * 1000L

  // CPU使用率通知のアイコンモード
  const val PREF_KEY_CORE_DISTRIBUTION_MODE = "CoreDistributionMode"
  const val CORE_DISTRIBUTION_MODE_2ICONS = 0          // 最大2アイコン(デフォルト)
  const val CORE_DISTRIBUTION_MODE_1ICON_UNSORTED = 1  // 1アイコン+非ソート
  const val CORE_DISTRIBUTION_MODE_1ICON_SORTED = 2    // 1アイコン+ソート

  const val READ_BUFFER_SIZE = 1024
}
