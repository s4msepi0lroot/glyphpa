package com.glyphpay.nfc

/**
 * Решает, похоже ли уведомление на оплату картой / телефоном.
 */
object PaymentDetector {

    /** Банки и кошельки. Можно дополнить в настройках приложения. */
    val DEFAULT_PACKAGES = setOf(
        "ru.sberbankmobile",                       // СберБанк Онлайн (SberPay)
        "ru.nspk.mirpay",                          // Mir Pay
        "com.google.android.apps.walletnfcrel",    // Google Кошелёк
        "com.idamob.tinkoff.android",              // Т-Банк
        "ru.vtb24.mobilebanking.android",          // ВТБ
        "ru.alfabank.mobile.android",              // Альфа-Банк
        "ru.gazprombank.android.mobilebank.app",   // Газпромбанк
        "ru.raiffeisennews",                       // Райффайзен
        "ru.yandex.bank",                          // Яндекс Банк / Пэй
        "ru.ozon.app.android",                     // Ozon Банк
        "ru.mw"                                    // QIWI
    )

    /** SMS-приложения: для них дополнительно проверяем отправителя (900 и т.п.). */
    private val SMS_PACKAGES = setOf(
        "com.google.android.apps.messaging",
        "com.android.mms",
        "com.samsung.android.messaging",
        "com.nothing.messaging"
    )

    private val SMS_SENDER = Regex("(^|\\W)(900|sber|сбер|tinkoff|т-банк|t-bank|vtb|втб|alfa|альфа|mir|мир)", RegexOption.IGNORE_CASE)

    private val PAY_WORDS = Regex(
        "(оплат|покупк|списан|списал|платеж|платёж|sberpay|сберпэй|mir pay|tap to pay|payment|paid|purchase|charged|spent)",
        RegexOption.IGNORE_CASE
    )

    private val IGNORE_WORDS = Regex(
        "(зачисл|поступл|пополн|возврат|отмен|отклон|отказ|declined|refund|пароль|код подтверждения|подтвердите|otp|одноразов)",
        RegexOption.IGNORE_CASE
    )

    fun match(pkg: String, title: String, text: String, extraPackages: String): Boolean {
        val extra = extraPackages.split(',', ';', ' ', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val isKnown = pkg in DEFAULT_PACKAGES || pkg in extra
        val isSms = pkg in SMS_PACKAGES
        if (!isKnown && !isSms) return false

        val body = "$title $text"
        if (isSms && !SMS_SENDER.containsMatchIn(title)) return false
        if (IGNORE_WORDS.containsMatchIn(body)) return false
        return PAY_WORDS.containsMatchIn(body)
    }
}
