package jp.takke.cpustats.widget

import jp.takke.cpustats.widget.WidgetData.snapshot
import jp.takke.cpustats.widget.WidgetData.update


/**
 * ホーム画面ウィジェット用の最新スナップショット(プロセス内共有)
 *
 * サービスが execTask のたびに [update] を呼ぶ。ウィジェットの composition から [snapshot] を読む。
 * ウィジェットは常時起動していないので、Flow で購読させず「一番新しい値」を静的に保持するだけで十分。
 */
object WidgetData {

  @Volatile
  private var current: Snapshot = Snapshot()

  data class Snapshot(
    val cpuUsage: Int = 0,
    val cpuClock: Int = 0,
    val batteryTemperature: Int = 0,
  )

  fun update(cpuUsage: Int, cpuClock: Int, batteryTemperature: Int) {
    current = Snapshot(cpuUsage, cpuClock, batteryTemperature)
  }

  fun snapshot(): Snapshot = current
}
