package jp.takke.cpustats

import jp.takke.cpustats.util.MyUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MyUtilTest {

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
  fun formatFreq() {

    assertThat(MyUtil.formatFreq(300000)).isEqualTo("300 MHz")
    assertThat(MyUtil.formatFreq(999000)).isEqualTo("999 MHz")
    assertThat(MyUtil.formatFreq(1000000)).isEqualTo("1.0 GHz")
    assertThat(MyUtil.formatFreq(1500000)).isEqualTo("1.5 GHz")
    assertThat(MyUtil.formatFreq(2840000)).isEqualTo("2.8 GHz")
  }
}
