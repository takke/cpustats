package jp.takke.cpustats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ConfigActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, ConfigFragment())
                .commit()
    }
}
