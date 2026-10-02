package com.dismal.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dismal.app.core.ui.common.UiPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Configuracion", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = viewModel::refresh) {
                Text("Actualizar")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Conexion", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                SummaryRow(label = "API Base", value = state.apiBaseUrl)
                SummaryRow(label = "Sesion", value = if (state.hasToken) "Activa" else "Inactiva")
                SummaryRow(label = "Modo offline", value = if (state.offlineMode) "Activo" else "Desactivado")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatLastSync(state.lastSyncAt, state.lastSyncStatus, state.lastSyncMessage),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        if (state.offlineMode) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = viewModel::disableOfflineMode, modifier = Modifier.fillMaxWidth()) {
                Text("Salir de modo offline")
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatLastSync(timestamp: Long, status: String?, message: String?): String {
    if (timestamp <= 0L) return "Sin datos de sincronizacion"
    val formatted = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    val statusText = status ?: "SIN_ESTADO"
    val messageText = message ?: ""
    return "Ultima sync: $formatted · $statusText ${if (messageText.isNotBlank()) "· $messageText" else ""}"
}
