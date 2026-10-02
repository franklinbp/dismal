package com.dismal.app.core.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class UiColors(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
)

object UiPalette {
    private val light =
        UiColors(
            background = Color(0xFFF5F8FD),
            surface = Color(0xFFFFFFFF),
            accent = Color(0xFF1DB4E7),
            accentSoft = Color(0xFFE8F4FC),
            textPrimary = Color(0xFF1A2547),
            textSecondary = Color(0xFF61789A),
            border = Color(0xFFD8E4F0),
        )

    private val dark =
        UiColors(
            background = Color(0xFF0F1730),
            surface = Color(0xFF14203F),
            accent = Color(0xFF4CC7EF),
            accentSoft = Color(0xFF1A2749),
            textPrimary = Color(0xFFEDF2FB),
            textSecondary = Color(0xFFA9B7D0),
            border = Color(0xFF2C3C63),
        )

    @Composable
    fun current(): UiColors {
        return palette(isSystemInDarkTheme())
    }

    fun palette(darkTheme: Boolean): UiColors {
        return if (darkTheme) dark else light
    }
}
