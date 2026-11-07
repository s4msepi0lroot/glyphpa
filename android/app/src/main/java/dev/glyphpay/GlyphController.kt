package dev.glyphpay

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphManager

/**
 * Обёртка над Glyph SDK: подключается к системному сервису, открывает сессию
 * и проигрывает [GlyphAnimation] кадр за кадром через setFrameColors().
 *
 * ВАЖНО: Glyph SDK работает только пока приложение на переднем плане.
 */
class GlyphController(context: Context) {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())

    private var manager: GlyphManager? = null
    private var sessionOpen = false
    private var connecting = false
    private var released = false
    private var loop: Runnable? = null
    private var timeout: Runnable? = null
    private val waiting = ArrayList<(String?) -> Unit>()

    /**
     * Проиграть анимацию. [onDone] вызывается с null при успехе
     * или с текстом ошибки.
     */
    fun play(animation: GlyphAnimation, brightnessPercent: Int, onDone: (String?) -> Unit) {
        stopLoop()
        ensureSession { error ->
            if (error != null || released) {
                onDone(error)
                return@ensureSession
            }
            startLoop(animation, brightnessPercent, onDone)
        }
    }

    private fun startLoop(animation: GlyphAnimation, brightnessPercent: Int, onDone: (String?) -> Unit) {
        val start = SystemClock.uptimeMillis()
        val buffer = FloatArray(Glyphs.COUNT)
        val gain = brightnessPercent.coerceIn(5, 100) / 100f

        val step = object : Runnable {
            override fun run() {
                if (released) return
                val t = (SystemClock.uptimeMillis() - start).toInt()
                if (t >= animation.durationMs) {
                    lightsOff()
                    loop = null
                    onDone(null)
                    return
                }
                buffer.fill(0f)
                animation.render(t, buffer)
                send(buffer, gain)
                handler.postDelayed(this, FRAME_MS)
            }
        }
        loop = step
        handler.post(step)
    }

    private fun send(buffer: FloatArray, gain: Float) {
        val colors = IntArray(Glyphs.COUNT)
        for (i in colors.indices) {
            val v = buffer[i].coerceIn(0f, 1f)
            // Гамма 2.0: линейные ШИМ-светодиоды выглядят плавнее
            colors[i] = (v * v * gain * MAX_LEVEL).toInt()
        }
        try {
            manager?.setFrameColors(colors)
        } catch (t: Throwable) {
            Log.e(TAG, "setFrameColors failed", t)
        }
    }

    private fun lightsOff() {
        try {
            manager?.setFrameColors(IntArray(Glyphs.COUNT))
        } catch (t: Throwable) {
            Log.e(TAG, "lightsOff failed", t)
        }
    }

    private fun stopLoop() {
        loop?.let { handler.removeCallbacks(it) }
        loop = null
    }

    private fun ensureSession(callback: (String?) -> Unit) {
        if (sessionOpen) {
            callback(null)
            return
        }
        waiting.add(callback)
        if (connecting) return
        connecting = true

        val gm = try {
            GlyphManager.getInstance(appContext)
        } catch (t: Throwable) {
            finishConnect("Не удалось получить GlyphManager: ${t.message}")
            return
        }
        manager = gm

        val serviceCallback = object : GlyphManager.Callback {
            override fun onServiceConnected(name: ComponentName?) {
                timeout?.let { handler.removeCallbacks(it) }
                try {
                    val registered = gm.register(Glyph.DEVICE_22111)
                    if (!registered) {
                        finishConnect(ERR_REGISTER)
                        return
                    }
                    gm.openSession()
                    sessionOpen = true
                    finishConnect(null)
                } catch (t: Throwable) {
                    Log.e(TAG, "openSession failed", t)
                    finishConnect("$ERR_REGISTER (${t.message})")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                sessionOpen = false
            }
        }

        val timer = Runnable {
            finishConnect("Glyph-сервис не отвечает. Это точно Nothing Phone (2) с Nothing OS?")
        }
        timeout = timer
        handler.postDelayed(timer, CONNECT_TIMEOUT_MS)

        try {
            gm.init(serviceCallback)
        } catch (t: Throwable) {
            handler.removeCallbacks(timer)
            finishConnect("Ошибка подключения к Glyph: ${t.message}")
        }
    }

    private fun finishConnect(error: String?) {
        connecting = false
        timeout?.let { handler.removeCallbacks(it) }
        val list = ArrayList(waiting)
        waiting.clear()
        list.forEach { it(error) }
    }

    /** Погасить Glyph и освободить сервис. Обязательно вызывать в onPause/onDestroy. */
    fun release() {
        if (released) return
        stopLoop()
        lightsOff()
        released = true
        timeout?.let { handler.removeCallbacks(it) }
        try {
            if (sessionOpen) manager?.closeSession()
        } catch (t: Throwable) {
            Log.e(TAG, "closeSession failed", t)
        }
        try {
            manager?.unInit()
        } catch (t: Throwable) {
            Log.e(TAG, "unInit failed", t)
        }
        sessionOpen = false
        manager = null
    }

    companion object {
        private const val TAG = "GlyphPay"
        private const val FRAME_MS = 30L
        private const val MAX_LEVEL = 4095f
        private const val CONNECT_TIMEOUT_MS = 4000L
        const val ERR_REGISTER =
            "Glyph отклонил приложение. Включите отладку Glyph командой: " +
                "adb shell settings put global nt_glyph_interface_debug_enable 1"
    }
}
