package dev.glyphpay

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Glyph SDK работает только из foreground-приложения, поэтому для анимации «из фона»
 * мы показываем невидимую прозрачную activity (нужно разрешение «Поверх других окон»).
 */
object GlyphLauncher {

    const val EXTRA_ANIMATION = "animation"

    @Volatile
    var lastLaunch = 0L

    /** @return false, если запуск невозможен (нет разрешения) или слишком частый. */
    fun launch(context: Context, animationId: String? = null): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        val now = System.currentTimeMillis()
        if (now - lastLaunch < 3500) return false
        lastLaunch = now

        val intent = Intent(context, GlyphActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_HISTORY,
            )
            if (animationId != null) putExtra(EXTRA_ANIMATION, animationId)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            false
        }
    }
}
