package com.dismal.app.ui.customers

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dismal.app.core.ui.common.UiPalette
import java.util.Locale
import kotlin.random.Random

@Composable
fun CustomersScreen(viewModel: CustomerViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()
    val context = LocalContext.current

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    val filteredCustomers =
        state.customers.filter { customer ->
            normalizedQuery.isBlank() ||
                customer.name.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                customer.email.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                (customer.phone?.lowercase(Locale.getDefault())?.contains(normalizedQuery) == true)
        }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            isSubmitting = state.isSubmitting,
            onDismiss = { showAddCustomerDialog = false },
            onAddCustomer = { name, email, phone, password ->
                viewModel.addCustomer(name, email, phone, password)
                showAddCustomerDialog = false
            },
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Clientes", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Alta y consulta de clientes desde administracion.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
                TextButton(onClick = viewModel::refreshCustomers, enabled = !state.isLoading) {
                    Text("Actualizar")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.errorMessage?.let {
                FeedbackCard(title = "Error", message = it, palette = palette, isError = true)
                Spacer(modifier = Modifier.height(10.dp))
            }
            state.successMessage?.let {
                FeedbackCard(title = "Listo", message = it, palette = palette, isError = false)
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar por nombre, email o telefono") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                filteredCustomers.isEmpty() -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = palette.surface),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Sin clientes", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Crea un cliente nuevo o ajusta la busqueda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredCustomers, key = { it.id }) { customer ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.surface),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .background(palette.accentSoft, RoundedCornerShape(12.dp))
                                                .padding(10.dp),
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = palette.accent)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(customer.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            customer.email,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textSecondary,
                                        )
                                        customer.phone?.let {
                                            Text(
                                                it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary,
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        customer.phone?.let { phone ->
                                            TextButton(onClick = { openWhatsApp(context, phone) }) {
                                                Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                                            }
                                        }
                                        TextButton(onClick = { openEmail(context, customer.email) }) {
                                            Icon(Icons.Filled.Email, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                viewModel.clearFeedback()
                showAddCustomerDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = palette.accent,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Crear cliente")
        }
    }
}

@Composable
private fun AddCustomerDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onAddCustomer: (name: String, email: String, phone: String?, password: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(generateTempPassword()) }
    val isEmailValid = email.trim().contains("@") && email.contains(".")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = UiPalette.current().surface),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Nuevo cliente", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = email.isNotBlank() && !isEmailValid,
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefono") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password inicial") },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { password = generateTempPassword() }) {
                    Text("Generar password")
                }
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
                            onAddCustomer(
                                name.trim(),
                                email.trim(),
                                phone.trim().takeIf { it.isNotBlank() },
                                password.trim(),
                            )
                        },
                        enabled = name.isNotBlank() && isEmailValid && password.isNotBlank() && !isSubmitting,
                    ) {
                        Text(if (isSubmitting) "Guardando..." else "Crear")
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    message: String,
    palette: com.dismal.app.core.ui.common.UiColors,
    isError: Boolean,
) {
    val background = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else palette.accentSoft
    val textColor = if (isError) MaterialTheme.colorScheme.error else palette.accent
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = textColor)
            Text(message, style = MaterialTheme.typography.bodySmall, color = textColor)
        }
    }
}

private fun generateTempPassword(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789"
    return (1..10).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}

private fun openWhatsApp(
    context: android.content.Context,
    phone: String,
) {
    val digits = phone.filter { it.isDigit() }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits"))
    startActivityIfResolved(context, intent, "No se encontro una app para WhatsApp.")
}

private fun openEmail(
    context: android.content.Context,
    email: String,
) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
    startActivityIfResolved(context, intent, "No se encontro una app de correo.")
}

private fun startActivityIfResolved(
    context: android.content.Context,
    intent: Intent,
    fallbackMessage: String,
) {
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, fallbackMessage, Toast.LENGTH_SHORT).show()
    }
}
