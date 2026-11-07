package dev.glyphpay

import android.app.Notification
import android.service.notification.StatusBarNotification

/** Решает, похоже ли уведомление на оплату картой/телефоном (NFC). */
object PaymentDetector {

    private val POSITIVE = Regex(
        "оплат|покупк|списан|платеж|платёж|purchase|payment|paid|spent|charged|tap to pay|bezahl|achat|compra",
        RegexOption.IGNORE_CASE,
    )

    // Пополнения, переводы, возвраты и кэшбэк — это не оплата в магазине
    private val NEGATIVE = Regex(
        "пополн|зачисл|поступ|получен|получил|возврат|кэшбэк|кешбэк|cashback|refund|received|deposit|перевод|transfer|кредит.*одобр",
        RegexOption.IGNORE_CASE,
    )

    enum class Verdict { TRIGGER, IGNORED_TEXT, IGNORED_APP, IGNORED_SYSTEM }

    class Result(val verdict: Verdict, val summary: String)

    fun check(prefs: Prefs, sbn: StatusBarNotification, ownPackage: String): Result {
        val n = sbn.notification
        val pkg = sbn.packageName
        val text = extractText(n)
        val summary = "$pkg: ${text.take(70)}"

        if (pkg == ownPackage) return Result(Verdict.IGNORED_SYSTEM, summary)
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return Result(Verdict.IGNORED_SYSTEM, summary)
        if (sbn.isOngoing) return Result(Verdict.IGNORED_SYSTEM, summary)

        if (pkg !in prefs.packages) return Result(Verdict.IGNORED_APP, summary)

        if (prefs.strict) {
            if (NEGATIVE.containsMatchIn(text)) return Result(Verdict.IGNORED_TEXT, summary)
            if (!POSITIVE.containsMatchIn(text)) return Result(Verdict.IGNORED_TEXT, summary)
        }
        return Result(Verdict.TRIGGER, summary)
    }

    private fun extractText(n: Notification): String {
        val e = n.extras
        val parts = listOf(
            e.getCharSequence(Notification.EXTRA_TITLE),
            e.getCharSequence(Notification.EXTRA_TEXT),
            e.getCharSequence(Notification.EXTRA_BIG_TEXT),
            e.getCharSequence(Notification.EXTRA_SUB_TEXT),
            n.tickerText,
        )
        return parts.filterNotNull().joinToString(" | ") { it.toString() }
    }
}
