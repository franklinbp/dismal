package com.dismal.app.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dismal.app.core.ui.common.UiPalette
import com.dismal.app.domain.models.AdminProduct
import com.dismal.app.domain.models.Customer
import java.util.Locale

@Composable
fun SalesScreen(viewModel: SaleViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()
    var showAddSaleDialog by remember { mutableStateOf(false) }

    if (showAddSaleDialog) {
        AddSaleDialog(
            customers = state.customers,
            products = state.products,
            availableActivationsByProductId = state.availableActivationsByProductId,
            inventoryLoaded = state.inventoryLoaded,
            isSubmitting = state.isSubmitting,
            onDismiss = { if (!state.isSubmitting) showAddSaleDialog = false },
            onAddSale = { clientId, softwareId, quantity, unitPrice, saleType, country, deliveryChannels, registerFullPayment, paymentMethod, paymentReference ->
                viewModel.addSale(
                    clientId = clientId,
                    softwareId = softwareId,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    saleType = saleType,
                    country = country,
                    deliveryChannels = deliveryChannels,
                    registerFullPayment = registerFullPayment,
                    paymentMethod = paymentMethod,
                    paymentReference = paymentReference,
                    onSuccess = { showAddSaleDialog = false },
                )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ventas", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Crea, confirma, cobra y entrega desde la app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                    )
                }
                TextButton(onClick = viewModel::refreshData, enabled = !state.isSubmitting) {
                    Text("Actualizar")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.errorMessage?.let {
                FeedbackCard("No se pudo completar", it, true)
                Spacer(modifier = Modifier.height(10.dp))
            }
            state.successMessage?.let {
                FeedbackCard("Venta procesada", it, false)
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (state.sales.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.surface),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Sin ventas registradas", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pulsa + para crear la primera venta.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.sales, key = { it.id }) { sale ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = palette.surface),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        sale.saleNumber ?: "Venta #${sale.id.takeLast(6).uppercase()}",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        saleStatusLabel(sale.status),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.accent,
                                    )
                                }
                                Text(
                                    state.customerNames[sale.clientId] ?: sale.clientId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                )
                                Text(
                                    formatMoney(sale.total, sale.currency),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                )
                                sale.items.firstOrNull()?.let { item ->
                                    Text(
                                        "${item.quantity} unidad(es) · ${item.softwareName ?: item.softwareId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary,
                                    )
                                }
                                Text(
                                    if (sale.country == "PE") "Perú" else "Ecuador",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary,
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (viewModel.prepareNewSale()) showAddSaleDialog = true
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = palette.accent,
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Crear venta")
        }
    }
}

@Composable
private fun FeedbackCard(
    title: String,
    message: String,
    isError: Boolean,
) {
    val palette = UiPalette.current()
    val background = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.12f) else palette.accentSoft
    val textColor = if (isError) MaterialTheme.colorScheme.error else palette.accent
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = textColor)
            Text(message, style = MaterialTheme.typography.bodySmall, color = textColor)
        }
    }
}

private enum class PriceMode(
    val label: String,
    val country: String,
) {
    EC_FINAL("Ecuador · Cliente final", "EC"),
    EC_DISTRIBUTOR("Ecuador · Distribuidor", "EC"),
    PE_FINAL("Perú · Cliente final", "PE"),
    PE_DISTRIBUTOR("Perú · Distribuidor", "PE"),
}

private enum class SaleTypeOption(
    val apiValue: String,
    val label: String,
) {
    CASH("CASH", "Contado"),
    CREDIT("CREDIT", "Crédito"),
}

