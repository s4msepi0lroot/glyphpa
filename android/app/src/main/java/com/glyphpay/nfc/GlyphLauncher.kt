package com.glyphpay.nfc

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Glyph SDK разрешает работу только приложению на переднем плане.
 * Поэтому при оплате мы на пару секунд показываем невидимую прозрачную Activity.
 *
 * Чтобы система разрешила старт Activity из фона, приложению нужно право
 * «Поверх других окон» и (Android 15+) видимое overlay-окно — мы создаём крошечное 1×1.
 */
object GlyphLauncher {
    private const val TAG = "GlyphLauncher"

    fun trigger(context: Context) {
        if (GlyphActivity.active) return
        val app = context.applicationContext
        val handler = Handler(Looper.getMainLooper())

        var overlay: View? = null
        val wm = app.getSystemService(WindowManager::class.java)

        if (Settings.canDrawOverlays(app)) {
            try {
                val view = View(app)
                val params = WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                ).apply { gravity = Gravity.TOP or Gravity.START }
                wm.addView(view, params)
                overlay = view
            } catch (e: Throwable) {
                Log.w(TAG, "Не удалось создать overlay", e)
            }
        } else {
            Log.w(TAG, "Нет разрешения «Поверх других окон» — анимация из фона может не запуститься")
        }

        try {
            val intent = Intent(app, GlyphActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            app.startActivity(intent)
        } catch (e: Throwable) {
            Log.e(TAG, "Не удалось запустить GlyphActivity", e)
        }

        overlay?.let { v ->
            handler.postDelayed({
                try {
                    wm.removeView(v)
                } catch (_: Throwable) {
                }
            }, 2_500)
        }
    }
}
