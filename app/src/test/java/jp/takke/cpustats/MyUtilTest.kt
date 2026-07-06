package jp.takke.cpustats

import jp.takke.cpustats.util.MyUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// MyLog 経由で android.util.Log を呼ぶ経路があるため Robolectric で実行する
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24])
class MyUtilTest {

  private fun oneCpu(idle: Long, total: Long) = OneCpuInfo().also {
    it.idle = idle
    it.total = total
  }

  @Test
  fun calcCpuUsages_normal() {

    // totalDiff=1000, idleDiff=400 → 100 - 40 = 60%
    val last = arrayListOf(oneCpu(1000, 2000), oneCpu(500, 1000))
    val curr = arrayListOf(oneCpu(1400, 3000), oneCpu(1500, 2000))

    val usages = MyUtil.calcCpuUsages(curr, last)

    assertThat(usages).isNotNull()
    assertThat(usages!!.size).isEqualTo(2)
    assertThat(usages[0]).isEqualTo(60)
    // totalDiff=1000, idleDiff=1000 → 0%
    assertThat(usages[1]).isEqualTo(0)
  }

  @Test
  fun calcCpuUsages_totalDiffZero() {

    // 前回と同一(totalDiff=0)なら 0% とする
    val last = arrayListOf(oneCpu(1000, 2000))
    val curr = arrayListOf(oneCpu(1000, 2000))

    val usages = MyUtil.calcCpuUsages(curr, last)

    assertThat(usages).isNotNull()
    assertThat(usages!![0]).isEqualTo(0)
  }

  @Test
  fun calcCpuUsages_fullUsage() {

    // idleDiff=0 → 100%
    val last = arrayListOf(oneCpu(1000, 2000))
    val curr = arrayListOf(oneCpu(1000, 3000))

    val usages = MyUtil.calcCpuUsages(curr, last)

    assertThat(usages).isNotNull()
    assertThat(usages!![0]).isEqualTo(100)
  }

  @Test
  fun calcCpuUsages_coreCountFluctuation() {

    // コア数変動時(Galaxy S II 等)は少ない方のコア数で算出する
    val last = arrayListOf(oneCpu(1000, 2000), oneCpu(500, 1000))
    val curr = arrayListOf(
      oneCpu(1400, 3000), oneCpu(1000, 2000), oneCpu(100, 200), oneCpu(100, 200)
    )

    val usages = MyUtil.calcCpuUsages(curr, last)

    assertThat(usages).isNotNull()
    assertThat(usages!!.size).isEqualTo(2)

    // 逆方向(減少)
    val usages2 = MyUtil.calcCpuUsages(last, curr)
    assertThat(usages2).isNotNull()
    assertThat(usages2!!.size).isEqualTo(2)
  }

  @Test
  fun calcCpuUsages_nullOrEmpty() {

    val valid = arrayListOf(oneCpu(1000, 2000))

    assertThat(MyUtil.calcCpuUsages(null, valid)).isNull()
    assertThat(MyUtil.calcCpuUsages(valid, null)).isNull()
    assertThat(MyUtil.calcCpuUsages(arrayListOf(), valid)).isNull()
    assertThat(MyUtil.calcCpuUsages(valid, arrayListOf())).isNull()
  }

  @Test
  fun getClockPercent() {

    // (574000 - 300000) * 100 / (1800000 - 300000) = 18%
    assertThat(MyUtil.getClockPercent(574000, 300000, 1800000)).isEqualTo(18)

    // 最小値・最大値
    assertThat(MyUtil.getClockPercent(300000, 300000, 1800000)).isEqualTo(0)
    assertThat(MyUtil.getClockPercent(1800000, 300000, 1800000)).isEqualTo(100)

    // min == max(冬眠コアなどで 0,0 になるケース)
    assertThat(MyUtil.getClockPercent(0, 0, 0)).isEqualTo(0)
    assertThat(MyUtil.getClockPercent(500, 500, 500)).isEqualTo(0)

    // min > max(不正値)
    assertThat(MyUtil.getClockPercent(500, 1000, 100)).isEqualTo(0)
  }

