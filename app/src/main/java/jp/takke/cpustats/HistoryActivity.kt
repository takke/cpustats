package jp.takke.cpustats

import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import jp.takke.cpustats.util.EdgeToEdgeUtil
import jp.takke.cpustats.util.MyUtil
import jp.takke.cpustats.view.HistoryGraphView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 履歴記録のグラフ表示画面
 */
class HistoryActivity : AppCompatActivity() {

  // 表示期間[ms]
  private var mPeriodMs = 24 * 60 * 60 * 1000L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_history)

    // Edge to Edge 対応
    EdgeToEdgeUtil.optimizeEdgeToEdge(findViewById(R.id.root))

    // Toolbar初期化
    setSupportActionBar(findViewById<Toolbar>(R.id.my_toolbar))
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    title = getString(R.string.title_activity_history)

    // 期間切替
    findViewById<RadioGroup>(R.id.period_group).setOnCheckedChangeListener { _, checkedId ->
      mPeriodMs = when (checkedId) {
        R.id.period_1h -> 1 * 60 * 60 * 1000L
        R.id.period_6h -> 6 * 60 * 60 * 1000L
        else -> 24 * 60 * 60 * 1000L
      }
      reloadGraphs()
    }
  }

  override fun onResume() {
    super.onResume()

    reloadGraphs()
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return true
  }

  private fun reloadGraphs() {

    val toTime = System.currentTimeMillis()
    val fromTime = toTime - mPeriodMs

    lifecycleScope.launch {

      // DB 読み出しは IO スレッドで行う
      val rows = withContext(Dispatchers.IO) {
        UsageLogDatabase.getInstance(this@HistoryActivity).queryRange(fromTime)
      }

      findViewById<TextView>(R.id.no_data_text).visibility =
        if (rows.isEmpty()) View.VISIBLE else View.GONE

      // CPU使用率(0-100% 固定レンジ)
      findViewById<HistoryGraphView>(R.id.graph_cpu).setData(
        points = rows.map { it.recordedAt to it.cpuUsage },
        fromTime = fromTime,
        toTime = toTime,
        fixedMaxValue = 100,
        valueFormatter = { "$it%" }
      )

      // バッテリー温度(0.1℃単位、自動レンジ)
      findViewById<HistoryGraphView>(R.id.graph_temperature).setData(
        points = rows.map { it.recordedAt to it.batteryTemperature },
        fromTime = fromTime,
        toTime = toTime,
        valueFormatter = { MyUtil.formatTemperature(it) }
      )

      // メモリ使用量(自動レンジ)
      findViewById<HistoryGraphView>(R.id.graph_memory).setData(
        points = rows.map { it.recordedAt to it.memUsedMb },
        fromTime = fromTime,
        toTime = toTime,
        valueFormatter = { MyUtil.formatMemSize(it) }
      )
    }
  }
}
