package dev.glyphpay

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private val handler = Handler(Looper.getMainLooper())
    private var controller: GlyphController? = null

    private val cBg = Color.parseColor("#0B0B0C")
    private val cCard = Color.parseColor("#161618")
    private val cCardSel = Color.parseColor("#1F1214")
    private val cText = Color.WHITE
    private val cMuted = Color.parseColor("#8E8E93")
    private val cAccent = Color.parseColor("#FF2D2D")
    private val cOk = Color.parseColor("#34C759")

    private class PermRow(val status: TextView, val button: Button)

    private lateinit var rowListener: PermRow
    private lateinit var rowOverlay: PermRow
    private lateinit var rowBattery: PermRow
    private lateinit var glyphStatus: TextView
    private lateinit var logView: TextView
    private lateinit var appsButton: Button
    private val animRows = LinkedHashMap<String, LinearLayout>()
    private val animMarks = LinkedHashMap<String, TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        setContentView(buildUi())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        releaseGlyph()
    }

    // ───────────────────────────── Glyph ─────────────────────────────

    private fun releaseGlyph() {
        controller?.release()
        controller = null
    }

    private fun playPreview(id: String = prefs.animationId) {
        releaseGlyph()
        val gc = GlyphController(this)
        controller = gc
        setGlyphStatus("Проигрываю…", cMuted)
        gc.play(Animations.byId(id), prefs.brightness) { error ->
            if (error == null) {
                setGlyphStatus("✓ Glyph работает", cOk)
            } else {
                setGlyphStatus("✗ $error", cAccent)
            }
            releaseGlyph()
        }
    }

    private fun setGlyphStatus(text: String, color: Int) {
        glyphStatus.text = text
        glyphStatus.setTextColor(color)
    }

    private fun testLikePayment() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Сначала выдайте разрешение «Поверх других окон»", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(
            this,
            "Сверните приложение или заблокируйте экран — через 5 секунд будет анимация",
            Toast.LENGTH_LONG,
        ).show()
        handler.postDelayed({
            val ok = GlyphLauncher.launch(applicationContext, null)
            prefs.log(if (ok) "✓ Тест «как при оплате»" else "✗ Тест не удалось запустить")
        }, 5000)
    }

    // ───────────────────────────── Статусы ─────────────────────────────

    private fun isListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(this, PaymentListenerService::class.java)
        return flat.split(":").any { ComponentName.unflattenFromString(it) == me }
    }

    private fun isBatteryUnrestricted(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun refresh() {
        setPerm(rowListener, isListenerEnabled(), "Включить")
        setPerm(rowOverlay, Settings.canDrawOverlays(this), "Разрешить")
        setPerm(rowBattery, isBatteryUnrestricted(), "Настроить")
        appsButton.text = "Выбрать приложения оплаты (${prefs.packages.size})"
        val log = prefs.readLog()
        logView.text = if (log.isEmpty()) "Пока пусто. Совершите оплату или нажмите тест." else log.joinToString("\n")
        for ((id, row) in animRows) {
            val selected = id == prefs.animationId
            row.background = rounded(if (selected) cCardSel else cCard, 16, if (selected) cAccent else Color.TRANSPARENT)
            animMarks[id]?.apply {
                text = if (selected) "●" else "○"
                setTextColor(if (selected) cAccent else cMuted)
            }
        }
    }

    private fun setPerm(row: PermRow, ok: Boolean, actionLabel: String) {
        row.status.text = if (ok) "● Готово" else "● Требуется"
        row.status.setTextColor(if (ok) cOk else cAccent)
        row.button.text = if (ok) "Открыть" else actionLabel
    }

    // ───────────────────────────── Выбор приложений ─────────────────────────────

    private fun chooseApps() {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val selected = prefs.packages
        val apps = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != packageName }
            .distinctBy { it.first }
            .sortedWith(
                compareByDescending<Pair<String, String>> { it.first in selected }
                    .thenByDescending { it.first in KnownApps.packages }
                    .thenBy { it.second.lowercase() },
            )
        val labels = apps.map { it.second }.toTypedArray()
        val checked = BooleanArray(apps.size) { apps[it].first in selected }

        AlertDialog.Builder(this)
            .setTitle("Какие приложения считать оплатой")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
            .setPositiveButton("Сохранить") { _, _ ->
                val listed = apps.map { it.first }.toSet()
                val result = HashSet(selected.filter { it !in listed })
                apps.forEachIndexed { i, app -> if (checked[i]) result.add(app.first) }
                prefs.packages = result
                refresh()
            }
            .setNeutralButton("По умолчанию") { _, _ ->
                prefs.packages = KnownApps.packages
                refresh()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ───────────────────────────── UI ─────────────────────────────

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(fill: Int, radiusDp: Int, stroke: Int = Color.TRANSPARENT): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun text(
        s: String,
        size: Float = 14f,
        color: Int = cText,
        bold: Boolean = false,
    ) = TextView(this).apply {
        text = s
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(cCard, 16)
        setPadding(dp(16), dp(14), dp(16), dp(14))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(10) }
    }

    private fun sectionTitle(s: String) = text(s.uppercase(), 12f, cMuted, true).apply {
        letterSpacing = 0.1f
        setPadding(dp(4), dp(14), 0, dp(8))
    }

    private fun pillButton(label: String, filled: Boolean, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(if (filled) Color.WHITE else cText)
        background = if (filled) rounded(cAccent, 24) else rounded(Color.parseColor("#232326"), 24)
        stateListAnimator = null
        minHeight = dp(46)
        minimumHeight = dp(46)
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(8) }
    }

    private fun permRow(title: String, desc: String, onClick: () -> Unit): Pair<View, PermRow> {
        val box = card()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val status = text("● …", 12f, cMuted, true)
        titles.addView(text(title, 15f, cText, true))
        titles.addView(status)
        val btn = Button(this).apply {
            isAllCaps = false
            textSize = 13f
            setTextColor(cText)
            background = rounded(Color.parseColor("#232326"), 20)
            stateListAnimator = null
            minHeight = dp(40)
            minimumHeight = dp(40)
            setOnClickListener { onClick() }
        }
        top.addView(titles)
        top.addView(btn)
        box.addView(top)
        box.addView(text(desc, 12.5f, cMuted).apply { setPadding(0, dp(6), 0, 0) })
        return box to PermRow(status, btn)
    }

    private fun switchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit): View {
        val box = card()
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        titles.addView(text(title, 15f, cText, true))
        titles.addView(text(desc, 12.5f, cMuted).apply { setPadding(0, dp(4), dp(12), 0) })
        val sw = Switch(this).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, v -> onChange(v) }
        }
        top.addView(titles)
        top.addView(sw)
        box.addView(top)
        return box
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }

        // Шапка
        root.addView(text("Glyph Pay", 32f, cText, true).apply { setPadding(dp(4), dp(8), 0, 0) })
        root.addView(
            text(
                "Красивая анимация Glyph на Nothing Phone (2), когда вы платите телефоном по NFC",
                14f,
                cMuted,
            ).apply { setPadding(dp(4), dp(4), dp(4), dp(8)) },
        )

        // Главный переключатель
        root.addView(
            switchRow(
                "Анимация при оплате",
                "Реагирует на уведомления об оплате от выбранных приложений",
                prefs.enabled,
            ) { prefs.enabled = it },
        )

        // Настройка доступа
        root.addView(sectionTitle("1 · Настройка доступа"))
        val (v1, r1) = permRow(
            "Доступ к уведомлениям",
            "Чтобы узнать, что банк прислал уведомление об оплате. Найдите «Glyph Pay» в списке и включите.",
        ) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        rowListener = r1
        root.addView(v1)

        val (v2, r2) = permRow(
            "Поверх других окон",
            "Glyph SDK работает только из активного приложения — через невидимое окно мы запускаем анимацию из фона.",
        ) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            )
        }
        rowOverlay = r2
        root.addView(v2)

        val (v3, r3) = permRow(
            "Работа в фоне",
            "Выберите «Без ограничений», чтобы система не усыпляла приложение (необязательно, но надёжнее).",
        ) { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        rowBattery = r3
        root.addView(v3)

        // Анимации
        root.addView(sectionTitle("2 · Анимация"))
        for (anim in Animations.all) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(8) }
                setOnClickListener {
                    prefs.animationId = anim.id
                    refresh()
                    playPreview(anim.id)
                }
            }
            val mark = text("○", 20f, cMuted).apply { setPadding(0, 0, dp(14), 0) }
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            col.addView(text(anim.title, 15f, cText, true))
            col.addView(text(anim.description, 12.5f, cMuted))
            row.addView(mark)
            row.addView(col)
            animRows[anim.id] = row
            animMarks[anim.id] = mark
            root.addView(row)
        }

        // Яркость
        val brightCard = card()
        val brightTitle = text("Яркость: ${prefs.brightness}%", 15f, cText, true)
        val seek = SeekBar(this).apply {
            max = 95
            progress = prefs.brightness - 5
            progressTintList = android.content.res.ColorStateList.valueOf(cAccent)
            thumbTintList = android.content.res.ColorStateList.valueOf(cAccent)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    brightTitle.text = "Яркость: ${p + 5}%"
                }

                override fun onStartTrackingTouch(sb: SeekBar?) {}

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    prefs.brightness = (sb?.progress ?: 95) + 5
                    playPreview()
                }
            })
        }
        brightCard.addView(brightTitle)
        brightCard.addView(seek)
        root.addView(brightCard)

        // Тест
        root.addView(sectionTitle("3 · Проверка"))
        root.addView(pillButton("▶  Проверить анимацию", true) { playPreview() })
        root.addView(pillButton("Тест «как при оплате» (через 5 секунд)", false) { testLikePayment() })
        glyphStatus = text("Нажмите «Проверить», чтобы увидеть анимацию на Glyph", 13f, cMuted).apply {
            setPadding(dp(4), dp(2), dp(4), dp(6))
        }
        root.addView(glyphStatus)

        // Фильтры
        root.addView(sectionTitle("4 · Что считать оплатой"))
        appsButton = pillButton("Выбрать приложения", false) { chooseApps() }
        root.addView(appsButton)
        root.addView(
            switchRow(
                "Только уведомления об оплате",
                "Ищет слова «покупка», «оплата», «списание», «payment» и игнорирует пополнения и переводы. " +
                    "Выключите — будет реагировать на любое уведомление выбранных приложений.",
                prefs.strict,
            ) { prefs.strict = it },
        )

        // Журнал
        root.addView(sectionTitle("Журнал"))
        val logCard = card()
        logView = text("", 12f, cMuted).apply { typeface = Typeface.MONOSPACE }
        logCard.addView(logView)
        root.addView(logCard)

        root.addView(
            text(
                "Если Glyph не включается, выполните на компьютере:\n" +
                    "adb shell settings put global nt_glyph_interface_debug_enable 1",
                12f,
                cMuted,
            ).apply {
                setPadding(dp(4), dp(8), dp(4), 0)
                setTextIsSelectable(true)
            },
        )

        val scroll = ScrollView(this).apply {
            setBackgroundColor(cBg)
            isFillViewport = true
            addView(root)
            // Edge-to-edge (Android 15+): отступы под системные панели
            setOnApplyWindowInsetsListener { v, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(0, bars.top, 0, bars.bottom)
                insets
            }
        }
        return scroll
    }
}