  @Test
  fun getActiveCoreIndex() {

    // 最大周波数のコアを返す
    assertThat(MyUtil.getActiveCoreIndex(intArrayOf(100, 300, 200))).isEqualTo(1)
    assertThat(MyUtil.getActiveCoreIndex(intArrayOf(100, 200, 300))).isEqualTo(2)

    // 同値なら先頭を返す
    assertThat(MyUtil.getActiveCoreIndex(intArrayOf(300, 300, 300))).isEqualTo(0)

    // 1コア
    assertThat(MyUtil.getActiveCoreIndex(intArrayOf(100))).isEqualTo(0)
  }

  @Test
  fun formatTemperature() {

    // 通常ケース(0.1℃単位)
    assertThat(MyUtil.formatTemperature(325)).isEqualTo("32.5°C")
    assertThat(MyUtil.formatTemperature(405)).isEqualTo("40.5°C")
    assertThat(MyUtil.formatTemperature(1000)).isEqualTo("100.0°C")

    // 境界値
    assertThat(MyUtil.formatTemperature(0)).isEqualTo("0.0°C")
    assertThat(MyUtil.formatTemperature(9)).isEqualTo("0.9°C")
    assertThat(MyUtil.formatTemperature(10)).isEqualTo("1.0°C")

    // 負値(氷点下)
    assertThat(MyUtil.formatTemperature(-53)).isEqualTo("-5.3°C")
    assertThat(MyUtil.formatTemperature(-3)).isEqualTo("-0.3°C")
  }

  @Test
  fun formatMemSize() {

    // MB 表示
    assertThat(MyUtil.formatMemSize(0)).isEqualTo("0 MB")
    assertThat(MyUtil.formatMemSize(800)).isEqualTo("800 MB")
    assertThat(MyUtil.formatMemSize(1023)).isEqualTo("1023 MB")

    // GB 表示(小数第1位まで)
    assertThat(MyUtil.formatMemSize(1024)).isEqualTo("1.0 GB")
    assertThat(MyUtil.formatMemSize(3277)).isEqualTo("3.2 GB")
    assertThat(MyUtil.formatMemSize(7987)).isEqualTo("7.7 GB")
    assertThat(MyUtil.formatMemSize(16384)).isEqualTo("16.0 GB")
  }

  @Test
  fun formatCpuClusters() {

    // big.LITTLE(4+4)
    assertThat(
      MyUtil.formatCpuClusters(
        intArrayOf(1804800, 1804800, 1804800, 1804800, 2841600, 2841600, 2841600, 2841600)
      )
    ).isEqualTo("4×1.8 GHz + 4×2.8 GHz")

    // 3 クラスタ(1+3+4)は周波数昇順
    assertThat(
      MyUtil.formatCpuClusters(
        intArrayOf(2841600, 1804800, 1804800, 1804800, 1704000, 1704000, 1704000, 1704000)
      )
    ).isEqualTo("4×1.7 GHz + 3×1.8 GHz + 1×2.8 GHz")

    // 単一クラスタ
    assertThat(MyUtil.formatCpuClusters(intArrayOf(2000000, 2000000)))
      .isEqualTo("2×2.0 GHz")

    // 0(取得不可)は無視する
    assertThat(MyUtil.formatCpuClusters(intArrayOf(0, 2000000, 0, 2000000)))
      .isEqualTo("2×2.0 GHz")

    // 全て取得不可・空
    assertThat(MyUtil.formatCpuClusters(intArrayOf(0, 0))).isEmpty()
    assertThat(MyUtil.formatCpuClusters(intArrayOf())).isEmpty()
  }

  @Test
  fun formatFreq() {

    assertThat(MyUtil.formatFreq(300000)).isEqualTo("300 MHz")
    assertThat(MyUtil.formatFreq(999000)).isEqualTo("999 MHz")
    assertThat(MyUtil.formatFreq(1000000)).isEqualTo("1.0 GHz")
    assertThat(MyUtil.formatFreq(1500000)).isEqualTo("1.5 GHz")
    assertThat(MyUtil.formatFreq(2840000)).isEqualTo("2.8 GHz")
  }
}
