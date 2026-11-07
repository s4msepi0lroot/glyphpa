package com.glyphpay.nfc

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Процедурные анимации для Glyph Interface Nothing Phone (2).
 *
 * Индексы каналов Phone (2):
 *   A1=0, A2=1, B1=2, C1_1..C1_16=3..18, C2..C6=19..23, E1=24, D1_1..D1_8=25..32
 *
 * Каждая анимация — функция времени: render(style, t) -> яркость (0..maxLevel) для 33 каналов.
 */
object GlyphAnimations {
    const val CHANNELS = 33
    const val MAX = 4095

    /** Порядок обхода всех сегментов «по пути»: камера -> кольцо -> USB-линия -> точка. */
    private val PATH: IntArray = buildList {
        add(0); add(1); add(2)
        for (i in 18 downTo 3) add(i)   // C1_16 .. C1_1
        for (i in 19..23) add(i)        // C2 .. C6
        for (i in 32 downTo 25) add(i)  // D1_8 .. D1_1
        add(24)                         // E1
    }.toIntArray()

    enum class Style(val title: String, val durationMs: Int) {
        PAY("Оплата прошла: волны + галочка", 3700),
        COMET("Комета", 2800),
        BREATH("Дыхание", 3600),
        SPARKLE("Искры", 3200),
        RANDOM("Случайная", 0);
    }

    fun resolve(style: Style): Style =
        if (style == Style.RANDOM) Style.entries.filter { it != Style.RANDOM }.random() else style

    fun render(style: Style, t: Int, maxLevel: Int = MAX): IntArray {
        val out = IntArray(CHANNELS)
        val n = PATH.size
        for (i in 0 until n) {
            val pos = i / (n - 1f)
            val v = when (style) {
                Style.PAY -> pay(pos, t)
                Style.COMET -> comet(pos, t)
                Style.BREATH -> breath(pos, t)
                Style.SPARKLE -> sparkle(i, t)
                Style.RANDOM -> 0f
            }
            // гамма-коррекция, чтобы затухание выглядело плавнее
            val level = v.coerceIn(0f, 1f).pow(1.8f)
            out[PATH[i]] = (level * maxLevel).toInt()
        }
        return out
    }

    private fun gauss(x: Float, w: Float) = exp(-(x * x) / (w * w))

    /** Две волны от камеры (где NFC-антенна) -> заливка всего -> двойная вспышка -> угасание. */
    private fun pay(pos: Float, t: Int): Float {
        var v = 0f
        if (t < 1900) {
            for (k in 0..1) {
                val local = t - k * 750
                if (local < 0) continue
                val head = local / 800f * 1.3f - 0.1f
                val peak = if (k == 0) 1f else 0.75f
                v = max(v, peak * gauss(pos - head, 0.11f))
            }
        }
        if (t in 1700..2399) {
            val f = (t - 1700) / 600f * 1.25f
            v = max(v, ((f - pos) / 0.25f).coerceIn(0f, 1f))
        }
        if (t in 2400..2899) {
            val x = (t - 2400) / 500f
            v = max(v, 0.65f + 0.35f * cos(x * PI.toFloat() * 4f))
        }
        if (t in 2900..3699) {
            val f = 1f - (t - 2900) / 800f
            v = max(v, (f * 1.3f - pos * 0.3f).coerceIn(0f, 1f))
        }
        return v
    }

    /** Яркая комета с хвостом, затем вторая — в обратную сторону. */
    private fun comet(pos: Float, t: Int): Float {
        fun tail(d: Float) = if (d < 0f) gauss(d, 0.03f) else exp(-d / 0.14f)
        val h1 = t / 1500f * 1.5f - 0.2f
        var v = tail(h1 - pos)
        val t2 = t - 900
        if (t2 >= 0) {
            val h2 = t2 / 1500f * 1.5f - 0.2f
            v = max(v, tail(h2 - (1f - pos)))
        }
        return if (v < 0.02f) 0f else v
    }

    /** Два «вдоха» с бегущим фазовым сдвигом. */
    private fun breath(pos: Float, t: Int): Float {
        val x = t / 1500f - pos * 0.35f
        if (x < 0f || x > 2f) return 0f
        val s = sin(PI.toFloat() * x)
        return s * s
    }

    /** Случайные мерцающие искры (детерминированно). */
    private fun sparkle(idx: Int, t: Int): Float {
        var v = 0f
        val starts = intArrayOf((idx * 7919) % 2000, 800 + (idx * 6007) % 1600)
        for (s in starts) {
            val x = (t - s) / 700f
            if (x in 0f..1f) {
                val rise = (x * 8f).coerceAtMost(1f)
                v = max(v, rise * (1f - x) * (1f - x))
            }
        }
        return v
    }
}
