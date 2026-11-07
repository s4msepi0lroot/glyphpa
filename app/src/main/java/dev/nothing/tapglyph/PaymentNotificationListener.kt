package dev.nothing.tapglyph

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Watches notifications only from package names chosen by the user.
 * Notification text is inspected in memory and is never saved or logged.
 */
class PaymentNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val preferences = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        if (!preferences.getBoolean(MainActivity.PREF_AUTO_TRIGGER, true)) return

        val allowedPackages = preferences.getString(MainActivity.PREF_PACKAGES, MainActivity.DEFAULT_PACKAGES)
            .orEmpty()
            .split(',', ';', '\n')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (allowedPackages.isEmpty() || allowedPackages.none { it == sbn.packageName.lowercase() }) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigBody = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val textForMatching = "$title $body $bigBody".lowercase()

        val paymentWords = listOf(
            "оплат", "покупк", "списан", "платёж", "платеж",
            "payment", "purchase", "transaction"
        )
        val failureWords = listOf(
            "отклон", "отмен", "ошиб", "не прошло", "не удалось",
            "declined", "failed", "rejected", "cancelled"
        )
        if (paymentWords.none { textForMatching.contains(it) }) return
        if (failureWords.any { textForMatching.contains(it) }) return

        val now = System.currentTimeMillis()
        synchronized(DEDUP_LOCK) {
            if (now - lastTriggeredAt < 3_500L) return
            lastTriggeredAt = now
        }

        // Do not reveal the bank, merchant, amount or notification contents in our own alert.
        if (MainActivity.isVisible) {
            sendBroadcast(
                Intent(MainActivity.ACTION_PAYMENT_DETECTED).setPackage(packageName)
            )
        } else {
            postTapToPlayNotification()
        }
    }

    private fun postTapToPlayNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val channelId = "tapglyph_payment_detected"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Обнаружение оплаты",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Нейтральное уведомление с кнопкой запуска Glyph-анимации"
                }
            )
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_PLAY_GLYPH, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            701,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("TapGlyph: обнаружено платёжное уведомление")
            .setContentText("Нажмите, чтобы открыть приложение и запустить Glyph-анимацию")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()

        try {
            manager.notify(701, notification)
        } catch (_: SecurityException) {
            // The user may have denied POST_NOTIFICATIONS; the app explains this in its UI.
        }
    }

    companion object {
        private val DEDUP_LOCK = Any()
        private var lastTriggeredAt: Long = 0L
    }
}
