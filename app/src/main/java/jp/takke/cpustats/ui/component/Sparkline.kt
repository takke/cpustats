package jp.takke.cpustats.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import jp.takke.cpustats.C

/**
 * CPU 使用率(全体平均)の履歴を折れ線で描画するスパークライン
 *
 * 右端が最新。履歴が容量未満の間は右詰めで描画する
 */
@Composable
fun Sparkline(
  history: IntArray,
  modifier: Modifier = Modifier,
  capacity: Int = C.USAGE_HISTORY_CAPACITY,
  lineColor: Color = MaterialTheme.colorScheme.primary,
) {
  val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
  val fillColor = lineColor.copy(alpha = 0.18f)

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 0 || h <= 0) return@Canvas

    val padY = 2.dp.toPx()
    val graphHeight = h - padY * 2
    fun yOf(percent: Int): Float = padY + graphHeight * (100 - percent) / 100f

    // グリッド(0%, 50%, 100%)
    val gridStroke = Stroke(width = 1.dp.toPx())
    for (p in intArrayOf(0, 50, 100)) {
      val y = yOf(p)
      drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = gridStroke.width)
    }

    val n = history.size
    if (n == 0) return@Canvas

    val step = w / (capacity - 1).coerceAtLeast(1)
    val startX = w - step * (n - 1)

    val linePath = Path()
    val fillPath = Path()
    for (i in 0 until n) {
      val x = startX + step * i
      val y = yOf(history[i])
      if (i == 0) {
        linePath.moveTo(x, y)
        fillPath.moveTo(x, y)
      } else {
        linePath.lineTo(x, y)
        fillPath.lineTo(x, y)
      }
    }
    // 塗り(折れ線の下側)
    fillPath.lineTo(startX + step * (n - 1), yOf(0))
    fillPath.lineTo(startX, yOf(0))
    fillPath.close()

    drawPath(fillPath, color = fillColor)
    drawPath(linePath, color = lineColor, style = Stroke(width = 1.5.dp.toPx()))
  }
}
