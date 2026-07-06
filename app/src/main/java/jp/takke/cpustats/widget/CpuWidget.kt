package jp.takke.cpustats.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
 * - 全体 CPU 使用率(%)+ 現在クロック周波数 + バッテリー温度を 1 行で表示
 * - タップで MainActivity を開く
 * - サービスの execTask のたびに [CpuWidget.updateAll(context)] を呼んで再描画する
 */
class CpuWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {

    provideContent {
      GlanceTheme {
        WidgetContent(context)
      }
    }
  }
}

@Composable
private fun WidgetContent(context: Context) {

  val snapshot = WidgetData.snapshot()
  val bg = GlanceTheme.colors.widgetBackground
  val fg = GlanceTheme.colors.onSurface

  Row(
    modifier = GlanceModifier
      .fillMaxSize()
      .background(bg)
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .clickable(actionStartActivity<MainActivity>()),
    verticalAlignment = Alignment.CenterVertically,
  ) {

    // 使用率アイコン(色付き single)
    Image(
      provider = ImageProvider(ResourceUtil.getIconIdForCpuUsageSingleColor(snapshot.cpuUsage)),
      contentDescription = null,
      modifier = GlanceModifier.size(48.dp),
    )

    Spacer(GlanceModifier.width(8.dp))

    Column(verticalAlignment = Alignment.CenterVertically) {

      Text(
        text = "CPU ${snapshot.cpuUsage}%",
        style = TextStyle(
          color = fg,
          fontSize = 18.sp,
          fontWeight = FontWeight.Medium,
        ),
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
