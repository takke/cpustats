package jp.takke.cpustats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import jp.takke.cpustats.util.EdgeToEdgeUtil

class ConfigActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_config)

    // Edge to Edge 対応
    EdgeToEdgeUtil.optimizeEdgeToEdge(findViewById(R.id.root))

    supportFragmentManager
      .beginTransaction()
      .replace(R.id.settings_container, ConfigFragment())
      .commit()
  }
}