private enum class PaymentMethodOption(
    val apiValue: String,
    val label: String,
) {
    CASH("CASH", "Efectivo"),
    CARD("CARD", "Tarjeta"),
    TRANSFER("TRANSFER", "Transferencia"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSaleDialog(
    customers: List<Customer>,
    products: List<AdminProduct>,
    availableActivationsByProductId: Map<String, Int>,
    inventoryLoaded: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onAddSale: (
        clientId: String,
        softwareId: String,
        quantity: Int,
        unitPrice: Double,
        saleType: String,
        country: String,
        deliveryChannels: List<String>,
        registerFullPayment: Boolean,
        paymentMethod: String,
        paymentReference: String?,
    ) -> Unit,
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedProduct by remember { mutableStateOf<AdminProduct?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var saleType by remember { mutableStateOf(SaleTypeOption.CASH) }
    var registerFullPayment by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf(PaymentMethodOption.TRANSFER) }
    var paymentReference by remember { mutableStateOf("") }
    var sendEmail by remember { mutableStateOf(true) }
    var sendWhatsApp by remember { mutableStateOf(true) }
    var priceMode by remember { mutableStateOf(PriceMode.EC_FINAL) }

    fun resolvePrice(product: AdminProduct?, mode: PriceMode): Double? {
        if (product == null) return null
        return when (mode) {
            PriceMode.EC_FINAL -> product.ecFinalPrice ?: product.price
            PriceMode.EC_DISTRIBUTOR -> product.ecDistributorPrice ?: product.ecFinalPrice ?: product.price
            PriceMode.PE_FINAL -> product.peFinalPrice ?: product.ecFinalPrice ?: product.price
            PriceMode.PE_DISTRIBUTOR -> product.peDistributorPrice ?: product.peFinalPrice ?: product.ecFinalPrice ?: product.price
        }
    }

    val quantityValue = quantity.toIntOrNull()
    val unitPriceValue = unitPrice.toDoubleOrNull()
    val availableActivations =
        selectedProduct?.id?.let { productId ->
            if (inventoryLoaded) availableActivationsByProductId[productId] ?: 0 else null
        }
    val hasInsufficientStock =
        availableActivations != null &&
            quantityValue != null &&
            quantityValue > availableActivations
    val total = (quantityValue ?: 0) * (unitPriceValue ?: 0.0)
    val formValid =
        selectedCustomer != null &&
            selectedProduct != null &&
            quantityValue != null &&
            quantityValue > 0 &&
            unitPriceValue != null &&
            unitPriceValue > 0

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .padding(horizontal = 12.dp, vertical = 16.dp)
                    .imePadding(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = UiPalette.current().surface),
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nueva venta", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Selecciona cliente, producto y forma de pago.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UiPalette.current().textSecondary,
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SelectCustomerField(customers, selectedCustomer) { selectedCustomer = it }
                    SelectProductField(
                        products = products,
                        selectedProduct = selectedProduct,
                        onSelected = {
                            selectedProduct = it
                            resolvePrice(it, priceMode)?.let { price ->
                                unitPrice = String.format(Locale.US, "%.2f", price)
                            }
                        },
                    )
                    SelectPriceModeField(
                        selectedMode = priceMode,
                        onSelected = { mode ->
                            priceMode = mode
                            resolvePrice(selectedProduct, mode)?.let { price ->
                                unitPrice = String.format(Locale.US, "%.2f", price)
                            }
                        },
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it.filter(Char::isDigit) },
                            label = { Text("Cantidad") },
                            modifier = Modifier.weight(0.38f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = unitPrice,
                            onValueChange = { unitPrice = it.replace(',', '.') },
                            label = { Text("Precio unitario") },
                            modifier = Modifier.weight(0.62f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                        )
                    }

                    selectedProduct?.let {
                        val stockColor =
                            if (hasInsufficientStock) {
                                MaterialTheme.colorScheme.error
                            } else {
                                UiPalette.current().textSecondary
                            }
                        Text(
                            text =
                                when (availableActivations) {
                                    null -> "Inventario: se validara al confirmar."
                                    0 -> "Inventario: sin activaciones disponibles para confirmar."
                                    1 -> "Inventario: 1 activacion disponible para confirmar."
                                    else -> "Inventario: $availableActivations activaciones disponibles para confirmar."
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = stockColor,
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = UiPalette.current().accentSoft),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Total de la venta", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "$${String.format(Locale.US, "%.2f", total)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    SelectSaleTypeField(
                        selected = saleType,
                        onSelected = {
                            saleType = it
                            if (it == SaleTypeOption.CREDIT) registerFullPayment = false
                        },
                    )

                    if (saleType == SaleTypeOption.CASH) {
                        LabeledCheckbox(
                            checked = registerFullPayment,
                            onCheckedChange = { registerFullPayment = it },
                            label = "Registrar el pago completo ahora",
                        )
                        if (registerFullPayment) {
                            SelectPaymentMethodField(paymentMethod) { paymentMethod = it }
                            OutlinedTextField(
                                value = paymentReference,
                                onValueChange = { paymentReference = it },
                                label = { Text("Referencia o comprobante") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    } else {
                        Text(
                            "La venta se aplicará al crédito disponible del cliente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UiPalette.current().textSecondary,
                        )
                    }

                    Text("Entrega y notificaciones", style = MaterialTheme.typography.titleSmall)
                    LabeledCheckbox(sendEmail, { sendEmail = it }, "Enviar por correo")
                    LabeledCheckbox(sendWhatsApp, { sendWhatsApp = it }, "Enviar por WhatsApp")
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val deliveryChannels =
                                buildList {
                                    if (sendEmail) add("EMAIL")
                                    if (sendWhatsApp) add("WHATSAPP")
                                }
                            onAddSale(
                                selectedCustomer!!.id,
                                selectedProduct!!.id,
                                quantityValue!!,
                                unitPriceValue!!,
                                saleType.apiValue,
                                priceMode.country,
                                deliveryChannels,
                                registerFullPayment && saleType == SaleTypeOption.CASH,
                                paymentMethod.apiValue,
                                paymentReference.ifBlank { "MOBILE_APP" },
                            )
                        },
                        enabled = formValid && !hasInsufficientStock && !isSubmitting,
                    ) {
                        Text(if (isSubmitting) "Procesando..." else "Confirmar venta")
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectCustomerField(
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelected: (Customer) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedCustomer?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Cliente") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            customers.forEach { customer ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(customer.name)
                            Text(customer.email, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelected(customer)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectProductField(
    products: List<AdminProduct>,
    selectedProduct: AdminProduct?,
    onSelected: (AdminProduct) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedProduct?.name.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Producto") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            products.forEach { product ->
                DropdownMenuItem(
                    text = { Text(product.name) },
                    onClick = {
                        onSelected(product)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPriceModeField(
    selectedMode: PriceMode,
    onSelected: (PriceMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedMode.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("País y tarifa") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PriceMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    onClick = {
                        onSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectSaleTypeField(
    selected: SaleTypeOption,
    onSelected: (SaleTypeOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Condición de venta") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SaleTypeOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectPaymentMethodField(
    selected: PaymentMethodOption,
    onSelected: (PaymentMethodOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Método de pago") },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaymentMethodOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun saleStatusLabel(status: String): String =
    when (status.uppercase(Locale.US)) {
        "DRAFT" -> "Borrador"
        "CONFIRMED" -> "Confirmada"
        "PAID" -> "Pagada"
        "PARTIALLY_PAID" -> "Pago parcial"
        "CANCELLED" -> "Anulada"
        else -> status
    }

private fun formatMoney(
    value: Double,
    currency: String,
): String {
    val prefix = if (currency.uppercase(Locale.US) == "PEN") "S/ " else "$"
    return prefix + String.format(Locale.US, "%.2f", value)
}
