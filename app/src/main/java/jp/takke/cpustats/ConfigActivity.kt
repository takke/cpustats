package jp.takke.cpustats

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import jp.takke.cpustats.ui.theme.CpuStatsTheme

class ConfigActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      CpuStatsTheme {
        ConfigScreen(onBack = { finish() })
      }
    }
  }

  override fun onPause() {

    // 通知アクション等、MainActivity を経由せず開かれた場合でも設定変更を
    // サービスに反映させる(常駐中のみ。常駐状態には影響しない)
    if (UsageUpdateService.isResidentRunning) {
      val intent = Intent(this, UsageUpdateService::class.java)
      intent.action = UsageUpdateService.ACTION_RELOAD_SETTINGS
      startService(intent)
    }

    super.onPause()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigScreen(onBack: () -> Unit) {

  val context = LocalContext.current
  val prefs = remember { context.defaultSharedPreferences() }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.title_activity_config)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        )
      )
    }
  ) { innerPadding ->

    Column(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
    ) {
      // CPU 使用率カテゴリ
      CategoryTitle(stringResource(R.string.config_usage_category_title))

      SwitchPreference(
        prefs = prefs,
        key = C.PREF_KEY_SHOW_USAGE_NOTIFICATION,
        title = stringResource(R.string.config_show_usage_notification),
        default = true,
      )

      ListPreference(
        prefs = prefs,
        key = C.PREF_KEY_CORE_DISTRIBUTION_MODE,
        title = stringResource(R.string.config_core_distribution_mode),
        summary = stringResource(R.string.config_core_distribution_mode_summary),
        entries = stringArrayResource(R.array.core_distribution_mode_entries),
        entryValues = arrayOf(
          C.CORE_DISTRIBUTION_MODE_2ICONS.toString(),
          C.CORE_DISTRIBUTION_MODE_1ICON_UNSORTED.toString(),
          C.CORE_DISTRIBUTION_MODE_1ICON_SORTED.toString(),
        ),
        default = C.CORE_DISTRIBUTION_MODE_1ICON_SORTED.toString(),
      )

      HorizontalDivider()

      // CPU 周波数カテゴリ
      CategoryTitle(stringResource(R.string.config_frequency_category_title))

      SwitchPreference(
        prefs = prefs,
        key = C.PREF_KEY_SHOW_FREQUENCY_NOTIFICATION,
        title = stringResource(R.string.config_show_frequency_notification),
        default = false,
      )

      HorizontalDivider()

      // General カテゴリ
      CategoryTitle(stringResource(R.string.config_general_category_title))

      ListPreference(
        prefs = prefs,
        key = C.PREF_KEY_UPDATE_INTERVAL_SEC,
        title = stringResource(R.string.config_update_interval_title),
        summary = null,
        entries = arrayOf("0.5sec", "1sec", "2sec", "3sec", "5sec", "10sec"),
        entryValues = arrayOf("0.5", "1", "2", "3", "5", "10"),
        default = "" + C.PREF_DEFAULT_UPDATE_INTERVAL_SEC,
      )

      SwitchPreference(
        prefs = prefs,
        key = C.PREF_KEY_START_ON_BOOT,
        title = stringResource(R.string.config_start_on_boot_title),
        summary = stringResource(R.string.config_start_on_boot_summary),
        default = true,
      )

      SwitchPreference(
        prefs = prefs,
        key = C.PREF_KEY_RECORD_HISTORY,
        title = stringResource(R.string.config_record_history_title),
        summary = stringResource(R.string.config_record_history_summary),
        default = true,
      )
    }
  }
}

@Composable
private fun CategoryTitle(title: String) {

  Text(
    text = title,
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
    style = MaterialTheme.typography.labelLarge,
  )
}

@Composable
private fun SwitchPreference(
  prefs: SharedPreferences,
  key: String,
  title: String,
  summary: String? = null,
  default: Boolean,
) {
  var checked by remember(key) { mutableStateOf(prefs.getBoolean(key, default)) }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        checked = !checked
        prefs.edit().putBoolean(key, checked).apply()
      }
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (summary != null) {
        Text(
          summary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = {
        checked = it
        prefs.edit().putBoolean(key, it).apply()
      }
    )
  }
}

@Composable
private fun ListPreference(
  prefs: SharedPreferences,
  key: String,
  title: String,
  summary: String?,
  entries: Array<String>,
  entryValues: Array<String>,
  default: String,
) {
  var selectedValue by remember(key) { mutableStateOf(prefs.getString(key, default) ?: default) }
  var showDialog by rememberSaveable(key) { mutableStateOf(false) }

  val selectedEntry = entries.getOrNull(entryValues.indexOf(selectedValue)) ?: default

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { showDialog = true }
      .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    Text(title, style = MaterialTheme.typography.bodyLarge)
    Text(
      text = summary?.replace("%s", selectedEntry) ?: selectedEntry,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
  }

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { showDialog = false },
      title = { Text(title) },
      text = {
        Column {
          entries.forEachIndexed { index, entry ->
            val value = entryValues[index]
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .selectable(
                  selected = selectedValue == value,
                  onClick = {
                    selectedValue = value
                    prefs.edit().putString(key, value).apply()
                    showDialog = false
                  }
                )
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedValue == value,
                onClick = {
                  selectedValue = value
                  prefs.edit().putString(key, value).apply()
                  showDialog = false
                }
              )
              Text(entry, modifier = Modifier.padding(start = 8.dp))
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDialog = false }) { Text("Cancel") }
      }
    )
  }
}
