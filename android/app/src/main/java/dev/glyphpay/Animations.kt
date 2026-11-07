package dev.glyphpay

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Раскладка Glyph у Nothing Phone (2) (индексы из официальной документации SDK).
 *
 *  A1, A2      – возле камеры
 *  B1          – диагональ
 *  C1_1..C1_16 – длинная линия по периметру (C1_1 внизу справа, C1_16 вверху слева)
 *  C2..C6      – кольцо вокруг беспроводной зарядки
 *  D1_1..D1_8  – вертикальная линия справа (D1_1 снизу, D1_8 сверху)
 *  E1          – точка
 */
object Glyphs {
    const val COUNT = 33
    const val A1 = 0
    const val A2 = 1
    const val B1 = 2
    const val E1 = 24
    val C1 = IntArray(16) { 3 + it }
    val RING = IntArray(5) { 19 + it }
    val D1 = IntArray(8) { 25 + it }
}

/** Одна анимация: [render] заполняет массив яркостей 0..1 для момента времени t (мс). */
class GlyphAnimation(
    val id: String,
    val title: String,
    val description: String,
    val durationMs: Int,
    val render: (t: Int, out: FloatArray) -> Unit,
)

object Animations {
    const val DEFAULT_ID = "wave"

    private const val TWO_PI = (2.0 * PI).toFloat()

    private fun clamp01(x: Float) = if (x < 0f) 0f else if (x > 1f) 1f else x

    /** Треугольный импульс: 0 -> 1 -> 0 на отрезке x∈[0,1]. */
    private fun tri(x: Float) = if (x < 0f || x > 1f) 0f else 1f - abs(2f * x - 1f)

    private fun FloatArray.lit(i: Int, v: Float) {
        if (v > this[i]) this[i] = v
    }

    private fun FloatArray.litAll(v: Float) {
        for (i in indices) lit(i, v)
    }

    private fun FloatArray.scale(f: Float) {
        for (i in indices) this[i] *= f
    }

    /** Комета на кольцевой линии из n сегментов. */
    private fun ringComet(pos: Float, k: Int, n: Int, tail: Float): Float {
        val d = (((pos - k) % n) + n) % n
        return if (d < tail) 1f - d / tail else 0f
    }

    // ───────────────────────────── 1. Волна оплаты ─────────────────────────────
    private val wave = GlyphAnimation(
        id = "wave",
        title = "Волна оплаты",
        description = "Касание → волна по кольцу → шкала заполняется → двойная вспышка успеха",
        durationMs = 2900,
    ) { t, o ->
        val tf = t.toFloat()
        if (t < 1900) {
            // «Касание»: два удара пульса на точке
            if (t < 1000) {
                o.lit(Glyphs.E1, 0.5f - 0.5f * cos(TWO_PI * tf / 500f))
            } else {
                o.lit(Glyphs.E1, 0.35f)
            }
            // Рябь по кольцу вокруг зарядки
            for (i in Glyphs.RING.indices) {
                o.lit(Glyphs.RING[i], tri((tf - (250f + i * 140f)) / 450f))
            }
            // Искры у камеры
            o.lit(Glyphs.A1, tri((tf - 500f) / 400f))
            o.lit(Glyphs.A2, tri((tf - 600f) / 400f))
            o.lit(Glyphs.B1, tri((tf - 700f) / 400f))
            if (t >= 900) {
                // Шкала снизу вверх
                val n = (tf - 900f) / 1000f * 8f
                for (i in Glyphs.D1.indices) o.lit(Glyphs.D1[i], clamp01(n - i))
                // Комета по периметру
                val head = (tf - 900f) / 1000f * 21f
                for (k in Glyphs.C1.indices) {
                    val d = head - k
                    if (d >= 0f && d < 5f) o.lit(Glyphs.C1[k], 1f - d / 5f)
                }
            }
        } else {
            // Вспышка успеха: полтора колебания с затуханием
            val u = (tf - 1900f) / 1000f
            val s = (0.5f + 0.5f * cos(3f * PI.toFloat() * u)) * (1f - u * 0.5f)
            o.litAll(clamp01(s) * clamp01((2900f - tf) / 400f))
        }
    }

