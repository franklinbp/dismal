package com.dismal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dismal.app.core.ui.common.UiPalette

private fun buildColorScheme(darkTheme: Boolean) =
    run {
        val palette = UiPalette.palette(darkTheme)
        if (darkTheme) {
            darkColorScheme(
                primary = palette.accent,
                secondary = palette.accentSoft,
                tertiary = palette.border,
                background = palette.background,
                surface = palette.surface,
                onPrimary = palette.background,
                onSecondary = palette.textPrimary,
                onTertiary = palette.textPrimary,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
            )
        } else {
            lightColorScheme(
                primary = palette.accent,
                secondary = palette.accentSoft,
                tertiary = palette.border,
                background = palette.background,
                surface = palette.surface,
                onPrimary = palette.surface,
                onSecondary = palette.textPrimary,
                onTertiary = palette.textPrimary,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
            )
        }
    }

@Composable
fun DismalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            else -> buildColorScheme(darkTheme)
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
