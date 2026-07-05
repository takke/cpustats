package jp.takke.cpustats.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * `painterResource` が対応していない Drawable(layer-list / adaptive-icon など)を
 * Bitmap にラスタライズして Painter として返す
 *
 * 本アプリの通知アイコン群(single_XXX / dual_X_X / tri_XXX / quad_XXXX / freq_XX)は
 * すべて layer-list なため、UI で表示する場合はこの関数を使う必要がある
 */
@Composable
fun rememberDrawablePainter(@DrawableRes id: Int): Painter {
  val context = LocalContext.current
  return remember(id) {
    val drawable = ContextCompat.getDrawable(context, id)
      ?: return@remember BitmapPainter(
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
      )
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    BitmapPainter(bitmap.asImageBitmap())
  }
}
