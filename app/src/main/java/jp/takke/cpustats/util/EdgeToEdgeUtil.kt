package jp.takke.cpustats.util

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

object EdgeToEdgeUtil {

  /**
   * EdgeToEdge の場合、ステータスバー、ナビゲーションバーのパディングを設定する
   */
  fun optimizeEdgeToEdge(rootView: View) {

    ViewCompat.setOnApplyWindowInsetsListener(rootView) { root, windowInsets ->
      val insets = windowInsets.getInsets(
        // システムバー＝ステータスバー、ナビゲーションバー
        WindowInsetsCompat.Type.systemBars() or
                // ディスプレイカットアウト
                WindowInsetsCompat.Type.displayCutout(),
      )

      root.updatePadding(
        top = insets.top,
        bottom = insets.bottom,
        left = insets.left,
        right = insets.right,
      )

      // この Activity で WindowInsets を消費する（子Viewには伝播しない）
      WindowInsetsCompat.CONSUMED
    }
  }

  /**
   * EdgeToEdge の場合、ダミーのステータスバーエリア背景を設定し、ナビゲーションバーはパディングを設定する
   */
  fun optimizeEdgeToEdgeWithSDummyStatusBar(rootView: View, dummySystemBarOnEdgeToEdge: View) {

    // ダミーのステータスバーエリア背景を設定する
    ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, windowInsets ->

      val insets = windowInsets.getInsets(
        // システムバー＝ステータスバー、ナビゲーションバー
        WindowInsetsCompat.Type.systemBars() or
                // ディスプレイカットアウト
                WindowInsetsCompat.Type.displayCutout(),
      )

      rootView.updatePadding(
        bottom = insets.bottom,
        left = insets.left,
        right = insets.right,
      )

      dummySystemBarOnEdgeToEdge.updateLayoutParams {
        height = insets.top
      }

      // この Activity で WindowInsets を消費する（子Viewには伝播しない）
      WindowInsetsCompat.CONSUMED
    }
  }
}
