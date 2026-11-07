package com.glyphpay.nfc

import android.app.Notification
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Следит за уведомлениями Сбера / Mir Pay / Google Wallet и др.
 * Как только приходит уведомление об оплате — запускает Glyph-анимацию.
 */
class PaymentListenerService : NotificationListenerService() {

    private var lastTrigger = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val prefs = Prefs(this)
        if (!prefs.enabled) return
        if (sbn.packageName == packageName) return

        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        // старые уведомления (например, после перезапуска сервиса) пропускаем
        if (System.currentTimeMillis() - sbn.postTime > 15_000) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = listOf(
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT
        ).mapNotNull { extras.getCharSequence(it)?.toString() }.joinToString(" ")

        if (!PaymentDetector.match(sbn.packageName, title, text, prefs.extraPackages)) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTrigger < 6_000) return // защита от дублей
        lastTrigger = now

        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        prefs.lastEvent = "$time · ${sbn.packageName}\n${title.take(60)}"
        Log.d(TAG, "Оплата обнаружена: ${sbn.packageName} / $title")

        GlyphLauncher.trigger(this)
    }

    companion object {
        private const val TAG = "PaymentListener"
    }
}
