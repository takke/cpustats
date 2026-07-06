package jp.takke.cpustats

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import jp.takke.cpustats.ui.component.Sparkline
import jp.takke.cpustats.ui.theme.CpuStatsTheme
import jp.takke.cpustats.ui.util.rememberDrawablePainter
import jp.takke.cpustats.util.MyUtil
import jp.takke.cpustats.util.ResourceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

  private var mService: UsageUpdateService? = null
  private var mBound = false

  private val _state = MutableStateFlow(MainState())
  val state: StateFlow<MainState> = _state

  // コールバックの実装(サービスの収集スレッドから呼ばれる)
  private val mCallback = UsageUpdateCallback { cpuUsages, freqs, minFreqs, maxFreqs, systemStats ->
    _state.value = _state.value.copy(
      cpuUsages = cpuUsages,
      freqs = freqs,
      minFreqs = minFreqs,
      maxFreqs = maxFreqs,
      systemStats = systemStats,
      history = mService?.getUsageHistory() ?: IntArray(0),
      serviceRunning = true,
    )
  }

  private val mServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(name: ComponentName, service: IBinder) {

      MyLog.i("MainActivity.onServiceConnected")

      mService = (service as UsageUpdateService.LocalBinder).service
      mService?.registerCallback(mCallback)
      mService?.reloadSettings()

      // 履歴の即時復元
      _state.value = _state.value.copy(
        history = mService?.getUsageHistory() ?: IntArray(0)
      )
    }

    override fun onServiceDisconnected(name: ComponentName) {

      MyLog.i("MainActivity.onServiceDisconnected")
      mService = null
    }
  }

  private val mConfigActivityLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      // 設定変更の内容をサービスに反映させる
      if (mService != null) {
        MyLog.d("request reloadSettings")
        mService?.reloadSettings()
      } else {
        MyLog.w("cannot reloadSettings (no service i.f.)")
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      // 通知権限のチェックは Compose 側で launcher を持ち、初回のみ実行する
    }

    doBindService()

    setContent {
      CpuStatsTheme {
        MainScreen(
          stateFlow = state,
          onStartService = { mService?.startResident() },
          onStopService = {
            mService?.stopResident()
            _state.value = _state.value.copy(serviceRunning = false)
          },
          onOpenSettings = {
            mConfigActivityLauncher.launch(Intent(this, ConfigActivity::class.java))
          },
          onOpenHistory = { startActivity(Intent(this, HistoryActivity::class.java)) },
          onOpenAbout = { startActivity(Intent(this, AboutActivity::class.java)) },
          onExit = { finish() },
        )
      }
    }
  }

  private fun doBindService() {

    val serviceIntent = Intent(this, UsageUpdateService::class.java)

    MyLog.d("MainActivity: startService of UsageUpdateService")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      serviceIntent.putExtra("FOREGROUND_REQUEST", true)
      startForegroundService(serviceIntent)
    } else {
      startService(serviceIntent)
    }

    mBound = bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
  }

  override fun onDestroy() {

    MyLog.d("MainActivity.onDestroy")

    mService?.unregisterCallback(mCallback)
    if (mBound) {
      unbindService(mServiceConnection)
      mBound = false
    }

    super.onDestroy()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
  stateFlow: StateFlow<MainState>,
  onStartService: () -> Unit,
  onStopService: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenAbout: () -> Unit,
  onExit: () -> Unit,
) {
  val state by stateFlow.collectAsState()
  val context = LocalContext.current

  // 通知権限リクエスト(Android 13+)
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (!isGranted) {
      Toast.makeText(
        context, R.string.require_notification_permission, Toast.LENGTH_LONG
      ).show()
    }
  }
  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  androidx.compose.runtime.LaunchedEffect(lifecycleState) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.title_activity_main)) },
        navigationIcon = {
          val avg = state.cpuUsages.firstOrNull() ?: 0
          Image(
            painter = rememberDrawablePainter(ResourceUtil.getIconIdForCpuUsageSingleColor(avg)),
            contentDescription = null,
            modifier = Modifier
              .padding(start = 8.dp)
              .size(32.dp)
          )
        },
        actions = {
          OverflowMenu(
            state,
            onStartService,
            onStopService,
            onOpenSettings,
            onOpenHistory,
            onOpenAbout,
            onExit
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
        )
      )
    }
  ) { innerPadding ->

    LazyColumn(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
      item { FrequencyRow(state) }
      item {
        Sparkline(
          history = state.history,
          modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }

      val coreCount = CpuInfoCollector.calcCpuCoreCount()
      val cpuUsages = state.cpuUsages
      items(coreCount) { i ->
        CoreRow(
          coreIndex = i,
          cpuUsage = if (cpuUsages.size > i + 1) cpuUsages[i + 1] else 0,
          freq = state.freqs.getOrElse(i) { 0 },
          minFreq = state.minFreqs.getOrElse(i) { 0 },
          maxFreq = state.maxFreqs.getOrElse(i) { 0 },
        )
      }
    }
  }
}

