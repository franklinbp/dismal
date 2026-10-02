package com.dismal.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dismal.app.core.ui.common.UiPalette

@Composable
fun PlaceholderScreen(title: String) {
    val palette = UiPalette.current()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = palette.textPrimary,
        )
        Text(
            text = "Modulo no implementado aun",
            color = palette.textSecondary,
        )
    }
}
