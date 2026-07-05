package jp.takke.cpustats

import android.content.Context
import android.content.SharedPreferences

/**
 * androidx.preference.PreferenceManager.getDefaultSharedPreferences() 互換のヘルパー
 * ("<packageName>_preferences" 名の SharedPreferences を返す)
 */
internal fun Context.defaultSharedPreferences(): SharedPreferences {
  return getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
}
