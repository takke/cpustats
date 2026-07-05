package jp.takke.cpustats

/**
 * CPU 使用率・クロック周波数の更新通知を受け取るコールバック
 *
 * ※サービスの収集スレッドから呼ばれるため、UI 更新する場合は呼び出し側でメインスレッドに切り替えること
 */
fun interface UsageUpdateCallback {

  fun onUsageUpdated(
    cpuUsages: IntArray,
    freqs: IntArray,
    minFreqs: IntArray,
    maxFreqs: IntArray,
    batteryTemperature: Int  // 0.1℃単位(取得できない場合は0)
  )
}
