package com.glyphpay.nfc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var controller: GlyphController

    private lateinit var tvDevice: TextView
    private lateinit var tvNotif: TextView
    private lateinit var tvOverlay: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvLast: TextView
    private lateinit var btnTest: Button

    private val styles = GlyphAnimations.Style.entries.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        prefs = Prefs(this)
        controller = GlyphController(this)

        tvDevice = findViewById(R.id.tvDevice)
        tvNotif = findViewById(R.id.tvNotif)
        tvOverlay = findViewById(R.id.tvOverlay)
        tvBattery = findViewById(R.id.tvBattery)
        tvLast = findViewById(R.id.tvLast)
        btnTest = findViewById(R.id.btnTest)

        findViewById<Button>(R.id.btnNotif).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.btnRestricted).setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            )
            Toast.makeText(this, "Нажмите ⋮ вверху справа → «Разрешить ограниченные настройки»", Toast.LENGTH_LONG).show()
        }
        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        findViewById<Button>(R.id.btnBattery).setOnClickListener {
            try {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
                )
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        val sw = findViewById<MaterialSwitch>(R.id.switchEnabled)
        sw.isChecked = prefs.enabled
        sw.setOnCheckedChangeListener { _, checked -> prefs.enabled = checked }

        val spinner = findViewById<Spinner>(R.id.spinnerStyle)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, styles.map { it.title })
        spinner.setSelection(styles.indexOf(prefs.style).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.style = styles[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val slider = findViewById<Slider>(R.id.sliderBrightness)
        slider.value = prefs.brightness.toFloat().coerceIn(10f, 100f)
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) prefs.brightness = value.toInt()
        }

        val etExtra = findViewById<EditText>(R.id.etExtra)
        etExtra.setText(prefs.extraPackages)
        findViewById<Button>(R.id.btnSaveExtra).setOnClickListener {
            prefs.extraPackages = etExtra.text.toString()
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show()
        }

        // Тест №1: анимация прямо сейчас
        btnTest.setOnClickListener {
            if (!controller.isReady) {
                Toast.makeText(this, "Glyph не подключён — см. статус вверху", Toast.LENGTH_LONG).show()
                connectGlyph()
            } else {
                btnTest.isEnabled = false
                controller.play(prefs.style, prefs.brightness) { btnTest.isEnabled = true }
            }
        }

        // Тест №2: как при настоящей оплате — приложение в фоне
        findViewById<Button>(R.id.btnTestBackground).setOnClickListener {
            val app = applicationContext
            Toast.makeText(this, "Сверните приложение — анимация запустится через 5 секунд", Toast.LENGTH_LONG).show()
            Handler(Looper.getMainLooper()).postDelayed({ GlyphLauncher.trigger(app) }, 5_000)
        }
    }

    override fun onStart() {
        super.onStart()
        connectGlyph()
    }

    override fun onStop() {
        controller.release()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun connectGlyph() {
        tvDevice.text = "Подключение к Glyph…"
        controller.connect { ok ->
            tvDevice.text = when {
                ok && controller.isPhone2() -> "✅ Glyph подключён (Nothing Phone (2))"
                ok -> "⚠️ Glyph подключён, но это не Phone (2) — анимация рассчитана на Phone (2)"
                else -> "❌ Не удалось подключиться к Glyph.\nВыполните на компьютере:\nadb shell settings put global nt_glyph_interface_debug_enable 1"
            }
        }
    }

    private fun refreshStatus() {
        val notifOk = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val overlayOk = Settings.canDrawOverlays(this)
        val batteryOk = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

        tvNotif.text = if (notifOk) "✅ Доступ к уведомлениям выдан" else "⛔ Нужен доступ к уведомлениям"
        tvOverlay.text = if (overlayOk) "✅ Поверх других окон разрешено" else "⛔ Нужно «Поверх других окон»"
        tvBattery.text = if (batteryOk) "✅ Без ограничений батареи" else "⚠️ Рекомендуется снять ограничения батареи"

        val last = prefs.lastEvent
        tvLast.text = if (last.isBlank()) "Пока не срабатывало" else last
    }
}
