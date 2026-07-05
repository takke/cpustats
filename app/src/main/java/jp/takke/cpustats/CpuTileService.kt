package jp.takke.cpustats

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import jp.takke.cpustats.util.ResourceUtil

/**
 * クイック設定パネルの CPU 使用率タイル
 *
 * - タップで常駐(モニタリング)の ON/OFF を切り替える
 * - パネル表示中は UsageUpdateService に bind して使用率をライブ表示する
 */
@RequiresApi(Build.VERSION_CODES.N)
class CpuTileService : TileService() {

  private var mService: UsageUpdateService? = null
  private var mBound = false

  private val mMainHandler = Handler(Looper.getMainLooper())

  // コールバックの実装(収集スレッドから呼ばれるのでメインスレッドに切り替える)
  private val mCallback = UsageUpdateCallback { cpuUsages, _, _, _, _ ->
    val avgUsage = if (cpuUsages.isNotEmpty()) cpuUsages[0] else null
    mMainHandler.post {
      updateTile(avgUsage)
    }
  }

  private val mConnection = object : ServiceConnection {

    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
      mService = (binder as UsageUpdateService.LocalBinder).service
      mService?.registerCallback(mCallback)
    }

    override fun onServiceDisconnected(name: ComponentName) {
      mService = null
    }
  }

  override fun onStartListening() {
    super.onStartListening()

    updateTile(null)

    // 常駐中ならライブ表示のために bind する
    // (flags=0: サービスが起動していない場合に生成しない=タイル表示だけで収集を始めない)
    if (UsageUpdateService.isResidentRunning) {
      bindToService(0)
    }
  }

  override fun onStopListening() {

    unbindFromService()

    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()

    if (UsageUpdateService.isResidentRunning) {
      // 常駐停止
      MyLog.i("CpuTileService: stop resident")
      unbindFromService()

      val intent = Intent(this, UsageUpdateService::class.java)
      intent.action = UsageUpdateService.ACTION_STOP_RESIDENT
      startService(intent)

    } else {
      // 常駐開始
      MyLog.i("CpuTileService: start resident")

      val intent = Intent(this, UsageUpdateService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        intent.putExtra("FOREGROUND_REQUEST", true)
        startForegroundService(intent)
      } else {
        startService(intent)
      }

      // 起動直後からライブ表示できるように bind する(起動中なので生成を伴ってよい)
      bindToService(Context.BIND_AUTO_CREATE)
    }

    updateTile(null)
  }

  private fun bindToService(flags: Int) {

    if (mBound) {
      return
    }
    mBound = bindService(Intent(this, UsageUpdateService::class.java), mConnection, flags)
  }

  private fun unbindFromService() {

    mService?.unregisterCallback(mCallback)
    mService = null

    if (mBound) {
      unbindService(mConnection)
      mBound = false
    }
  }

  /**
   * タイルの状態・ラベル・アイコンを更新する
   *
   * @param avgUsage 全体CPU使用率[%](未取得時は null)
   */
  private fun updateTile(avgUsage: Int?) {

    val tile = qsTile ?: return

    val running = UsageUpdateService.isResidentRunning
    tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    tile.label = if (running && avgUsage != null) {
      "CPU $avgUsage%"
    } else {
      getString(R.string.app_name)
    }

    // 使用率レベルに応じたモノクロアイコン(single000~single100)
    val usage = if (running) (avgUsage ?: 0) else 0
    val iconId = ResourceUtil.getIconIdForCpuUsage(intArrayOf(usage, usage))
    tile.icon = Icon.createWithResource(this, iconId)

    tile.updateTile()
  }
}
