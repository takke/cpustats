package jp.takke.cpustats

import android.app.AlertDialog
import android.content.Context

object NotificationPermissionUtil {

    fun showNotificationPermissionRationaleDialog(context: Context, onOk: () -> Unit, onCancel: () -> Unit) {

        AlertDialog.Builder(context)
            .setMessage(R.string.require_notification_permission)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                MyLog.d("通知権限: OK")
                onOk()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                MyLog.d("通知権限: キャンセル")
                onCancel()
            }
            .show()
    }

}
