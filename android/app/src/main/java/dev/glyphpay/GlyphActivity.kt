package dev.glyphpay

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/**
 * Невидимая прозрачная activity. Не перехватывает касания и фокус,
 * проигрывает анимацию Glyph и сразу закрывается.
 */
class GlyphActivity : Activity() {

    private var controller: GlyphController? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        )
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)

        val prefs = Prefs(this)
        val animation = Animations.byId(
            intent.getStringExtra(GlyphLauncher.EXTRA_ANIMATION) ?: prefs.animationId,
        )

        val gc = GlyphController(this)
        controller = gc
        gc.play(animation, prefs.brightness) { error ->
            if (error != null) prefs.log("⚠ $error")
            finish()
        }
        // Страховка: в любом случае закрываемся
        handler.postDelayed({ finish() }, animation.durationMs + 6000L)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        controller?.release()
        controller = null
        super.onDestroy()
    }
}
