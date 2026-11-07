package dev.nothing.tapglyph

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.reflect.Proxy

/**
 * Thin reflection-based bridge to Nothing's optional Ketchum SDK.
 * The app can compile without the proprietary SDK JAR; GitHub Actions adds it
 * before packaging the APK. The actual class names and API follow Nothing's
 * public Glyph Developer Kit documentation.
 */
class GlyphController(
    private val context: Context,
    private val onStatus: (String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var manager: Any? = null
    private var managerClass: Class<*>? = null
    private var callbackProxy: Any? = null
    private var serviceConnected = false
    private var sessionOpen = false
    private var foreground = false
    private var pendingAnimation = false
    private var animationGeneration = 0

    fun initialize() {
        try {
            managerClass = Class.forName("com.nothing.ketchum.GlyphManager")
            val cls = managerClass!!
            manager = cls.getMethod("getInstance", Context::class.java)
                .invoke(null, context.applicationContext)

            val callbackClass = Class.forName("com.nothing.ketchum.GlyphManager\$Callback")
            callbackProxy = Proxy.newProxyInstance(
                callbackClass.classLoader,
                arrayOf(callbackClass),
            ) { _, method, _ ->
                when (method.name) {
                    "onServiceConnected" -> mainHandler.post { onServiceConnected() }
                    "onServiceDisconnected" -> mainHandler.post {
                        serviceConnected = false
                        sessionOpen = false
                        onStatus("Glyph-сервис отключился. Откройте приложение ещё раз.")
                    }
                }
                null
            }

            cls.getMethod("init", callbackClass).invoke(manager, callbackProxy)
            onStatus("Подключаемся к Glyph-сервису Nothing…")
        } catch (e: ClassNotFoundException) {
            onStatus("SDK Glyph не найден. Он будет добавлен при сборке через GitHub Actions.")
        } catch (e: Exception) {
            onStatus("Не удалось запустить Glyph SDK: ${rootMessage(e)}")
        }
    }

    fun onForeground() {
        foreground = true
        if (serviceConnected) openSession()
    }

    fun onBackground() {
        foreground = false
        animationGeneration++
        if (sessionOpen) {
            safely { managerClass?.getMethod("turnOff")?.invoke(manager) }
            safely { managerClass?.getMethod("closeSession")?.invoke(manager) }
            sessionOpen = false
        }
    }

    fun playPaymentAnimation() {
        if (!foreground) {
            pendingAnimation = true
            onStatus("Анимация запустится, когда TapGlyph будет на экране.")
            return
        }
        if (!serviceConnected || !sessionOpen) {
            pendingAnimation = true
            onStatus("Glyph ещё подключается. Анимация запустится сразу после подключения.")
            return
        }

        pendingAnimation = false
        val currentRun = ++animationGeneration
        onStatus("Оплата обнаружена — запускаем Glyph-анимацию ✦")

        // Phone (2): 33 addressable zones, indices 0..32.
        // The effect sweeps from the upper/central zones into the C/D segments,
        // then finishes with a soft full-interface pulse.
        val patterns = listOf(
            Triple(listOf(0, 1, 2, 24), 260, 1),
            Triple((3..18).toList(), 360, 1),
            Triple((19..23).toList() + (25..32).toList(), 420, 1),
            Triple((0..32).toList(), 780, 2),
        )
        val offsets = listOf(0L, 420L, 850L, 1_300L)

        patterns.forEachIndexed { index, item ->
            mainHandler.postDelayed({
                if (currentRun == animationGeneration && foreground && sessionOpen) {
                    animateChannels(item.first, item.second, item.third)
                }
            }, offsets[index])
        }

        mainHandler.postDelayed({
            if (currentRun == animationGeneration && foreground && sessionOpen) {
                safely { managerClass?.getMethod("turnOff")?.invoke(manager) }
                onStatus("Готово. Можно протестировать ещё раз.")
            }
        }, 3_600L)
    }

    fun release() {
        onBackground()
        try {
            managerClass?.getMethod("unInit")?.invoke(manager)
        } catch (_: Exception) {
            // Nothing's service may already have disconnected.
        }
        manager = null
        callbackProxy = null
    }

    private fun onServiceConnected() {
        serviceConnected = true
        try {
            val glyphClass = Class.forName("com.nothing.ketchum.Glyph")
            val device = try {
                glyphClass.getField("DEVICE_22111").get(null) as String
            } catch (_: Exception) {
                "22111"
            }

            val registerWithDevice = managerClass?.methods?.firstOrNull {
                it.name == "register" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            }
            val registered = if (registerWithDevice != null) {
                registerWithDevice.invoke(manager, device) as? Boolean ?: false
            } else {
                managerClass?.getMethod("register")?.invoke(manager) as? Boolean ?: false
            }

            if (!registered) {
                onStatus("Glyph не зарегистрирован. Проверьте ключ и режим отладки Nothing.")
                return
            }

            onStatus("Glyph-сервис подключён. Готово к анимации.")
            if (foreground) openSession()
        } catch (e: Exception) {
            onStatus("Ошибка регистрации Glyph: ${rootMessage(e)}")
        }
    }

    private fun openSession() {
        if (sessionOpen) return
        try {
            managerClass?.getMethod("openSession")?.invoke(manager)
            sessionOpen = true
            onStatus("Glyph готов. Нажмите «Тест анимации».")
            if (pendingAnimation) playPaymentAnimation()
        } catch (e: Exception) {
            onStatus("Не удалось открыть Glyph-сессию: ${rootMessage(e)}")
        }
    }

    private fun animateChannels(channels: List<Int>, periodMs: Int, cycles: Int) {
        try {
            val glyphFrameClass = Class.forName("com.nothing.ketchum.GlyphFrame")
            var builder = managerClass?.getMethod("getGlyphFrameBuilder")?.invoke(manager)
                ?: return

            val buildChannel = builder.javaClass.getMethod("buildChannel", Int::class.javaPrimitiveType!!)
            channels.forEach { channel ->
                builder = buildChannel.invoke(builder, channel) ?: builder
            }

            builder = builder.javaClass.getMethod("buildPeriod", Int::class.javaPrimitiveType!!)
                .invoke(builder, periodMs) ?: builder
            builder = builder.javaClass.getMethod("buildCycles", Int::class.javaPrimitiveType!!)
                .invoke(builder, cycles) ?: builder
            builder = builder.javaClass.getMethod("buildInterval", Int::class.javaPrimitiveType!!)
                .invoke(builder, 80) ?: builder

            val frame = builder.javaClass.getMethod("build").invoke(builder)
            managerClass?.getMethod("animate", glyphFrameClass)?.invoke(manager, frame)
        } catch (e: Exception) {
            onStatus("Ошибка анимации Glyph: ${rootMessage(e)}")
        }
    }

    private fun safely(action: () -> Unit) {
        try {
            action()
        } catch (_: Exception) {
            // A closed session is an expected lifecycle condition.
        }
    }

    private fun rootMessage(error: Throwable): String {
        var current = error
        while (current.cause != null) current = current.cause!!
        return current.message ?: current.javaClass.simpleName
    }
}
