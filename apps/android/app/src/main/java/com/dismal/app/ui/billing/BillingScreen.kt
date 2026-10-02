package com.dismal.app.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dismal.app.core.ui.common.UiPalette
import com.dismal.app.domain.models.Invoice

@Composable
fun BillingScreen(viewModel: BillingViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateInvoiceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { clientId, softwareId, quantity, unitPrice, saleType, dueDate ->
                viewModel.createInvoiceFromSale(clientId, softwareId, quantity, unitPrice, saleType, dueDate)
                showCreateDialog = false
            },
        )
    }

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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Facturacion", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Control de facturas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { viewModel.refreshInvoices() },
                    enabled = !state.offlineMode,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Actualizar")
                }
                Button(
                    onClick = { showCreateDialog = true },
                    enabled = !state.offlineMode,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nueva factura")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nueva factura")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.offlineMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.accentSoft),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Modo offline activo",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.accent,
                    )
                    Text(
                        text = "No es posible crear o actualizar facturas sin conexion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        state.errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
            ) {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        state.noticeMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.accentSoft),
            ) {
                Text(
                    text = message,
                    color = palette.accent,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.invoices.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Sin facturas",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "Crea una nueva factura o sincroniza para ver datos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.invoices, key = { it.id }) { invoice ->
                        InvoiceCard(invoice = invoice)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceCard(invoice: Invoice) {
    val palette = UiPalette.current()
    val statusColor =
        when (invoice.status) {
            "PAID" -> palette.accent
            "CANCELLED" -> MaterialTheme.colorScheme.error
            else -> palette.textSecondary
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = invoice.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = invoice.clientName,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = invoice.clientEmail,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Emision: ${invoice.issueDate} · Vence: ${invoice.dueDate}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Total: $${String.format("%.2f", invoice.totalAmount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
            )
        }
    }
}

@Composable
private fun CreateInvoiceDialog(
    onDismiss: () -> Unit,
    onCreate: (clientId: String, softwareId: String, quantity: Int, unitPrice: Double, saleType: String, dueDate: String?) -> Unit,
) {
    var clientId by remember { mutableStateOf("") }
    var softwareId by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unitPrice by remember { mutableStateOf("") }
    var saleType by remember { mutableStateOf("CASH") }
    var dueDate by remember { mutableStateOf("") }
    val quantityValue = quantity.toIntOrNull()
    val unitPriceValue = unitPrice.toDoubleOrNull()
    val normalizedSaleType = saleType.trim().uppercase()
    val isSaleTypeValid = normalizedSaleType == "CASH" || normalizedSaleType == "CREDIT"
    val isQuantityValid = quantityValue != null && quantityValue > 0
    val isUnitPriceValid = unitPriceValue != null && unitPriceValue > 0.0
    val isFormValid =
        clientId.isNotBlank() &&
            softwareId.isNotBlank() &&
            isSaleTypeValid &&
            isQuantityValid &&
            isUnitPriceValid

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = UiPalette.current().surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Nueva factura", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it },
                    label = { Text("ID cliente") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = softwareId,
                    onValueChange = { softwareId = it },
                    label = { Text("ID software") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = saleType,
                    onValueChange = { saleType = it },
                    label = { Text("Tipo (CASH o CREDIT)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") },
                    isError = quantity.isNotBlank() && !isQuantityValid,
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Precio unitario") },
                    isError = unitPrice.isNotBlank() && !isUnitPriceValid,
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Fecha vencimiento (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!isSaleTypeValid || (quantity.isNotBlank() && !isQuantityValid) || (unitPrice.isNotBlank() && !isUnitPriceValid)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Revisa el tipo, cantidad y precio.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onCreate(
                                clientId,
                                softwareId,
                                quantityValue ?: 0,
                                unitPriceValue ?: 0.0,
                                normalizedSaleType,
                                dueDate.takeIf { it.isNotBlank() },
                            )
                        },
                        enabled = isFormValid,
                    ) {
                        Text("Crear")
                    }
                }
            }
        }
    }
}
