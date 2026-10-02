package com.dismal.app.ui.pricelist

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dismal.app.core.ui.common.UiPalette
import java.util.Locale

@Composable
fun PriceListScreen(viewModel: PriceListViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var clientName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("Lista comercial de precios - Dismal") }
    var whatsapp by remember { mutableStateOf("") }
    var includePdf by remember { mutableStateOf(true) }
    var includeXlsx by remember { mutableStateOf(false) }
    var includeEcFinal by remember { mutableStateOf(true) }
    var includeEcDistributor by remember { mutableStateOf(true) }
    var includePeFinal by remember { mutableStateOf(true) }
    var includePeDistributor by remember { mutableStateOf(true) }
    var includeStock by remember { mutableStateOf(true) }

    val emailEnabled = email.trim().isNotBlank() && subject.trim().isNotBlank()
    val whatsappEnabled = whatsapp.trim().isNotBlank()
    val anyPriceSelected = includeEcFinal || includeEcDistributor || includePeFinal || includePeDistributor

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
                Text("Lista de precios", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Genera y envia listas comerciales desde la app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
            TextButton(onClick = viewModel::refreshProducts, enabled = !state.isLoading) {
                Text("Actualizar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.errorMessage != null) {
            FeedbackCard(
                title = "Error",
                message = state.errorMessage,
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                textColor = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (state.successMessage != null) {
            FeedbackCard(
                title = "Listo",
                message = state.successMessage,
                containerColor = palette.accentSoft,
                textColor = palette.accent,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Destino", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del cliente") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Asunto del email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Columnas", style = MaterialTheme.typography.titleMedium)
                    SelectionRow("EC Final", includeEcFinal) { includeEcFinal = it }
                    SelectionRow("EC Distribuidor", includeEcDistributor) { includeEcDistributor = it }
                    SelectionRow("PE Final", includePeFinal) { includePeFinal = it }
                    SelectionRow("PE Distribuidor", includePeDistributor) { includePeDistributor = it }
                    SelectionRow("Stock", includeStock) { includeStock = it }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Archivos para email",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textSecondary,
                    )
                    SelectionRow("Adjuntar PDF", includePdf) { includePdf = it }
                    SelectionRow("Adjuntar Excel", includeXlsx) { includeXlsx = it }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Productos", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.products.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }

                    when {
                        state.isLoading -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        state.products.isEmpty() -> {
                            Text(
                                "No hay productos disponibles todavia.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.height(260.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(state.products, key = { it.id }) { product ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = palette.background),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(product.name, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                buildPricePreview(product, includeEcFinal, includeEcDistributor, includePeFinal, includePeDistributor),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.sendByEmail(
                            clientName = clientName,
                            email = email,
                            subject = subject,
                            includePdf = includePdf,
                            includeXlsx = includeXlsx,
                            includeEcFinalPrice = includeEcFinal,
                            includeEcDistributorPrice = includeEcDistributor,
                            includePeFinalPrice = includePeFinal,
                            includePeDistributorPrice = includePeDistributor,
                            includeStock = includeStock,
                        )
                    },
                    enabled = emailEnabled && anyPriceSelected && !state.isSending,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSending) "Enviando..." else "Enviar email")
                }
                Button(
                    onClick = {
                        viewModel.sendByWhatsapp(
                            clientName = clientName,
                            whatsappPhone = whatsapp,
                            includeEcFinalPrice = includeEcFinal,
                            includeEcDistributorPrice = includeEcDistributor,
                            includePeFinalPrice = includePeFinal,
                            includePeDistributorPrice = includePeDistributor,
                            includeStock = includeStock,
                        )
                    },
                    enabled = whatsappEnabled && anyPriceSelected && !state.isSending,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (state.isSending) "Enviando..." else "Enviar WhatsApp")
                }
            }

            state.lastResult?.whatsappText?.takeIf { it.isNotBlank() }?.let { text ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Texto generado", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                        TextButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(text))
                                Toast.makeText(context, "Texto copiado.", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Text("Copiar texto")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = textColor)
            Text(message, style = MaterialTheme.typography.bodySmall, color = textColor)
        }
    }
}

private fun buildPricePreview(
    product: com.dismal.app.domain.models.AdminProduct,
    includeEcFinal: Boolean,
    includeEcDistributor: Boolean,
    includePeFinal: Boolean,
    includePeDistributor: Boolean,
): String {
    val prices = buildList {
        if (includeEcFinal) add("EC Final ${formatPrice(product.ecFinalPrice ?: product.price)}")
        if (includeEcDistributor) add("EC Dist ${formatPrice(product.ecDistributorPrice ?: product.ecFinalPrice ?: product.price)}")
        if (includePeFinal) add("PE Final ${formatPrice(product.peFinalPrice ?: product.ecFinalPrice ?: product.price)}")
        if (includePeDistributor) add("PE Dist ${formatPrice(product.peDistributorPrice ?: product.peFinalPrice ?: product.ecFinalPrice ?: product.price)}")
    }
    return prices.joinToString(" · ")
}

private fun formatPrice(value: Double): String = "$" + String.format(Locale.US, "%.2f", value)
