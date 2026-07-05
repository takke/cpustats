package jp.takke.cpustats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UsageHistoryBufferTest {

  @Test
  fun empty() {

    val buffer = UsageHistoryBuffer(capacity = 4)

    assertThat(buffer.toArray()).isEmpty()
  }

  @Test
  fun addWithinCapacity() {

    val buffer = UsageHistoryBuffer(capacity = 4)
    buffer.add(10)
    buffer.add(20)
    buffer.add(30)

    // 古い→新しい順
    assertThat(buffer.toArray()).containsExactly(10, 20, 30)
  }

  @Test
  fun addExactCapacity() {

    val buffer = UsageHistoryBuffer(capacity = 3)
    buffer.add(10)
    buffer.add(20)
    buffer.add(30)

    assertThat(buffer.toArray()).containsExactly(10, 20, 30)
  }

  @Test
  fun addOverCapacity() {

    val buffer = UsageHistoryBuffer(capacity = 3)
    buffer.add(10)
    buffer.add(20)
    buffer.add(30)
    buffer.add(40)
    buffer.add(50)

    // 古いものから捨てられる
    assertThat(buffer.toArray()).containsExactly(30, 40, 50)
  }

  @Test
  fun wrapAroundMultipleTimes() {

    val buffer = UsageHistoryBuffer(capacity = 3)
    for (i in 1..10) {
      buffer.add(i)
    }

    assertThat(buffer.toArray()).containsExactly(8, 9, 10)
  }
}
