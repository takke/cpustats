package jp.takke.cpustats.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import jp.takke.cpustats.C
import jp.takke.cpustats.R

/**
 * CPU使用率(全体平均)の履歴を折れ線で描画するスパークライン
 *
 * 右端が最新値。履歴が容量未満の間は右詰めで描画する。
 */
class SparklineView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  // 全体CPU使用率の履歴[%](古い→新しい)
  private var mHistory = IntArray(0)

  // 履歴の最大件数
  private var mCapacity = C.USAGE_HISTORY_CAPACITY

  private val mDensity = context.resources.displayMetrics.density

  private val mLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = mDensity * 1.5f
    color = ContextCompat.getColor(context, R.color.sparkline_line)
  }

  private val mFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    color = ContextCompat.getColor(context, R.color.sparkline_line)
    alpha = 0x30
  }

  private val mGridPaint = Paint().apply {
    strokeWidth = mDensity
    // テーマの文字色を薄くしてグリッドに使う
    val tv = TypedValue()
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
    color = ContextCompat.getColor(context, tv.resourceId)
    alpha = 0x28
  }

  private val mLinePath = Path()
  private val mFillPath = Path()

  fun setHistory(history: IntArray, capacity: Int) {
    mHistory = history
    mCapacity = capacity
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val w = width.toFloat()
    val h = height.toFloat()
    if (w <= 0 || h <= 0) {
      return
    }

    // 線が上下に見切れないように少しだけ余白を取る
    val padY = mDensity * 2
    val graphHeight = h - padY * 2

    fun yOf(percent: Int): Float = padY + graphHeight * (100 - percent) / 100f

    // グリッド(0%, 50%, 100%)
    for (p in intArrayOf(0, 50, 100)) {
      val y = yOf(p)
      canvas.drawLine(0f, y, w, y, mGridPaint)
    }

    val n = mHistory.size
    if (n == 0) {
      return
    }

    val step = w / (mCapacity - 1).coerceAtLeast(1)
    val startX = w - step * (n - 1)

    mLinePath.reset()
    for (i in 0 until n) {
      val x = startX + step * i
      val y = yOf(mHistory[i])
      if (i == 0) {
        mLinePath.moveTo(x, y)
      } else {
        mLinePath.lineTo(x, y)
      }
    }

    // 塗り(折れ線の下側)
    mFillPath.set(mLinePath)
    mFillPath.lineTo(startX + step * (n - 1), yOf(0))
    mFillPath.lineTo(startX, yOf(0))
    mFillPath.close()

    canvas.drawPath(mFillPath, mFillPaint)
    canvas.drawPath(mLinePath, mLinePaint)
  }
}
