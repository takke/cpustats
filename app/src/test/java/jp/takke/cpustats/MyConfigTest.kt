package jp.takke.cpustats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24])
class MyConfigTest {

  private val context get() = RuntimeEnvironment.getApplication()

  private fun putPrefs(block: android.content.SharedPreferences.Editor.() -> Unit) {
    context.defaultSharedPreferences().edit().apply(block).commit()
  }

  @Test
  fun load_defaults() {

    val config = MyConfig.load(context)

    assertThat(config.intervalMs).isEqualTo(5000L)
    assertThat(config.showUsageNotification).isTrue()
    assertThat(config.showFrequencyNotification).isFalse()
    // 設定未保存時のデフォルトは「1 Icon (sorted)」
    assertThat(config.coreDistributionMode).isEqualTo(C.CORE_DISTRIBUTION_MODE_1ICON_SORTED)
  }

  @Test
  fun load_customValues() {

    putPrefs {
      putString(C.PREF_KEY_UPDATE_INTERVAL_SEC, "0.5")
      putBoolean(C.PREF_KEY_SHOW_USAGE_NOTIFICATION, false)
      putBoolean(C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION, true)
      putString(C.PREF_KEY_CORE_DISTRIBUTION_MODE, "" + C.CORE_DISTRIBUTION_MODE_2ICONS)
    }

    val config = MyConfig.load(context)

    // 小数秒 → ms 変換
    assertThat(config.intervalMs).isEqualTo(500L)
    assertThat(config.showUsageNotification).isFalse()
    assertThat(config.showFrequencyNotification).isTrue()
    assertThat(config.coreDistributionMode).isEqualTo(C.CORE_DISTRIBUTION_MODE_2ICONS)
  }

  @Test
  fun load_invalidValues() {

    putPrefs {
      putString(C.PREF_KEY_UPDATE_INTERVAL_SEC, "abc")
      putString(C.PREF_KEY_CORE_DISTRIBUTION_MODE, "xyz")
    }

    val config = MyConfig.load(context)

    // 不正値はデフォルトにフォールバックする
    assertThat(config.intervalMs).isEqualTo(5000L)
    assertThat(config.coreDistributionMode).isEqualTo(C.CORE_DISTRIBUTION_MODE_2ICONS)
  }
}
