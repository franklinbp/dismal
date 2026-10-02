package com.dismal.app.ui.marketing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dismal.app.core.ui.common.UiPalette
import com.dismal.app.domain.models.MarketingAnalysis
import com.dismal.app.domain.models.StrategyAction

@Composable
fun MarketingScreen(viewModel: MarketingViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(text = "Estrategia comercial", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Recomendaciones reales del motor de inteligencia de Dismal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
                Button(onClick = viewModel::loadStatus, enabled = !state.isLoading) {
                    Text(if (state.isLoading) "Actualizando..." else "Actualizar")
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                StatusCard("Error", message, palette)
            }
        }

        state.actionMessage?.let { message ->
            item {
                StatusCard("Acciones", message, palette)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "Lectura de hoy", style = MaterialTheme.typography.titleMedium)
                    SummaryRow(label = "Prioridad alta", value = state.highPriorityCount.toString())
                    SummaryRow(label = "Alerta comercial", value = state.commercialAlertCount.toString())
                    SummaryRow(label = "Baja traccion", value = state.lowTractionCount.toString())
                    SummaryRow(label = "Pausados", value = state.pausedCount.toString())
                    SummaryRow(label = "Acciones abiertas", value = state.openActions.size.toString())
                }
            }
        }

        if (state.openActions.isNotEmpty()) {
            item {
                Text("Acciones estrategicas", style = MaterialTheme.typography.titleMedium)
            }
            items(state.openActions.sortedWith(compareByDescending<StrategyAction> { priorityScore(it.priority) }.thenBy { it.dueDate ?: "" })) { item ->
                StrategyActionCard(
                    item = item,
                    palette = palette,
                    isUpdating = state.updatingActionId == item.id,
                    onStart = { viewModel.updateActionStatus(item.id, "EN_PROGRESO") },
                    onDone = { viewModel.updateActionStatus(item.id, "HECHA") },
                )
            }
        }

        if (state.analysis.isEmpty() && !state.isLoading) {
            item {
                StatusCard(
                    "Sin recomendaciones",
                    "No hay productos analizados por ahora. Verifica que el backend tenga datos comerciales.",
                    palette,
                )
            }
        } else {
            items(state.analysis.sortedWith(compareByDescending<MarketingAnalysis> { priorityScore(it.priority) }.thenBy { it.productName })) { item ->
                MarketingInsightCard(item = item, palette = palette)
            }
        }
    }
}

@Composable
private fun StrategyActionCard(
    item: StrategyAction,
    palette: com.dismal.app.core.ui.common.UiColors,
    isUpdating: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.status, style = MaterialTheme.typography.labelMedium, color = palette.accent)
            }
            Text(
                "${item.source} / ${item.priority} / ${item.recommendedChannel ?: "Sin canal"}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
            Text(
                "Producto: ${item.productName ?: "Sin producto"} - Vence: ${item.dueDate ?: "Sin fecha"}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!item.status.equals("EN_PROGRESO", ignoreCase = true)) {
                    OutlinedButton(
                        onClick = onStart,
                        enabled = !isUpdating,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (isUpdating) "Guardando..." else "Iniciar")
                    }
                }
                Button(
                    onClick = onDone,
                    enabled = !isUpdating,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isUpdating) "Guardando..." else "Hecha")
                }
            }
        }
    }
}

@Composable
private fun MarketingInsightCard(
    item: MarketingAnalysis,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${item.state} / ${item.priority}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                )
            }
            Text(
                item.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            if (item.suggestedActions.isNotEmpty()) {
                Text("Que hacer hoy", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item.suggestedActions.forEach { action ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.background),
                        ) {
                            Text(
                                text = action,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
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

private fun priorityScore(priority: String): Int =
    when (priority.uppercase()) {
        "HIGH", "ALTA" -> 3
        "MEDIUM", "MEDIA" -> 2
        else -> 1
    }
