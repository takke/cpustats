package jp.takke.cpustats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import jp.takke.cpustats.ui.component.HistoryGraph
import jp.takke.cpustats.ui.theme.CpuStatsTheme
import jp.takke.cpustats.util.MyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 履歴記録のグラフ表示画面(Compose 版)
 */
class HistoryActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      CpuStatsTheme {
        HistoryScreen(onBack = { finish() })
      }
    }
  }
}

private data class Period(val label: String, val ms: Long)

private val Periods = listOf(
  Period("1h", 60 * 60 * 1000L),
  Period("6h", 6 * 60 * 60 * 1000L),
  Period("24h", 24 * 60 * 60 * 1000L),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(onBack: () -> Unit) {

  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedPeriodIndex by remember { mutableStateOf(2) }  // 24h デフォルト
  var rows by remember { mutableStateOf<List<UsageLogRow>>(emptyList()) }
  var fromTime by remember { mutableStateOf(0L) }
  var toTime by remember { mutableStateOf(0L) }

  // 画面表示中は再開時にリロードする
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  LaunchedEffect(selectedPeriodIndex, lifecycleState) {
    val now = System.currentTimeMillis()
    toTime = now
    fromTime = now - Periods[selectedPeriodIndex].ms

    rows = withContext(Dispatchers.IO) {
      UsageLogDatabase.getInstance(context).queryRange(fromTime)
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.title_activity_history)) },
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
        .padding(horizontal = 8.dp)
    ) {

      // 期間切替
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Periods.forEachIndexed { index, period ->
          SegmentedButton(
            selected = index == selectedPeriodIndex,
            onClick = { selectedPeriodIndex = index },
            shape = SegmentedButtonDefaults.itemShape(index, Periods.size)
          ) {
            Text(period.label)
          }
        }
      }

      if (rows.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(stringResource(R.string.history_no_data))
        }
      } else {
        GraphSection("CPU Usage") {
          HistoryGraph(
            points = rows.map { it.recordedAt to it.cpuUsage },
            fromTime = fromTime,
            toTime = toTime,
            fixedMaxValue = 100,
            valueFormatter = { "$it%" },
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
          )
        }

        GraphSection("Battery Temperature") {
          HistoryGraph(
            points = rows.map { it.recordedAt to it.batteryTemperature },
            fromTime = fromTime,
            toTime = toTime,
            valueFormatter = { MyUtil.formatTemperature(it) },
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
          )
        }

        GraphSection("Memory") {
          HistoryGraph(
            points = rows.map { it.recordedAt to it.memUsedMb },
            fromTime = fromTime,
            toTime = toTime,
            valueFormatter = { MyUtil.formatMemSize(it) },
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun GraphSection(title: String, content: @Composable () -> Unit) {

  Column(
    modifier = Modifier.padding(top = 12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    content()
  }
}
