package jp.takke.cpustats

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24])
class UsageLogDatabaseTest {

  private val db get() = UsageLogDatabase.getInstance(RuntimeEnvironment.getApplication())

  @Before
  fun setUp() {
    // シングルトンはテスト間で生き残るため毎回リセットする
    UsageLogDatabase.resetInstanceForTest()
    RuntimeEnvironment.getApplication().deleteDatabase("usage_log.db")
  }

  private fun row(recordedAt: Long, cpuUsage: Int = 50) = UsageLogRow(
    recordedAt = recordedAt,
    cpuUsage = cpuUsage,
    cpuClock = 1500000,
    batteryTemperature = 325,
    memUsedMb = 3277,
  )

  @Test
  fun insertAndQuery() {

    db.insertAndPurge(row(recordedAt = 1000, cpuUsage = 10))
    db.insertAndPurge(row(recordedAt = 2000, cpuUsage = 20))
    db.insertAndPurge(row(recordedAt = 3000, cpuUsage = 30))

    // 全件(古い→新しい順)
    val all = db.queryRange(0)
    assertThat(all).hasSize(3)
    assertThat(all.map { it.cpuUsage }).containsExactly(10, 20, 30)

    // カラム値の往復確認
    assertThat(all[0].recordedAt).isEqualTo(1000L)
    assertThat(all[0].cpuClock).isEqualTo(1500000)
    assertThat(all[0].batteryTemperature).isEqualTo(325)
    assertThat(all[0].memUsedMb).isEqualTo(3277)

    // 範囲指定
    assertThat(db.queryRange(2000).map { it.cpuUsage }).containsExactly(20, 30)
    assertThat(db.queryRange(3001)).isEmpty()
  }

  @Test
  fun purgeOldRows() {

    // 保持期間 1000ms として記録する
    db.insertAndPurge(row(recordedAt = 1000, cpuUsage = 10), retentionMs = 1000)
    db.insertAndPurge(row(recordedAt = 1500, cpuUsage = 15), retentionMs = 1000)

    // recordedAt=3000 の挿入で 2000 より古い行が消える
    db.insertAndPurge(row(recordedAt = 3000, cpuUsage = 30), retentionMs = 1000)

    val all = db.queryRange(0)
    assertThat(all.map { it.cpuUsage }).containsExactly(30)
  }
}
