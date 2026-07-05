package jp.takke.cpustats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.BufferedReader
import java.io.StringReader

class CpuInfoCollectorTest {

  private fun readerOf(text: String) = BufferedReader(StringReader(text))

  @Test
  fun parseCpuUsageSnapshot_4cores() {

    // 実機の /proc/stat のサンプル(cpu 行以外は無視されること)
    val procStat = """
      cpu  48200 4601 35693 979258 5095 1 855 0 0 0
      cpu0 26847 1924 25608 212324 2212 1 782 0 0 0
      cpu1 8371 1003 4180 254096 1026 0 50 0 0 0
      cpu2 8450 983 3916 252872 1304 0 9 0 0 0
      cpu3 4532 691 1989 259966 553 0 14 0 0 0
      intr 248023 0 0 0 21744 0 0 0 0 0
      ctxt 331028
      btime 1411020847
    """.trimIndent()

    val result = CpuInfoCollector.parseCpuUsageSnapshot(readerOf(procStat))

    // [0] が全体、[1]以降が個別CPU
    assertThat(result.size).isEqualTo(5)

    // cpu(全体): idle=979258, total=48200+4601+35693+979258+5095+1+855
    assertThat(result[0].idle).isEqualTo(979258L)
    assertThat(result[0].total).isEqualTo(1073703L)

    // cpu0: idle=212324, total=26847+1924+25608+212324+2212+1+782
    assertThat(result[1].idle).isEqualTo(212324L)
    assertThat(result[1].total).isEqualTo(269698L)
  }

  @Test
  fun parseCpuUsageSnapshot_frozenCore() {

    // 冬眠コアが cpu 行から消えるケース(cpu2 が欠落)
    val procStat = """
      cpu  100 200 300 400 500 600 700 0 0 0
      cpu0 10 20 30 40 50 60 70 0 0 0
      cpu1 1 2 3 4 5 6 7 0 0 0
    """.trimIndent()

    val result = CpuInfoCollector.parseCpuUsageSnapshot(readerOf(procStat))

    assertThat(result.size).isEqualTo(3)
    assertThat(result[2].idle).isEqualTo(4L)
    assertThat(result[2].total).isEqualTo((1 + 2 + 3 + 4 + 5 + 6 + 7).toLong())
  }

  @Test
  fun parseCpuUsageSnapshot_empty() {

    val result = CpuInfoCollector.parseCpuUsageSnapshot(readerOf(""))

    assertThat(result).isEmpty()
  }

  @Test
  fun parseCpuUsageSnapshot_noCpuLines() {

    val procStat = """
      intr 248023 0 0 0
      ctxt 331028
    """.trimIndent()

    val result = CpuInfoCollector.parseCpuUsageSnapshot(readerOf(procStat))

    assertThat(result).isEmpty()
  }
}
