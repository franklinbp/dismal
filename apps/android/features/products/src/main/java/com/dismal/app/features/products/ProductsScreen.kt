package com.dismal.app.features.products

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dismal.app.core.ui.common.UiPalette
import java.util.Locale

@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel,
    onProductClick: (String) -> Unit,
    offlineMode: Boolean,
) {
    val state = viewModel.uiState
    val palette = UiPalette.current()
    val context = LocalContext.current
    val showPriceDialog = remember { mutableStateOf(false) }
    val pricePhone = remember { mutableStateOf("") }

    val platforms = state.items.map { it.platform }.distinct().sorted()
    val normalizedQuery = state.query.trim().lowercase(Locale.getDefault())
    val minPrice = state.minPrice.trim().toDoubleOrNull()
    val maxPrice = state.maxPrice.trim().toDoubleOrNull()
    val isRangeInvalid = minPrice != null && maxPrice != null && minPrice > maxPrice
    val filteredItems =
        state.items.filter { product ->
            val matchesQuery =
                normalizedQuery.isBlank() ||
                    product.name.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
                    product.description.lowercase(Locale.getDefault()).contains(normalizedQuery)
            val matchesPlatform = state.selectedPlatform == null || product.platform == state.selectedPlatform
            val matchesMin = minPrice == null || product.price >= minPrice
            val matchesMax = maxPrice == null || product.price <= maxPrice
            matchesQuery && matchesPlatform && matchesMin && matchesMax
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "Catalogo", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Productos disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showPriceDialog.value = true }) {
                    Text("Lista de precios")
                }
            }
        }
        if (offlineMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Modo offline activo",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Buscar productos") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.minPrice,
                onValueChange = viewModel::onMinPriceChange,
                label = { Text("Precio min") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                isError = isRangeInvalid,
            )
            OutlinedTextField(
                value = state.maxPrice,
                onValueChange = viewModel::onMaxPriceChange,
                label = { Text("Precio max") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                isError = isRangeInvalid,
            )
        }
        if (isRangeInvalid) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "El precio minimo no puede ser mayor que el maximo.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (platforms.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.selectedPlatform == null,
                        onClick = { viewModel.onPlatformChange(null) },
                        label = { Text("Todas") },
                    )
                }
                items(platforms) { platform ->
                    FilterChip(
                        selected = state.selectedPlatform == platform,
                        onClick = { viewModel.onPlatformChange(platform) },
                        label = { Text(platform) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mostrando ${filteredItems.size} de ${state.items.size}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            if (state.query.isNotBlank() ||
                state.selectedPlatform != null ||
                state.minPrice.isNotBlank() ||
                state.maxPrice.isNotBlank()
            ) {
                TextButton(
                    onClick = {
                        viewModel.onQueryChange("")
                        viewModel.onPlatformChange(null)
                        viewModel.onMinPriceChange("")
                        viewModel.onMaxPriceChange("")
                    },
                ) {
                    Text("Limpiar filtros")
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            else -> {
                if (filteredItems.isEmpty()) {
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
                                text = "Sin resultados",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Ajusta el filtro o intenta con otra busqueda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                            )
                            if (state.items.isEmpty()) {
                                Text(
                                    text = "No hay datos locales. Presiona Sincronizar en el Dashboard.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filteredItems, key = { it.id }) { product ->
                            Card(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onProductClick(product.id) },
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                colors = CardDefaults.cardColors(containerColor = palette.surface),
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val stockCount = product.licenses.count { it.available }
                                        Text(
                                            text = "Stock: $stockCount",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.textSecondary,
                                            maxLines = 1,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val price = String.format(Locale.US, "%.2f", product.price)
                                        Text(
                                            text = "$$price",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPriceDialog.value) {
        Dialog(onDismissRequest = { showPriceDialog.value = false }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Enviar lista de precios", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pricePhone.value,
                        onValueChange = { pricePhone.value = it },
                        label = { Text("Telefono WhatsApp") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showPriceDialog.value = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val digits = waDigitsFromPhone(pricePhone.value)
                                if (digits != null) {
                                    val message = buildPriceListMessage(state.items)
                                    openWhatsAppWithMessage(context, digits, message)
                                    showPriceDialog.value = false
                                }
                            },
                        ) {
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }
}

private fun buildPriceListMessage(items: List<com.dismal.app.domain.models.Product>): String {
    val header = "Lista de precios - Dismal"
    val lines =
        items.joinToString("\n") { item ->
            val price = String.format(Locale.US, "%.2f", item.price)
            "- ${item.name}: $$price"
        }
    return "$header\n$lines"
}

private fun formatEcuadorPhone(raw: String): String? {
    val cleaned =
        raw.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
    if (cleaned.isBlank()) return null

    val normalized =
        when {
            cleaned.startsWith("+") -> cleaned
            cleaned.startsWith("593") -> "+$cleaned"
            cleaned.startsWith("0") -> "+593${cleaned.drop(1)}"
            cleaned.all { it.isDigit() } && (cleaned.length == 8 || cleaned.length == 9) -> "+593$cleaned"
            else -> return null
        }

    val digits = normalized.drop(1)
    return if (digits.startsWith("593") && digits.length in 11..12) normalized else null
}

private fun waDigitsFromPhone(raw: String): String? {
    val formatted = formatEcuadorPhone(raw) ?: return null
    return formatted.drop(1)
}

private fun openWhatsAppWithMessage(
    context: android.content.Context,
    digits: String,
    message: String,
) {
    val encoded = Uri.encode(message)
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$digits?text=$encoded")
        }
    startActivityIfResolved(context, intent, "No se encontro una app para WhatsApp.")
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
