package jp.takke.cpustats.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.takke.cpustats.C
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 履歴記録の時系列グラフ(Compose Canvas 版)
 *
 * - X 軸は時刻(表示期間の左端 = fromTime、右端 = toTime)
 * - 記録間隔の 3 倍以上空いた区間(サービス停止・スリープ等)は線を切る
 *
 * @param points (記録時刻, 値) のリスト(古い→新しい順)
 * @param fixedMaxValue Y 軸の最大値(null ならデータから自動決定)
 * @param valueFormatter Y 軸ラベルの整形
 */
@Composable
fun HistoryGraph(
  points: List<Pair<Long, Int>>,
  fromTime: Long,
  toTime: Long,
  modifier: Modifier = Modifier,
  fixedMaxValue: Int? = null,
  valueFormatter: (Int) -> String = { it.toString() },
) {
  val lineColor = MaterialTheme.colorScheme.primary
  val fillColor = lineColor.copy(alpha = 0.18f)
  val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
  val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.63f)

  val textMeasurer: TextMeasurer = rememberTextMeasurer()
  val labelStyle = remember(labelColor) {
    TextStyle(color = labelColor, fontSize = 10.sp)
  }
  val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0 || h <= 0 || toTime <= fromTime) return@Canvas

    // ラベル領域の確保(下端 = 時刻ラベル)
    val labelSizePx = 10.sp.toPx()
    val labelHeight = labelSizePx + 2.dp.toPx()
    val padTop = 4.dp.toPx()
    val graphBottom = h - labelHeight
    val graphHeight = graphBottom - padTop

    // Y 軸の最大値
    val maxValue = fixedMaxValue
      ?: points.maxOfOrNull { it.second }?.coerceAtLeast(1)
      ?: 1

    fun xOf(time: Long): Float = w * (time - fromTime) / (toTime - fromTime)
    fun yOf(value: Int): Float = padTop + graphHeight * (maxValue - value) / maxValue

    // グリッド(縦 4 分割)
    for (i in 0..4) {
      val y = padTop + graphHeight * i / 4
      drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
    }

    // Y 軸ラベル(最大値・中間値)
    drawText(
      textMeasurer, valueFormatter(maxValue), Offset(2.dp.toPx(), padTop), labelStyle
    )
    drawText(
      textMeasurer, valueFormatter(maxValue / 2),
      Offset(2.dp.toPx(), padTop + graphHeight / 2), labelStyle
    )

    // X 軸ラベル(開始・終了時刻)
    val fromLabel = timeFormat.format(Date(fromTime))
    val toLabel = timeFormat.format(Date(toTime))
    drawText(
      textMeasurer, fromLabel, Offset(2.dp.toPx(), graphBottom + 2.dp.toPx()), labelStyle
    )
    val toLabelLayout = textMeasurer.measure(toLabel, labelStyle)
    drawText(
      textMeasurer, toLabel,
      Offset(w - toLabelLayout.size.width - 2.dp.toPx(), graphBottom + 2.dp.toPx()),
      labelStyle
    )

    if (points.isEmpty()) return@Canvas

    // 記録間隔の 3 倍以上空いた区間は欠測とみなして線を切る
    val gapThreshold = C.HISTORY_RECORD_INTERVAL_MS * 3

    val linePath = Path()
    val fillPath = Path()
    var segmentStartX = 0f
    var lastX = 0f
    var lastTime = 0L
    var inSegment = false

    fun closeSegment() {
      if (!inSegment) return
      fillPath.lineTo(lastX, yOf(0))
      fillPath.lineTo(segmentStartX, yOf(0))
      fillPath.close()
      inSegment = false
    }

    for ((time, value) in points) {
      val x = xOf(time)
      val y = yOf(value.coerceIn(0, maxValue))

      if (!inSegment || time - lastTime > gapThreshold) {
        closeSegment()
        linePath.moveTo(x, y)
        fillPath.moveTo(x, y)
        segmentStartX = x
        inSegment = true
      } else {
        linePath.lineTo(x, y)
        fillPath.lineTo(x, y)
      }
      lastX = x
      lastTime = time
    }
    closeSegment()

    drawPath(fillPath, color = fillColor)
    drawPath(linePath, color = lineColor, style = Stroke(width = 1.5.dp.toPx()))
  }
}
