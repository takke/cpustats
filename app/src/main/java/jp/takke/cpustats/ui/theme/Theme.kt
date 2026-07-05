package jp.takke.cpustats.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 既存 values/colors.xml の値と揃える。ダーク側は従来のダーク UI をそのまま維持する
private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF00C21C),        // 通知色パレットの緑
  onPrimary = Color(0xFF000000),
  background = Color(0xFF222222),
  onBackground = Color(0xFFEEEEEE),
  surface = Color(0xFF222222),
  onSurface = Color(0xFFEEEEEE),
  surfaceVariant = Color(0xFF333333),
  onSurfaceVariant = Color(0xFFDDDDDD),
)

private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF009018),
  onPrimary = Color(0xFFFFFFFF),
  background = Color(0xFFEEEEEE),
  onBackground = Color(0xFF111111),
  surface = Color(0xFFEEEEEE),
  onSurface = Color(0xFF111111),
  surfaceVariant = Color(0xFFE2E2E2),
  onSurfaceVariant = Color(0xFF333333),
)

/**
 * アプリ共通の Compose テーマ
 *
 * @param useDynamicColor Android 12+ の Dynamic Color を使うか(デフォルト false: 既存配色を維持)
 */
@Composable
fun CpuStatsTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  useDynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    content = content
  )
}
