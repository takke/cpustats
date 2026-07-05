package jp.takke.cpustats

import androidx.core.content.ContextCompat
import jp.takke.cpustats.util.ResourceUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import jp.takke.cpustats.quad5.R as QuadR

// getBackgroundColor がアプリリソースを解決するため manifest = NONE にしない
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class ResourceUtilTest {

  //--------------------------------------------------
  // getIconIdForCpuUsage
  //--------------------------------------------------

  @Test
  fun getIconIdForCpuUsage_nullOrEmpty() {

    assertThat(ResourceUtil.getIconIdForCpuUsage(null)).isEqualTo(R.drawable.single000)
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf())).isEqualTo(R.drawable.single000)
  }

  @Test
  fun getIconIdForCpuUsage_singleCore_levelBoundaries() {

    // 1コア(cpuUsages = [平均, コア1])は平均値でレベル判定する
    // level5 境界: <5, <20, <40, <60, <80, else
    fun icon(avg: Int) = ResourceUtil.getIconIdForCpuUsage(intArrayOf(avg, avg))

    assertThat(icon(0)).isEqualTo(R.drawable.single000)
    assertThat(icon(4)).isEqualTo(R.drawable.single000)
    assertThat(icon(5)).isEqualTo(R.drawable.single020)
    assertThat(icon(19)).isEqualTo(R.drawable.single020)
    assertThat(icon(20)).isEqualTo(R.drawable.single040)
    assertThat(icon(39)).isEqualTo(R.drawable.single040)
    assertThat(icon(40)).isEqualTo(R.drawable.single060)
    assertThat(icon(59)).isEqualTo(R.drawable.single060)
    assertThat(icon(60)).isEqualTo(R.drawable.single080)
    assertThat(icon(79)).isEqualTo(R.drawable.single080)
    assertThat(icon(80)).isEqualTo(R.drawable.single100)
    assertThat(icon(100)).isEqualTo(R.drawable.single100)
  }

  @Test
  fun getIconIdForCpuUsage_dualCore() {

    // 2コアは [平均, コア1, コア2] のコア値でレベル判定する
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(0, 0, 0)))
      .isEqualTo(R.drawable.dual_0_0)
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(20, 10, 25)))
      .isEqualTo(R.drawable.dual_1_2)
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(100, 100, 100)))
      .isEqualTo(R.drawable.dual_5_5)
  }

  @Test
  fun getIconIdForCpuUsage_triCore() {

    // 3コア(6コアの2分割など)は quad5 モジュールの tri_XXX を引く
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(0, 0, 0, 0)))
      .isEqualTo(QuadR.drawable.tri_000)
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(50, 90, 0, 30)))
      .isEqualTo(QuadR.drawable.tri_502)
  }

  @Test
  fun getIconIdForCpuUsage_quadCore() {

    // 4コア以上は quad5 モジュールの quad_XXXX を引く
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(0, 0, 0, 0, 0)))
      .isEqualTo(QuadR.drawable.quad_0000)
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(100, 100, 100, 100, 100)))
      .isEqualTo(QuadR.drawable.quad_5555)
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(30, 10, 25, 45, 65)))
      .isEqualTo(QuadR.drawable.quad_1234)

    // 5コア以上でも先頭4コアで判定する
    assertThat(ResourceUtil.getIconIdForCpuUsage(intArrayOf(30, 10, 25, 45, 65, 100, 100)))
      .isEqualTo(QuadR.drawable.quad_1234)
  }

  //--------------------------------------------------
  // getIconIdForCpuFreq
  //--------------------------------------------------

  @Test
  fun getIconIdForCpuFreq() {

    // KHz 単位。300MHz => 3, 1.5GHz => 15 に変換される
    assertThat(ResourceUtil.getIconIdForCpuFreq(0)).isEqualTo(R.drawable.freq_01)
    assertThat(ResourceUtil.getIconIdForCpuFreq(100000)).isEqualTo(R.drawable.freq_01)
    assertThat(ResourceUtil.getIconIdForCpuFreq(300000)).isEqualTo(R.drawable.freq_03)
    assertThat(ResourceUtil.getIconIdForCpuFreq(1500000)).isEqualTo(R.drawable.freq_15)
    assertThat(ResourceUtil.getIconIdForCpuFreq(2840000)).isEqualTo(R.drawable.freq_28)
    assertThat(ResourceUtil.getIconIdForCpuFreq(5000000)).isEqualTo(R.drawable.freq_50)

    // 5.0GHz 超は freq_50 に丸める
    assertThat(ResourceUtil.getIconIdForCpuFreq(6000000)).isEqualTo(R.drawable.freq_50)
  }

  //--------------------------------------------------
  // getNotificationIconColorFromUsage
  //--------------------------------------------------

  @Test
  fun getNotificationIconColorFromUsage() {

    assertThat(ResourceUtil.getNotificationIconColorFromUsage(0)).isEqualTo(0xff000000.toInt())
    assertThat(ResourceUtil.getNotificationIconColorFromUsage(5)).isEqualTo(0xff00c21c.toInt())
    assertThat(ResourceUtil.getNotificationIconColorFromUsage(20)).isEqualTo(0xff85c200.toInt())
    assertThat(ResourceUtil.getNotificationIconColorFromUsage(40)).isEqualTo(0xffc4bd00.toInt())
    assertThat(ResourceUtil.getNotificationIconColorFromUsage(60)).isEqualTo(0xffcf7400.toInt())
    assertThat(ResourceUtil.getNotificationIconColorFromUsage(80)).isEqualTo(0xffcf0000.toInt())
    assertThat(ResourceUtil.getNotificationIconColorFromUsage(100)).isEqualTo(0xffcf0000.toInt())
  }

  //--------------------------------------------------
  // getBackgroundColor
  //--------------------------------------------------

  @Test
  fun getBackgroundColor() {

    val context = RuntimeEnvironment.getApplication()

    // 負荷レベルに応じた色リソースを解決すること
    assertThat(ResourceUtil.getBackgroundColor(context, 0))
      .isEqualTo(ContextCompat.getColor(context, R.color.core_bg_idle))
    assertThat(ResourceUtil.getBackgroundColor(context, 30))
      .isEqualTo(ContextCompat.getColor(context, R.color.core_bg_active))
    assertThat(ResourceUtil.getBackgroundColor(context, 60))
      .isEqualTo(ContextCompat.getColor(context, R.color.core_bg_high))
    assertThat(ResourceUtil.getBackgroundColor(context, 100))
      .isEqualTo(ContextCompat.getColor(context, R.color.core_bg_high))
  }
}
