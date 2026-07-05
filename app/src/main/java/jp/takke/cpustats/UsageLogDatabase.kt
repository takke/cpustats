package jp.takke.cpustats

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 履歴記録の 1 サンプル
 */
data class UsageLogRow(
  val recordedAt: Long,          // 記録時刻(epoch ms)
  val cpuUsage: Int,             // 全体CPU使用率[%]
  val cpuClock: Int,             // CPUクロック(KHz)
  val batteryTemperature: Int,   // バッテリー温度(0.1℃単位)
  val memUsedMb: Int,            // 使用中メモリ[MB]
)

/**
 * 履歴記録用データベース
 *
 * テーブル 1 個・クエリ 3 種のみなので Room は使わず SQLiteOpenHelper 直で実装している。
 * SQLiteOpenHelper は内部ロックを持つため、収集スレッドからの書き込みと
 * UI(履歴画面)からの読み出しが競合しても安全。
 */
class UsageLogDatabase private constructor(context: Context) :
  SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(
      """
      CREATE TABLE usage_log (
        _id INTEGER PRIMARY KEY AUTOINCREMENT,
        recorded_at INTEGER NOT NULL,
        cpu_usage INTEGER NOT NULL,
        cpu_clock INTEGER NOT NULL DEFAULT 0,
        battery_temperature INTEGER NOT NULL DEFAULT 0,
        mem_used_mb INTEGER NOT NULL DEFAULT 0
      )
      """.trimIndent()
    )
    db.execSQL("CREATE INDEX idx_usage_log_recorded_at ON usage_log(recorded_at)")
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    // 履歴データは消えても実害がないので作り直す
    db.execSQL("DROP TABLE IF EXISTS usage_log")
    onCreate(db)
  }

  /**
   * 1 サンプルを記録し、保持期間を過ぎた古い行を削除する
   */
  fun insertAndPurge(row: UsageLogRow, retentionMs: Long = C.HISTORY_RETENTION_MS) {

    val db = writableDatabase
    val values = ContentValues().apply {
      put("recorded_at", row.recordedAt)
      put("cpu_usage", row.cpuUsage)
      put("cpu_clock", row.cpuClock)
      put("battery_temperature", row.batteryTemperature)
      put("mem_used_mb", row.memUsedMb)
    }
    db.insert("usage_log", null, values)

    // rolling 削除
    db.delete("usage_log", "recorded_at < ?", arrayOf((row.recordedAt - retentionMs).toString()))
  }

  /**
   * 指定時刻以降の履歴を取得する(古い→新しい順)
   */
  fun queryRange(fromTime: Long): List<UsageLogRow> {

    val result = ArrayList<UsageLogRow>()
    readableDatabase.query(
      "usage_log",
      arrayOf("recorded_at", "cpu_usage", "cpu_clock", "battery_temperature", "mem_used_mb"),
      "recorded_at >= ?",
      arrayOf(fromTime.toString()),
      null,
      null,
      "recorded_at ASC"
    ).use { cursor ->
      while (cursor.moveToNext()) {
        result.add(
          UsageLogRow(
            recordedAt = cursor.getLong(0),
            cpuUsage = cursor.getInt(1),
            cpuClock = cursor.getInt(2),
            batteryTemperature = cursor.getInt(3),
            memUsedMb = cursor.getInt(4),
          )
        )
      }
    }
    return result
  }

  companion object {

    private const val DB_NAME = "usage_log.db"
    private const val DB_VERSION = 1

    @Volatile
    private var sInstance: UsageLogDatabase? = null

    /**
     * シングルトンで取得する(サービスと履歴画面で同一インスタンスを共有する)
     */
    fun getInstance(context: Context): UsageLogDatabase {
      return sInstance ?: synchronized(this) {
        sInstance ?: UsageLogDatabase(context).also { sInstance = it }
      }
    }

    /**
     * テスト用: シングルトンを破棄する
     */
    @androidx.annotation.VisibleForTesting
    fun resetInstanceForTest() {
      synchronized(this) {
        sInstance?.close()
        sInstance = null
      }
    }
  }
}
