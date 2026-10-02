package com.dismal.app.features.products

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ProductDetailScreen(viewModel: ProductDetailViewModel) {
    val product by viewModel.product.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val purchaseState by viewModel.purchaseState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Confirm Purchase") },
            text = { Text("Do you want to buy ${product?.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.purchase()
                        showDialog = false
                    },
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (uiState.isLoading) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Cargando producto...",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else if (product == null) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = uiState.errorMessage ?: "Producto no disponible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        val currentProduct = requireNotNull(product)
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = currentProduct.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = currentProduct.name, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = currentProduct.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Price: ${currentProduct.price}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Platform: ${currentProduct.platform}", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            val licensesCount = currentProduct.licenses.size
            Text(
                text =
                    if (licensesCount > 0) {
                        "Licencias disponibles: $licensesCount"
                    } else {
                        "Licencias: no disponibles"
                    },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showDialog = true }, enabled = false) {
                Text(text = "Compra no disponible")
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (purchaseState) {
                is PurchaseState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PurchaseState.Success -> {
                    Text(
                        text = (purchaseState as PurchaseState.Success).message,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is PurchaseState.Error -> {
                    Text(
                        text = (purchaseState as PurchaseState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> {}
            }
        }
    }
}
