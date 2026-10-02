package com.dismal.app.ui.reports

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dismal.app.core.ui.common.UiPalette
import com.dismal.app.domain.models.DashboardSummary
import com.dismal.app.domain.models.DashboardTarget
import com.dismal.app.domain.models.SalesTargetSummary
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: ReportsViewModel) {
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
                    Text(text = "Reportes ejecutivos", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "Prioridades diarias del negocio basadas en Dismal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
                Button(onClick = viewModel::loadReports, enabled = !state.isLoading) {
                    Text(if (state.isLoading) "Actualizando..." else "Actualizar")
                }
            }
        }

        state.errorMessage?.let { message ->
            item {
                InfoCard("Error", listOf(message), palette)
            }
        }

        state.summary?.let { summary ->
            item {
                SummaryCard(summary = summary, palette = palette)
            }
        }

        state.targetSummary?.let { summary ->
            item {
                TargetSummaryCard(summary = summary, palette = palette)
            }
        }

        item {
            InfoCard("Que deberias hacer hoy", state.priorities, palette)
        }

        if (state.topTargets.isNotEmpty()) {
            item {
                Text("Metas para seguimiento", style = MaterialTheme.typography.titleLarge)
            }
            items(state.topTargets, key = { it.id }) { target ->
                TargetCard(target = target, palette = palette)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: DashboardSummary,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estado del negocio", style = MaterialTheme.typography.titleMedium)
            SummaryRow("Ventas hoy", "${summary.todaySalesCount} / ${formatMoney(summary.todaySalesAmount)}")
            SummaryRow("Ventas del mes", "${summary.monthSalesCount} / ${formatMoney(summary.monthSalesAmount)}")
            SummaryRow("Utilidad esperada", formatMoney(summary.monthExpectedProfit))
            SummaryRow("Cartera vencida", formatMoney(summary.overdueArBalance))
            SummaryRow("Cartera abierta", formatMoney(summary.openArBalance))
            SummaryRow("Outbox con fallas", summary.outboxFailedCount.toString())
        }
    }
}

@Composable
private fun TargetSummaryCard(
    summary: SalesTargetSummary,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Meta comercial", style = MaterialTheme.typography.titleMedium)
            SummaryRow("Metas activas", summary.totalTargets.toString())
            SummaryRow("Cumplidas", summary.achievedTargets.toString())
            SummaryRow("Pendientes", summary.pendingTargets.toString())
            SummaryRow("Unidades meta", summary.totalMetaUnits.toString())
            SummaryRow("Ingreso objetivo", formatMoney(summary.totalTargetRevenue))
            SummaryRow("Utilidad proyectada", formatMoney(summary.totalExpectedProfit))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    items: List<String>,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            items.forEach { item ->
                Text(item, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
        }
    }
}

@Composable
private fun TargetCard(
    target: DashboardTarget,
    palette: com.dismal.app.core.ui.common.UiColors,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(target.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (target.targetAchieved) "Cumplida" else "Pendiente",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.accent,
                )
            }
            Text("${target.unitsSoldCurrent}/${target.metaUnits} unidades", style = MaterialTheme.typography.bodyMedium)
            Text("Utilidad esperada ${formatMoney(target.expectedProfit)}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            Text("Break-even ${target.breakEvenUnits} | vence ${target.deadline}", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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

private fun formatMoney(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)
