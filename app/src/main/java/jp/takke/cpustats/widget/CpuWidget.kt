package jp.takke.cpustats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import jp.takke.cpustats.MainActivity
import jp.takke.cpustats.util.MyUtil
import jp.takke.cpustats.util.ResourceUtil

/**
 * ホーム画面ウィジェット
 *
 * - 全体 CPU 使用率(%)+ 現在クロック周波数 + バッテリー温度
 * - タップで MainActivity を開く
 * - サービスの execTask のたびに [CpuWidget.updateAll(context)] を呼んで再描画する
 *
 * SizeMode.Responsive で 2x1(コンパクト)と 3x1 以上(拡張)の 2 レイアウトを提供する。
 */
class CpuWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(SIZE_SMALL, SIZE_MEDIUM)
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {

    provideContent {
      GlanceTheme {
        val size = LocalSize.current
        val snapshot = WidgetData.snapshot()
        if (size.width < SIZE_MEDIUM.width) {
          CompactContent(snapshot)
        } else {
          ExpandedContent(snapshot)
        }
      }
    }
  }

  companion object {
    // 2x1 セル想定(端末により 1 セル ≒ 55〜75dp)
    val SIZE_SMALL = DpSize(110.dp, 40.dp)

    // 3x1 セル以上(周波数・温度を並記できる)
    val SIZE_MEDIUM = DpSize(180.dp, 60.dp)
  }
}

@Composable
private fun CompactContent(snapshot: WidgetData.Snapshot) {

  val fg = GlanceTheme.colors.onSurface

  Row(
    modifier = commonRootModifier(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      provider = ImageProvider(ResourceUtil.getIconIdForCpuUsageSingleColor(snapshot.cpuUsage)),
      contentDescription = null,
      modifier = GlanceModifier.size(32.dp),
    )
    Spacer(GlanceModifier.width(6.dp))
    Column {
      Text(
        text = "${snapshot.cpuUsage}%",
        style = TextStyle(color = fg, fontSize = 16.sp, fontWeight = FontWeight.Medium),
      )
      if (snapshot.cpuClock > 0) {
        Text(
          text = MyUtil.formatFreq(snapshot.cpuClock),
          style = TextStyle(color = fg, fontSize = 11.sp),
        )
      }
    }
  }
}

@Composable
private fun ExpandedContent(snapshot: WidgetData.Snapshot) {

  val fg = GlanceTheme.colors.onSurface

  Row(
    modifier = commonRootModifier(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(
      provider = ImageProvider(ResourceUtil.getIconIdForCpuUsageSingleColor(snapshot.cpuUsage)),
      contentDescription = null,
      modifier = GlanceModifier.size(48.dp),
    )
    Spacer(GlanceModifier.width(8.dp))
    Column(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "CPU ${snapshot.cpuUsage}%",
        style = TextStyle(color = fg, fontSize = 18.sp, fontWeight = FontWeight.Medium),
      )
      if (snapshot.cpuClock > 0) {
        Text(
          text = MyUtil.formatFreq(snapshot.cpuClock),
          style = TextStyle(color = fg, fontSize = 13.sp),
        )
      }
      if (snapshot.batteryTemperature != 0) {
        Text(
          text = MyUtil.formatTemperature(snapshot.batteryTemperature),
          style = TextStyle(color = fg, fontSize = 12.sp),
        )
      }
    }
  }
}

@Composable
private fun commonRootModifier(): GlanceModifier =
  GlanceModifier
    .fillMaxSize()
    .background(GlanceTheme.colors.widgetBackground)
    .padding(horizontal = 8.dp, vertical = 6.dp)
    .clickable(actionStartActivity<MainActivity>())
