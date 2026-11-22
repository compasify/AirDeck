package dev.airdeck.hid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import dev.airdeck.AirDeckApp
import dev.airdeck.MainActivity
import dev.airdeck.R

/**
 * Lightweight foreground service started only while a Bluetooth HID host is connected,
 * keeping the wireless keyboard & touchpad session alive if the screen dims or the user
 * briefly switches apps.
 */
class AirDeckHidService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            (application as? AirDeckApp)?.hidController?.disconnect()
            stopSelf()
            return START_NOT_STICKY
        }

        val deviceName = intent?.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
        ensureChannel()
        val notification = buildNotification(deviceName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Throwable) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(deviceName: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val disconnectIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AirDeckHidService::class.java).apply { action = ACTION_DISCONNECT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val contentText = if (deviceName.isNotBlank()) {
            getString(R.string.notif_connected, deviceName)
        } else {
            getString(R.string.notif_ready)
        }

        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_airdeck)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.notif_action_disconnect),
                    disconnectIntent,
                ).build()
            )
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "airdeck_hid"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_DISCONNECT = "dev.airdeck.action.DISCONNECT"
        private const val EXTRA_DEVICE_NAME = "device_name"

        fun startOrUpdate(context: Context, deviceName: String) {
            val intent = Intent(context, AirDeckHidService::class.java).apply {
                putExtra(EXTRA_DEVICE_NAME, deviceName)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Throwable) {
                // Ignore if OS denies FGS from background; connection still works in foreground.
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, AirDeckHidService::class.java))
            } catch (_: Throwable) {
            }
        }
    }
}
