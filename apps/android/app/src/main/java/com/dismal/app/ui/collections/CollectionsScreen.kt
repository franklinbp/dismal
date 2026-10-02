package com.dismal.app.ui.collections

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dismal.app.core.ui.common.UiPalette
import com.dismal.app.domain.models.Customer

@Composable
fun CollectionsScreen(viewModel: CollectionsViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }

    val debtors = state.customers.filter { (it.saldoActual ?: 0.0) > 0.0 }

    if (showPaymentDialog && selectedCustomer != null) {
        RegisterPaymentDialog(
            customer = selectedCustomer!!,
            onDismiss = { showPaymentDialog = false },
            isSubmitting = state.isSubmitting,
            onRegister = { amount, method, reference ->
                viewModel.registerPayment(selectedCustomer!!.id, amount, method, reference)
                showPaymentDialog = false
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
        Text(text = "Cobros", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Registra pagos y revisa quienes deben",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        state.successMessage?.let { message ->
            Text(
                text = message,
                color = palette.accent,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (debtors.isEmpty()) {
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
                        text = "Sin deudas pendientes",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "No hay clientes con saldo pendiente.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(debtors, key = { it.id }) { customer ->
                    DebtorCard(
                        customer = customer,
                        onRegisterPayment = {
                            selectedCustomer = customer
                            showPaymentDialog = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtorCard(
    customer: Customer,
    onRegisterPayment: () -> Unit,
) {
    val palette = UiPalette.current()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = palette.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = customer.email,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Saldo pendiente: $${String.format("%.2f", customer.saldoActual ?: 0.0)}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.accent,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRegisterPayment) {
                Text("Registrar cobro")
            }
        }
    }
}

@Composable
private fun RegisterPaymentDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    isSubmitting: Boolean,
    onRegister: (Double, String, String?) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CASH") }
    var reference by remember { mutableStateOf("") }
    val amountValue = amount.toDoubleOrNull()
    val isValid = amountValue != null && amountValue > 0.0
    val methodValid = method in setOf("CASH", "CARD", "TRANSFER")
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = UiPalette.current().surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Registrar cobro", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = UiPalette.current().textSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it.uppercase() },
                    label = { Text("Metodo (CASH, CARD o TRANSFER)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Referencia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
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
                        onClick = { onRegister(amountValue ?: 0.0, method, reference) },
                        enabled = isValid && methodValid && !isSubmitting,
                    ) {
                        Text(if (isSubmitting) "Guardando..." else "Guardar")
                    }
                }
            }
        }
    }
}
