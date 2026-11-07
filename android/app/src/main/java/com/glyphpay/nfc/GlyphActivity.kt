package com.glyphpay.nfc

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/**
 * Невидимая прозрачная Activity. Живёт ровно столько, сколько играет анимация,
 * не перехватывает касания и сразу закрывается.
 */
class GlyphActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var controller: GlyphController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        active = true

        setShowWhenLocked(true)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val prefs = Prefs(this)
        val c = GlyphController(this)
        controller = c
        c.connect { ok ->
            if (!ok) {
                closeQuietly()
            } else {
                c.play(prefs.style, prefs.brightness) { closeQuietly() }
            }
        }

        // страховка: никогда не висим дольше 10 секунд
        handler.postDelayed({ closeQuietly() }, 10_000)
    }

    private fun closeQuietly() {
        if (isFinishing) return
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        controller?.release()
        controller = null
        active = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var active = false
    }
}
