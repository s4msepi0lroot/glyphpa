package dev.nothing.tapglyph

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var glyphController: GlyphController
    private lateinit var statusText: TextView
    private lateinit var accessText: TextView
    private lateinit var packageField: EditText
    private lateinit var autoSwitch: Switch
    private var registeredReceiver = false

    private val paymentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PAYMENT_DETECTED) {
                glyphController.playPaymentAnimation()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(9, 9, 9)
        window.navigationBarColor = Color.rgb(9, 9, 9)
        window.setDecorFitsSystemWindows(true)
        window.decorView.systemUiVisibility = 0

        setContentView(createContent())
        updateAccessStatus()
        askNotificationPermissionIfNeeded()

        glyphController = GlyphController(applicationContext) { message ->
            runOnUiThread { statusText.text = message }
        }
        glyphController.initialize()

        if (intent?.getBooleanExtra(EXTRA_PLAY_GLYPH, false) == true) {
            glyphController.playPaymentAnimation()
            intent.removeExtra(EXTRA_PLAY_GLYPH)
        }
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
        glyphController.onForeground()
        if (!registeredReceiver) {
            registerReceiver(paymentReceiver, IntentFilter(ACTION_PAYMENT_DETECTED), Context.RECEIVER_NOT_EXPORTED)
            registeredReceiver = true
        }
        updateAccessStatus()
    }

    override fun onPause() {
        isVisible = false
        if (registeredReceiver) {
            try {
                unregisterReceiver(paymentReceiver)
            } catch (_: IllegalArgumentException) {
            }
            registeredReceiver = false
        }
        glyphController.onBackground()
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_PLAY_GLYPH, false)) {
            glyphController.playPaymentAnimation()
            intent.removeExtra(EXTRA_PLAY_GLYPH)
        }
    }

    override fun onDestroy() {
        glyphController.release()
        super.onDestroy()
    }

    private fun createContent(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(BG)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(30))
        }
        scroll.addView(root)

        root.addView(label("NOTHING PHONE (2)  /  GLYPH EXPERIENCE", 11, MUTED, true))
        root.addView(space(14))
        root.addView(label("TAP\nGLYPH", 42, WHITE, true).apply {
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            letterSpacing = -0.035f
        })
        root.addView(space(8))
        root.addView(label("Пусть каждая покупка оставляет световой след.", 15, SOFT_WHITE, false))
        root.addView(space(24))

        val statusCard = card()
        statusText = label("Инициализация Glyph SDK…", 14, WHITE, false)
        statusCard.addView(label("СТАТУС GLYPH", 10, MUTED, true))
        statusCard.addView(space(8))
        statusCard.addView(statusText)
        root.addView(statusCard)

        root.addView(space(18))
        val testButton = button("✦   ТЕСТ АНИМАЦИИ", true) {
            glyphController.playPaymentAnimation()
        }
        root.addView(testButton, matchWidth())

        root.addView(space(22))
        root.addView(label("АВТОМАТИЗАЦИЯ", 12, MUTED, true))
        root.addView(space(10))

        autoSwitch = Switch(this).apply {
            text = "Отслеживать уведомления об оплате"
            textSize = 15f
            setTextColor(WHITE)
            isChecked = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(PREF_AUTO_TRIGGER, true)
            setOnCheckedChangeListener { _, checked ->
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putBoolean(PREF_AUTO_TRIGGER, checked).apply()
            }
        }
        root.addView(autoSwitch)
        root.addView(space(8))
        root.addView(label(
            "TapGlyph смотрит только уведомления выбранных приложений и ищет слова, связанные с покупкой. Текст уведомлений не сохраняется.",
            12, MUTED, false
        ))
        root.addView(space(16))

        accessText = label("Доступ к уведомлениям: проверяем…", 13, SOFT_WHITE, false)
        root.addView(accessText)
        root.addView(space(10))
        root.addView(button("ОТКРЫТЬ ДОСТУП К УВЕДОМЛЕНИЯМ", false) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }, matchWidth())
        root.addView(space(10))
        root.addView(button("РАЗРЕШИТЬ УВЕДОМЛЕНИЯ TAPGLYPH", false) {
            askNotificationPermissionIfNeeded(force = true)
        }, matchWidth())

        root.addView(space(22))
        root.addView(label("ПРИЛОЖЕНИЯ-ИСТОЧНИКИ", 12, MUTED, true))
        root.addView(space(8))
        packageField = EditText(this).apply {
            setTextColor(WHITE)
            setHintTextColor(MUTED)
            textSize = 13f
            setSingleLine(false)
            minLines = 2
            maxLines = 3
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(DARK_CARD, 12)
            hint = "package.name, another.package"
            setText(
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString(PREF_PACKAGES, DEFAULT_PACKAGES)
            )
        }
        root.addView(packageField, matchWidth())
        root.addView(space(8))
        root.addView(button("СОХРАНИТЬ СПИСОК", false) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(PREF_PACKAGES, packageField.text.toString())
                .apply()
            statusText.text = "Список приложений сохранён."
        }, matchWidth())
        root.addView(space(12))
        root.addView(label(
            "По умолчанию указаны ru.sberbankmobile и ru.nspk.mirpay. У конкретной версии приложения пакет может отличаться — при необходимости замени его здесь.",
            11, MUTED, false
        ))

        root.addView(space(22))
        val limitationCard = card()
        limitationCard.addView(label("ВАЖНО О РАБОТЕ В ФОНЕ", 10, MUTED, true))
        limitationCard.addView(space(8))
        limitationCard.addView(label(
            "Официальный Glyph SDK работает только пока TapGlyph открыт на экране. Если приложение свернуто, появится нейтральное уведомление: нажми его, чтобы открыть TapGlyph и включить анимацию.",
            13, SOFT_WHITE, false
        ))
        root.addView(limitationCard)

        root.addView(space(18))
        root.addView(label("TapGlyph • локальная обработка • без доступа к данным карты", 10, MUTED, false))
        return scroll
    }

    private fun updateAccessStatus() {
        if (!::accessText.isInitialized) return
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val hasAccess = enabledListeners.contains(packageName)
        accessText.text = if (hasAccess) {
            "● Доступ к уведомлениям включён"
        } else {
            "○ Доступ к уведомлениям пока не выдан"
        }
        accessText.setTextColor(if (hasAccess) ACCENT else SOFT_WHITE)
    }

    private fun askNotificationPermissionIfNeeded(force: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else if (force) {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    private fun button(text: String, primary: Boolean, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 12f
            isAllCaps = false
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(if (primary) BG else WHITE)
            background = rounded(if (primary) WHITE else DARK_CARD, 14)
            minHeight = dp(if (primary) 58 else 50)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { action() }
        }
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(DARK_CARD, 16)
    }

    private fun label(text: String, size: Int, color: Int, mono: Boolean): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        typeface = if (mono) Typeface.create("monospace", Typeface.BOLD)
            else Typeface.create("sans-serif", Typeface.NORMAL)
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    private fun space(heightDp: Int): View = Space(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun matchWidth() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS_NAME = "tapglyph_settings"
        const val PREF_AUTO_TRIGGER = "auto_trigger"
        const val PREF_PACKAGES = "source_packages"
        const val DEFAULT_PACKAGES = "ru.sberbankmobile,ru.nspk.mirpay"
        const val ACTION_PAYMENT_DETECTED = "dev.nothing.tapglyph.PAYMENT_DETECTED"
        const val EXTRA_PLAY_GLYPH = "play_glyph_animation"
        const val REQUEST_NOTIFICATIONS = 704

        @Volatile
        var isVisible: Boolean = false

        private val BG = Color.rgb(9, 9, 9)
        private val DARK_CARD = Color.rgb(25, 25, 25)
        private val WHITE = Color.rgb(248, 248, 248)
        private val SOFT_WHITE = Color.rgb(220, 220, 220)
        private val MUTED = Color.rgb(139, 139, 139)
        private val ACCENT = Color.rgb(124, 224, 170)
    }
}
