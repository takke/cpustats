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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 履歴記録の時系列グラフ
 *
 * - X 軸は時刻(表示期間の左端 = fromTime、右端 = toTime)
 * - 記録間隔より大きく空いた区間(サービス停止・スリープ等)は線を切る
 */
class HistoryGraphView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  // (記録時刻, 値) のリスト(古い→新しい順)
  private var mPoints: List<Pair<Long, Int>> = emptyList()

  private var mFromTime = 0L
  private var mToTime = 0L

  // Y 軸の最大値(null なら データから自動決定)
  private var mFixedMaxValue: Int? = null

  // Y 軸ラベルの整形
  private var mValueFormatter: (Int) -> String = { it.toString() }

  private val mDensity = context.resources.displayMetrics.density

  private val mTimeFormat = SimpleDateFormat("HH:mm", Locale.US)

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
  }

  private val mLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = mDensity * 10
  }

  init {
    // テーマの文字色をグリッド・ラベルに使う
    val tv = TypedValue()
    context.theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
    val textColor = ContextCompat.getColor(context, tv.resourceId)
    mGridPaint.color = textColor
    mGridPaint.alpha = 0x28
    mLabelPaint.color = textColor
    mLabelPaint.alpha = 0xa0
  }

  /**
   * グラフデータの設定
   *
   * @param points (記録時刻, 値) のリスト(古い→新しい順)
   * @param fromTime 表示期間の開始時刻(epoch ms)
   * @param toTime 表示期間の終了時刻(epoch ms)
   * @param fixedMaxValue Y 軸の最大値(null ならデータから自動決定)
   * @param valueFormatter Y 軸ラベルの整形
   */
  fun setData(
    points: List<Pair<Long, Int>>,
    fromTime: Long,
    toTime: Long,
    fixedMaxValue: Int? = null,
    valueFormatter: (Int) -> String = { it.toString() }
  ) {
    mPoints = points
    mFromTime = fromTime
    mToTime = toTime
    mFixedMaxValue = fixedMaxValue
    mValueFormatter = valueFormatter
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val w = width.toFloat()
    val h = height.toFloat()
    if (w <= 0 || h <= 0 || mToTime <= mFromTime) {
      return
    }

    val labelHeight = mLabelPaint.textSize + mDensity * 2
    val padTop = mDensity * 4
    val graphBottom = h - labelHeight
    val graphHeight = graphBottom - padTop

    // Y 軸の最大値
    val maxValue = mFixedMaxValue
      ?: mPoints.maxOfOrNull { it.second }?.coerceAtLeast(1)
      ?: 1

    fun xOf(time: Long): Float = w * (time - mFromTime) / (mToTime - mFromTime)
    fun yOf(value: Int): Float = padTop + graphHeight * (maxValue - value) / maxValue

    // グリッド(縦 4 分割)
    for (i in 0..4) {
      val y = padTop + graphHeight * i / 4
      canvas.drawLine(0f, y, w, y, mGridPaint)
    }

    // Y 軸ラベル(最大値・中間値)
    canvas.drawText(mValueFormatter(maxValue), mDensity * 2, padTop + mLabelPaint.textSize, mLabelPaint)
    canvas.drawText(
      mValueFormatter(maxValue / 2),
      mDensity * 2, padTop + graphHeight / 2 + mLabelPaint.textSize, mLabelPaint
    )

    // X 軸ラベル(開始・終了時刻)
    val fromLabel = mTimeFormat.format(Date(mFromTime))
    val toLabel = mTimeFormat.format(Date(mToTime))
    canvas.drawText(fromLabel, mDensity * 2, h - mDensity * 2, mLabelPaint)
    canvas.drawText(
      toLabel,
      w - mLabelPaint.measureText(toLabel) - mDensity * 2, h - mDensity * 2, mLabelPaint
    )

    if (mPoints.isEmpty()) {
      return
    }

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
      // 塗り(セグメントの下側)
      fillPath.lineTo(lastX, yOf(0))
      fillPath.lineTo(segmentStartX, yOf(0))
      fillPath.close()
      inSegment = false
    }

    for ((time, value) in mPoints) {
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

    canvas.drawPath(fillPath, mFillPaint)
    canvas.drawPath(linePath, mLinePaint)
  }
}