@Composable
private fun FrequencyRow(state: MainState) {

  val freqs = state.freqs
  val minFreqs = state.minFreqs
  val maxFreqs = state.maxFreqs
  val activeCoreIndex = if (freqs.isEmpty()) 0 else MyUtil.getActiveCoreIndex(freqs)
  val currentFreq = freqs.getOrElse(activeCoreIndex) { 0 }
  val minFreq = minFreqs.getOrElse(activeCoreIndex) { 0 }
  val maxFreq = maxFreqs.getOrElse(activeCoreIndex) { 0 }
  val clockPercent = MyUtil.getClockPercent(currentFreq, minFreq, maxFreq)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      painter = rememberDrawablePainter(ResourceUtil.getIconIdForCpuFreq(currentFreq)),
      contentDescription = null,
      modifier = Modifier.size(72.dp)
    )
    Spacer(Modifier.width(8.dp))

    Column {
      val bodyText = buildAnnotatedString {
        append(MyUtil.formatFreq(currentFreq))
        withStyleSmall {
          append("  [Core ${activeCoreIndex + 1}: $clockPercent%]")
        }
      }
      Text(bodyText, style = MaterialTheme.typography.bodyLarge)

      Text(
        text = " (${MyUtil.formatFreq(minFreq)} - ${MyUtil.formatFreq(maxFreq)})",
        style = MaterialTheme.typography.bodySmall,
      )

      // クラスタ構成(big.LITTLE)
      val clusters = MyUtil.formatCpuClusters(maxFreqs)
      if (clusters.isNotEmpty()) {
        Text(" Cores: $clusters", style = MaterialTheme.typography.bodySmall)
      }

      if (state.systemStats.batteryTemperature != 0) {
        Text(
          text = " Battery: ${MyUtil.formatTemperature(state.systemStats.batteryTemperature)}",
          style = MaterialTheme.typography.bodySmall,
        )
      }
      if (state.systemStats.memTotalMb > 0) {
        val pct =
          state.systemStats.memUsedMb * 100 / state.systemStats.memTotalMb
        val used = MyUtil.formatMemSize(state.systemStats.memUsedMb)
        val total = MyUtil.formatMemSize(state.systemStats.memTotalMb)
        Text(" Memory: $used / $total ($pct%)", style = MaterialTheme.typography.bodySmall)
      }
    }
  }
}

@Composable
private fun CoreRow(coreIndex: Int, cpuUsage: Int, freq: Int, minFreq: Int, maxFreq: Int) {

  val context = LocalContext.current
  val clockPercent = MyUtil.getClockPercent(freq, minFreq, maxFreq)
  val bg = Color(ResourceUtil.getBackgroundColor(context, clockPercent))

  Surface(
    color = bg, modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Image(
        painter = rememberDrawablePainter(ResourceUtil.getIconIdForCpuUsageSingleColor(cpuUsage)),
        contentDescription = null,
        modifier = Modifier.size(72.dp)
      )
      Spacer(Modifier.width(8.dp))

      Column {
        Text(
          text = "Core${coreIndex + 1}: $cpuUsage%",
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          buildAnnotatedString {
            append(MyUtil.formatFreq(freq))
            withStyleSmall { append(" [$clockPercent%]") }
          },
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = " (${MyUtil.formatFreq(minFreq)} - ${MyUtil.formatFreq(maxFreq)})",
          style = MaterialTheme.typography.bodySmall,
        )
      }
    }
  }
}

@Composable
private fun OverflowMenu(
  state: MainState,
  onStartService: () -> Unit,
  onStopService: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenHistory: () -> Unit,
  onOpenAbout: () -> Unit,
  onExit: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  IconButton(onClick = { expanded = true }) {
    Icon(Icons.Filled.MoreVert, contentDescription = "More")
  }
  DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
    if (!state.serviceRunning) {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.menu_start_service)) },
        onClick = { expanded = false; onStartService() },
      )
    } else {
      DropdownMenuItem(
        text = { Text(stringResource(R.string.menu_stop_service)) },
        onClick = { expanded = false; onStopService() },
      )
    }
    DropdownMenuItem(
      text = { Text(stringResource(R.string.menu_settings)) },
      onClick = { expanded = false; onOpenSettings() },
    )
    DropdownMenuItem(
      text = { Text(stringResource(R.string.title_activity_history)) },
      onClick = { expanded = false; onOpenHistory() },
    )
    DropdownMenuItem(
      text = { Text(stringResource(R.string.title_activity_about)) },
      onClick = { expanded = false; onOpenAbout() },
    )
    DropdownMenuItem(
      text = { Text(stringResource(R.string.menu_exit)) },
      onClick = { expanded = false; onExit() },
    )
  }
}

private inline fun androidx.compose.ui.text.AnnotatedString.Builder.withStyleSmall(
  crossinline block: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit
) {
  pushStyle(SpanStyle(fontSize = 12.sp))
  block()
  pop()
}
