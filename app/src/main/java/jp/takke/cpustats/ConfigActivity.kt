package jp.takke.cpustats

import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory

class ConfigActivity : PreferenceActivity() {

    @Suppress("DEPRECATION")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ps = preferenceManager.createPreferenceScreen(this)

        PreferenceCategory(this).also { pc ->

            pc.setTitle(R.string.config_usage_category_title)
            ps.addPreference(pc)

            CheckBoxPreference(this).also { pref ->
                pref.key = C.PREF_KEY_SHOW_USAGE_NOTIFICATION
                pref.setTitle(R.string.config_show_usage_notification)
                pref.setDefaultValue(true)
                pc.addPreference(pref)
            }

            ListPreference(this).also { pref ->
                pref.key = C.PREF_KEY_CORE_DISTRIBUTION_MODE
                pref.setTitle(R.string.config_core_distribution_mode)
                pref.setSummary(R.string.config_core_distribution_mode_summary)
                val entryValues = arrayOf(
                        C.CORE_DISTRIBUTION_MODE_2ICONS.toString(),
                        C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED.toString(),
                        C.CORE_DISTRIBUTION_MODE_1ICON_SORTED.toString())
                pref.setEntries(R.array.core_distribution_mode_entries)
                pref.entryValues = entryValues
                pref.setDefaultValue(C.CORE_DISTRIBUTION_MODE_2ICONS.toString())
                pc.addPreference(pref)
            }

        }

        PreferenceCategory(this).also { pc ->

            pc.setTitle(R.string.config_frequency_category_title)
            ps.addPreference(pc)

            CheckBoxPreference(this).also { pref ->
                pref.key = C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION
                pref.setTitle(R.string.config_show_frequency_notification)
                pref.setDefaultValue(false)
                pc.addPreference(pref)
            }
        }

        PreferenceCategory(this).also { pc ->

            pc.setTitle(R.string.config_general_category_title)
            ps.addPreference(pc)

            ListPreference(this).also { pref ->
                pref.key = C.PREF_KEY_UPDATE_INTERVAL_SEC
                pref.setTitle(R.string.config_update_interval_title)
                pref.summary = "%s"
                val entries = arrayOf("0.5sec", "1sec", "2sec", "3sec", "5sec", "10sec")
                val entryValues = arrayOf("0.5", "1", "2", "3", "5", "10")
                pref.entries = entries
                pref.entryValues = entryValues
                pref.setDefaultValue("" + C.PREF_DEFAULT_UPDATE_INTERVAL_SEC)
                pc.addPreference(pref)
            }

            CheckBoxPreference(this).also { pref ->
                pref.key = C.PREF_KEY_START_ON_BOOT
                pref.setTitle(R.string.config_start_on_boot_title)
                pref.setSummary(R.string.config_start_on_boot_summary)
                pref.setDefaultValue(false)
                pc.addPreference(pref)
            }
        }

        preferenceScreen = ps
    }
}
