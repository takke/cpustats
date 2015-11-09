package jp.takke.cpustats;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class ConfigActivity extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(this);

        {
            final PreferenceCategory pc = new PreferenceCategory(this);
            pc.setTitle(R.string.config_usage_category_title);
            ps.addPreference(pc);
            
            {
                final CheckBoxPreference pref = new CheckBoxPreference(this);
                pref.setKey(C.PREF_KEY_SHOW_USAGE_NOTIFICATION);
                pref.setTitle(R.string.config_show_usage_notification);
                pref.setDefaultValue(true);
                pc.addPreference(pref);
            }

            {
                final ListPreference pref = new ListPreference(this);
                pref.setKey(C.PREF_KEY_CORE_DISTRIBUTION_MODE);
                pref.setTitle(R.string.config_core_distribution_mode);
                pref.setSummary(R.string.config_core_distribution_mode_summary);
                final String[] entryValues = {
                        C.CORE_DISTRIBUTION_MODE_2ICONS + "",
                        C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED + "",
                        C.CORE_DISTRIBUTION_MODE_1ICON_SORTED + "",
                };
                pref.setEntries(R.array.core_distribution_mode_entries);
                pref.setEntryValues(entryValues);
                pref.setDefaultValue(C.CORE_DISTRIBUTION_MODE_2ICONS + "");
                pc.addPreference(pref);
            }

        }
        
        {
            final PreferenceCategory pc = new PreferenceCategory(this);
            pc.setTitle(R.string.config_frequency_category_title);
            ps.addPreference(pc);
            
            {
                final CheckBoxPreference pref = new CheckBoxPreference(this);
                pref.setKey(C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION);
                pref.setTitle(R.string.config_show_frequency_notification);
                pref.setDefaultValue(false);
                pc.addPreference(pref);
            }
        }
        
        {
            final PreferenceCategory pc = new PreferenceCategory(this);
            pc.setTitle(R.string.config_general_category_title);
            ps.addPreference(pc);
            
            {
                final ListPreference pref = new ListPreference(this);
                pref.setKey(C.PREF_KEY_UPDATE_INTERVAL_SEC);
                pref.setTitle(R.string.config_update_interval_title);
                final String[] entries = {"0.5sec", "1sec", "2sec", "3sec", "5sec", "10sec"};
                final String[] entryValues = {"0.5", "1", "2", "3", "5", "10"};
                pref.setEntries(entries);
                pref.setEntryValues(entryValues);
                pref.setDefaultValue("" + C.PREF_DEFAULT_UPDATE_INTERVAL_SEC);
                pc.addPreference(pref);
            }
            
            {
                final CheckBoxPreference pref = new CheckBoxPreference(this);
                pref.setKey(C.PREF_KEY_START_ON_BOOT);
                pref.setTitle(R.string.config_start_on_boot_title);
                pref.setSummary(R.string.config_start_on_boot_summary);
                pref.setDefaultValue(false);
                pc.addPreference(pref);
            }
        }
        
        setPreferenceScreen(ps);
    }
}
