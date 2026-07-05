package jp.takke.cpustats

/**
 * CPU使用率(全体平均)の履歴を保持するリングバッファ
 *
 * 収集スレッドからの追加と UI スレッドからの読み出しが競合するため synchronized で保護する
 */
class UsageHistoryBuffer(val capacity: Int = C.USAGE_HISTORY_CAPACITY) {

  private val buffer = IntArray(capacity)

  // 次の書き込み位置
  private var head = 0

  private var size = 0

  @Synchronized
  fun add(value: Int) {
    buffer[head] = value
    head = (head + 1) % capacity
    if (size < capacity) {
      size++
    }
  }

  /**
   * 古い→新しい順の配列を返す
   */
  @Synchronized
  fun toArray(): IntArray {
    val result = IntArray(size)
    val start = (head - size + capacity) % capacity
    for (i in 0 until size) {
      result[i] = buffer[(start + i) % capacity]
    }
    return result
  }
}
