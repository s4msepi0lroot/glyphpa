package dev.glyphpay

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Следит за уведомлениями выбранных платёжных приложений (Сбер, Mir Pay, Т-Банк,
 * Google Кошелёк и т. д.). Когда приходит уведомление об оплате — запускает анимацию Glyph.
 */
class PaymentListenerService : NotificationListenerService() {

    private var lastKey: String? = null
    private var lastKeyTime = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val prefs = Prefs(this)
        if (!prefs.enabled) return

        val result = PaymentDetector.check(prefs, sbn, packageName)
        when (result.verdict) {
            PaymentDetector.Verdict.TRIGGER -> {
                // Банки часто обновляют одно и то же уведомление — не дублируем
                val now = System.currentTimeMillis()
                val key = "${sbn.packageName}/${sbn.id}/${sbn.tag}"
                if (key == lastKey && now - lastKeyTime < 6000) return
                lastKey = key
                lastKeyTime = now

                val started = GlyphLauncher.launch(this, null)
                prefs.log(
                    if (started) "✓ Оплата → анимация: ${result.summary}"
                    else "✗ Оплата найдена, но не запущена (нет разрешения «Поверх других окон»?): ${result.summary}",
                )
            }
            PaymentDetector.Verdict.IGNORED_TEXT ->
                prefs.log("· пропущено (не похоже на оплату): ${result.summary}")
            else -> Unit
        }
    }

    override fun onListenerDisconnected() {
        // Система иногда отвязывает сервис — просим подключить обратно
        requestRebind(ComponentName(this, PaymentListenerService::class.java))
    }
}
