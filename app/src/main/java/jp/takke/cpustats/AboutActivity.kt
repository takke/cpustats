package jp.takke.cpustats

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import jp.takke.cpustats.ui.theme.CpuStatsTheme
import jp.takke.cpustats.ui.util.rememberDrawablePainter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

class AboutActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      CpuStatsTheme {
        AboutScreen(versionString = resolveVersionString())
      }
    }
  }

  private fun resolveVersionString(): String {

    var versionCode = "?"
    var version = "x.x.x"
    try {
      val pinfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
      versionCode = PackageInfoCompat.getLongVersionCode(pinfo).toString()
      version = pinfo.versionName ?: ""
    } catch (e: PackageManager.NameNotFoundException) {
      MyLog.e(e)
    }

    return getString(R.string.app_version)
      .replace("{VERSION}", version)
      .replace("{REVISION}", versionCode)
  }
}

private const val LINK_SOURCE = "https://github.com/takke/cpustats"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AboutScreen(versionString: String) {

  val context = LocalContext.current

  var showDump by remember { mutableStateOf(false) }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) { innerPadding ->

    Column(
      modifier = Modifier
        .padding(innerPadding)
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {

      // アプリ名 + アイコン(長押しで内部情報ダンプ)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          painter = rememberDrawablePainter(R.drawable.ic_launcher),
          contentDescription = null,
          modifier = Modifier
            .size(48.dp)
            .combinedClickable(
              onClick = {},
              onLongClick = {
                showDump = true
                Toast.makeText(context, "updated", Toast.LENGTH_SHORT).show()
              }
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineMedium
        )
      }

      Spacer(Modifier.padding(top = 12.dp))
      Text(versionString, style = MaterialTheme.typography.bodySmall)

      Spacer(Modifier.padding(top = 16.dp))

      // ライセンス情報
      Text(
        buildLicenceText(),
        style = MaterialTheme.typography.bodyMedium
      )

      // CPU 情報
      Spacer(Modifier.padding(top = 12.dp))
      Text("Android CPU Info.:", fontWeight = FontWeight.Bold)
      Spacer(Modifier.padding(top = 4.dp))
      SelectionContainer {
        Text(
          text = remember { readCpuInfo() },
          fontFamily = FontFamily.Monospace,
          style = MaterialTheme.typography.bodySmall
        )
      }

      // 長押しで表示される内部ダンプ
      if (showDump) {
        Spacer(Modifier.padding(top = 12.dp))
        SelectionContainer {
          Text(
            text = remember { collectInternalDump() },
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}

@Composable
private fun buildLicenceText(): AnnotatedString {

  val linkStyle = TextLinkStyles(
    style = SpanStyle(
      color = MaterialTheme.colorScheme.primary,
      textDecoration = TextDecoration.Underline
    )
  )

  return buildAnnotatedString {
    append("Source code : ")
    withLink(LinkAnnotation.Url(LINK_SOURCE, linkStyle)) {
      append("github/takke/cpustats")
    }
  }
}

private fun readCpuInfo(): String {

  val sb = StringBuilder()
  try {
    val args = arrayOf("/system/bin/cat", "/proc/cpuinfo")
    val process = ProcessBuilder(*args).start()
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
      reader.forEachLine {
        sb.append(it).append('\n')
      }
    }
  } catch (ex: IOException) {
    MyLog.e(ex)
  }
  return sb.toString()
}

private fun collectInternalDump(): String {

  val targets = ArrayList<String>()
  for (i in 0 until CpuInfoCollector.calcCpuCoreCount()) {
    targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
    targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
    targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
    targets.add("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_available_frequencies")
  }
  targets.add("/proc/stat")

  val sb = StringBuilder()
  for (path in targets) {
    sb.append("----\n").append(path).append("\n")
    try {
      BufferedReader(InputStreamReader(FileInputStream(path)), C.READ_BUFFER_SIZE).use { r ->
        r.forEachLine { sb.append(it).append('\n') }
      }
    } catch (ex: Exception) {
      MyLog.e(ex)
      sb.append(ex.message).append('\n')
    }
  }
  return sb.toString()
}
