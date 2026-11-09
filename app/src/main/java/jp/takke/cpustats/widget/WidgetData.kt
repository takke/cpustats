package jp.takke.cpustats.widget

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * ホーム画面ウィジェット用の最新スナップショット(プロセス内共有)
 *
 * Compose の snapshot state で保持することで、Glance セッションの composition が
 * [snapshot] を読むと観測対象になり、サービスからの [update] で自動的に再コンポーズされる。
 * (ただの変数にすると update イベントが来ても composition が無効化されず、
 * セッションが作り直されるまでウィジェットが更新されない)
 */
object WidgetData {

  data class Snapshot(
    val cpuUsage: Int = 0,
    val cpuClock: Int = 0,
    val batteryTemperature: Int = 0,
    // 温度を華氏で表示するか
    val useFahrenheit: Boolean = false,
  )

  private var state by mutableStateOf(Snapshot())

  fun update(snapshot: Snapshot) {
    state = snapshot
  }

  /**
   * 最新のスナップショットを返す(composition 内から呼ぶと状態観測される)
   */
  fun snapshot(): Snapshot = state
}
