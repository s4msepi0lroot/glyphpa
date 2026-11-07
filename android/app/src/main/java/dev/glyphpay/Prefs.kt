package dev.glyphpay

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Настройки приложения + короткий журнал событий. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("glyph_pay", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = sp.getBoolean("enabled", true)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    var animationId: String
        get() = sp.getString("anim", Animations.DEFAULT_ID) ?: Animations.DEFAULT_ID
        set(v) = sp.edit().putString("anim", v).apply()

    /** Яркость 5..100 % */
    var brightness: Int
        get() = sp.getInt("brightness", 100)
        set(v) = sp.edit().putInt("brightness", v.coerceIn(5, 100)).apply()

    /** true – реагировать только на уведомления с ключевыми словами об оплате */
    var strict: Boolean
        get() = sp.getBoolean("strict", true)
        set(v) = sp.edit().putBoolean("strict", v).apply()

    var packages: Set<String>
        get() = HashSet(sp.getStringSet("packages", null) ?: KnownApps.packages)
        set(v) = sp.edit().putStringSet("packages", HashSet(v)).apply()

    fun log(line: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val all = (listOf("$time  $line") + readLog()).take(6)
        sp.edit().putString("log", all.joinToString("\n")).apply()
    }

    fun readLog(): List<String> {
        val raw = sp.getString("log", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split("\n")
    }
}

/** Известные платёжные приложения (можно изменить в настройках). */
object KnownApps {
    val packages: Set<String> = setOf(
        // Россия: банки и платёжные системы
        "ru.sberbankmobile",              // СберБанк Онлайн
        "ru.sberbank.mobile",
        "ru.nspk.mirpay",                 // Mir Pay
        "com.idamob.tinkoff.android",     // Т-Банк
        "ru.vtb24.mobilebanking.android", // ВТБ
        "ru.alfabank.mobile.android",     // Альфа-Банк
        "ru.raiffeisen.mobile.new",       // Райффайзен
        "ru.gazprombank.android.mobilebank.app",
        "ru.yandex.bank",
        "ru.ozon.app.android",
        "ru.sovcomcard.halva.v1",
        "ru.mtsbank.mobile",
        "ru.pochta.bank",
        "ru.rosbank.android",
        "ru.psbank.mobile",
        "com.openbank",
        // Международные кошельки
        "com.google.android.apps.walletnfcrel", // Google Кошелёк
        "com.google.android.apps.nbu.paisa.user",
        "com.samsung.android.spay",
        "com.paypal.android.p2pmobile",
        "com.revolut.revolut",
    )
}
