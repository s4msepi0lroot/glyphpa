package com.glyphpay.nfc

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphManager

/**
 * Обёртка над Nothing Glyph SDK: подключение, открытие сессии, проигрывание анимации по кадрам.
 */
class GlyphController(context: Context) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private var manager: GlyphManager? = null
    private var ready = false
    private var pending: ((Boolean) -> Unit)? = null
    private var player: Runnable? = null

    val isReady: Boolean get() = ready

    fun isPhone2(): Boolean = try {
        Common.is22111()
    } catch (e: Throwable) {
        false
    }

    fun connect(onResult: (Boolean) -> Unit) {
        if (ready) {
            onResult(true)
            return
        }
        pending = onResult
        try {
            val gm = GlyphManager.getInstance(appContext)
            manager = gm
            gm.init(object : GlyphManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    ready = try {
                        gm.register(Glyph.DEVICE_22111)
                        gm.openSession()
                        true
                    } catch (e: Throwable) {
                        Log.e(TAG, "Не удалось открыть сессию Glyph", e)
                        false
                    }
                    val cb = pending
                    pending = null
                    cb?.invoke(ready)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    ready = false
                    try {
                        gm.closeSession()
                    } catch (_: Throwable) {
                    }
                }
            })
        } catch (e: Throwable) {
            Log.e(TAG, "Glyph SDK недоступен", e)
            val cb = pending
            pending = null
            cb?.invoke(false)
        }
    }

    fun play(style: GlyphAnimations.Style, brightnessPercent: Int, onDone: () -> Unit = {}) {
        if (!ready || manager == null) {
            onDone()
            return
        }
        stop()
        val resolved = GlyphAnimations.resolve(style)
        val maxLevel = (GlyphAnimations.MAX * brightnessPercent / 100f).toInt()
            .coerceIn(80, GlyphAnimations.MAX)
        val start = SystemClock.uptimeMillis()

        val runnable = object : Runnable {
            override fun run() {
                val t = (SystemClock.uptimeMillis() - start).toInt()
                if (t >= resolved.durationMs) {
                    off()
                    player = null
                    onDone()
                    return
                }
                show(GlyphAnimations.render(resolved, t, maxLevel))
                handler.postDelayed(this, FRAME_MS)
            }
        }
        player = runnable
        handler.post(runnable)
    }

    fun stop() {
        player?.let { handler.removeCallbacks(it) }
        player = null
        off()
    }

    fun release() {
        stop()
        try {
            manager?.closeSession()
        } catch (_: Throwable) {
        }
        try {
            manager?.unInit()
        } catch (_: Throwable) {
        }
        ready = false
        manager = null
    }

    private fun show(levels: IntArray) {
        val gm = manager ?: return
        try {
            val builder = gm.getGlyphFrameBuilder()
            var any = false
            for (ch in levels.indices) {
                if (levels[ch] > 0) {
                    builder.buildChannel(ch, levels[ch])
                    any = true
                }
            }
            if (any) gm.toggle(builder.build()) else gm.turnOff()
        } catch (e: Throwable) {
            Log.w(TAG, "Ошибка кадра Glyph", e)
        }
    }

    private fun off() {
        try {
            manager?.turnOff()
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val TAG = "GlyphController"
        private const val FRAME_MS = 40L
    }
}
