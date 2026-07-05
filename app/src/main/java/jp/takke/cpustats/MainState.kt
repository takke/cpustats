package jp.takke.cpustats

/**
 * MainActivity が Compose に渡すサービス状態
 */
data class MainState(
  val cpuUsages: IntArray = IntArray(0),
  val freqs: IntArray = IntArray(0),
  val minFreqs: IntArray = IntArray(0),
  val maxFreqs: IntArray = IntArray(0),
  val systemStats: SystemStats = SystemStats(),
  val history: IntArray = IntArray(0),
  val serviceRunning: Boolean = false,
) {
  // data class の IntArray 比較を配列内容ベースにする(recompose 判定用)
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MainState) return false
    return cpuUsages.contentEquals(other.cpuUsages) &&
        freqs.contentEquals(other.freqs) &&
        minFreqs.contentEquals(other.minFreqs) &&
        maxFreqs.contentEquals(other.maxFreqs) &&
        systemStats == other.systemStats &&
        history.contentEquals(other.history) &&
        serviceRunning == other.serviceRunning
  }

  override fun hashCode(): Int {
    var r = cpuUsages.contentHashCode()
    r = 31 * r + freqs.contentHashCode()
    r = 31 * r + minFreqs.contentHashCode()
    r = 31 * r + maxFreqs.contentHashCode()
    r = 31 * r + systemStats.hashCode()
    r = 31 * r + history.contentHashCode()
    r = 31 * r + serviceRunning.hashCode()
    return r
  }
}
