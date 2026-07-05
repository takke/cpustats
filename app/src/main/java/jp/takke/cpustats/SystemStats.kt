package jp.takke.cpustats

/**
 * CPU 以外のシステム情報(コールバック配信用)
 */
data class SystemStats(
  // バッテリー温度(0.1℃単位、取得できない場合は0)
  val batteryTemperature: Int = 0,
  // 使用中メモリ[MB]
  val memUsedMb: Int = 0,
  // 合計メモリ[MB]
  val memTotalMb: Int = 0,
)