    // ───────────────────────────── 2. Комета ─────────────────────────────
    private val comet = GlyphAnimation(
        id = "comet",
        title = "Комета",
        description = "Светящаяся комета бегает по периметру, шкала и кольцо подхватывают",
        durationMs = 3200,
    ) { t, o ->
        val tf = t.toFloat()
        val head = (tf % 1600f) / 1600f * 16f
        for (k in Glyphs.C1.indices) o.lit(Glyphs.C1[k], ringComet(head, k, 16, 6f))

        // D1: сверху вниз, вдвое быстрее
        val headD = (tf % 800f) / 800f * 8f
        for (i in Glyphs.D1.indices) {
            val fromTop = 7 - i
            o.lit(Glyphs.D1[i], ringComet(headD, fromTop, 8, 4f))
        }

        // Кольцо: «бегущий огонёк»
        for (i in Glyphs.RING.indices) {
            val d = (((tf / 200f - i) % 5f) + 5f) % 5f
            o.lit(Glyphs.RING[i], clamp01(1f - d / 1.5f))
        }

        // Камера мигает попеременно
        val blink = 0.5f - 0.5f * cos(TWO_PI * tf / 400f)
        o.lit(Glyphs.A1, blink)
        o.lit(Glyphs.A2, 1f - blink)
        o.lit(Glyphs.B1, 0.4f)

        // Плавное появление и затухание
        o.scale(clamp01(tf / 250f) * clamp01((3200f - tf) / 500f))
    }

    // ───────────────────────────── 3. Заряд ─────────────────────────────
    private val fill = GlyphAnimation(
        id = "fill",
        title = "Заряд",
        description = "Обе линии заполняются как индикатор зарядки, затем три вспышки",
        durationMs = 2800,
    ) { t, o ->
        val tf = t.toFloat()
        if (t < 1500) {
            val n = clamp01(tf / 1200f)
            for (k in Glyphs.C1.indices) o.lit(Glyphs.C1[k], clamp01(n * 16f - k))
            for (i in Glyphs.D1.indices) o.lit(Glyphs.D1[i], clamp01(n * 8f - i))
            for (i in Glyphs.RING.indices) o.lit(Glyphs.RING[i], clamp01((tf - 1000f - i * 70f) / 120f))
            o.lit(Glyphs.E1, clamp01((tf - 400f) / 200f))
            o.lit(Glyphs.A1, clamp01((tf - 1100f) / 150f))
            o.lit(Glyphs.A2, clamp01((tf - 1200f) / 150f))
            o.lit(Glyphs.B1, clamp01((tf - 1300f) / 150f))
        } else {
            val u = (tf - 1500f) / 1300f
            val s = (0.5f + 0.5f * cos(TWO_PI * 3f * u)) * (1f - u)
            o.litAll(clamp01(s))
        }
    }

    // ───────────────────────────── 4. Искры ─────────────────────────────
    private val sparks = GlyphAnimation(
        id = "sparks",
        title = "Искры",
        description = "Случайные яркие вспышки по всему Glyph, как фейерверк",
        durationMs = 2800,
    ) { t, o ->
        val tf = t.toFloat()
        for (i in 0 until Glyphs.COUNT) {
            for (k in 0 until 3) {
                val h = ((i * 7919 + k * 104729 + 13) % 997) / 997f
                val start = h * 2100f
                if (tf >= start) o.lit(i, exp(-(tf - start) / 170f))
            }
        }
    }

    // ───────────────────────────── 5. Дыхание ─────────────────────────────
    private val breath = GlyphAnimation(
        id = "breath",
        title = "Дыхание",
        description = "Мягкое «дыхание» всей подсветки с бегущей фазой — спокойный вариант",
        durationMs = 3200,
    ) { t, o ->
        val tf = t.toFloat()
        fun level(offset: Float) = 0.5f - 0.5f * cos(TWO_PI * (tf / 1600f - offset))
        o.lit(Glyphs.A1, level(0f))
        o.lit(Glyphs.A2, level(0.03f))
        o.lit(Glyphs.B1, level(0.06f))
        for (k in Glyphs.C1.indices) o.lit(Glyphs.C1[k], level(0.02f + k * 0.006f))
        for (i in Glyphs.RING.indices) o.lit(Glyphs.RING[i], level(0.1f + i * 0.02f))
        o.lit(Glyphs.E1, level(0.2f))
        for (i in Glyphs.D1.indices) o.lit(Glyphs.D1[i], level(0.12f + i * 0.01f))
        o.scale(clamp01(tf / 400f) * clamp01((3200f - tf) / 300f))
    }

    val all: List<GlyphAnimation> = listOf(wave, comet, fill, sparks, breath)

    fun byId(id: String?): GlyphAnimation = all.firstOrNull { it.id == id } ?: wave
}
