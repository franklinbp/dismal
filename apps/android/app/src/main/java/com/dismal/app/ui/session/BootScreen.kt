package com.dismal.app.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dismal.app.core.ui.common.UiPalette

@Composable
fun BootScreen() {
    val palette = UiPalette.current()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Dismal",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }
    }
}
