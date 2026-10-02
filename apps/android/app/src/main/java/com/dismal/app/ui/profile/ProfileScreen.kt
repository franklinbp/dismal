package com.dismal.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dismal.app.core.ui.common.UiPalette

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state = viewModel.uiState
    val palette = UiPalette.current()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(palette.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Perfil", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = viewModel::loadProfile,
                enabled = !state.isLoading,
            ) {
                Text("Actualizar")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Text(text = "Cargando...")
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.errorMessage != null) {
            Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                )
                Text(
                    text = state.email ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rol",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary,
                )
                Text(
                    text = state.role ?: "-",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Conexion con DismalCRM",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    state.isLoadingCrmStatus -> Text("Validando conexion segura...")
                    state.crmStatus != null -> {
                        val status = state.crmStatus
                        Text(
                            text = if (status.reachable) "Conectado" else "No conectado",
                            color = if (status.reachable) palette.accent else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                        if (status.endpoint.isNotBlank()) {
                            Text(
                                text = status.endpoint,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary,
                            )
                        }
                    }
                    state.crmStatusError != null -> {
                        Text(
                            text = state.crmStatusError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "La app se conecta a Dismal; el servidor protege el token y se comunica con DismalCRM.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = palette.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Seguridad offline",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.offlinePin,
                    onValueChange = viewModel::onOfflinePinChange,
                    label = { Text("Nuevo PIN (4 digitos)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.offlinePinConfirm,
                    onValueChange = viewModel::onOfflinePinConfirmChange,
                    label = { Text("Confirmar PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.offlinePinMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.offlinePinMessage,
                        color = if (state.offlinePinMessage.contains("actualizado")) palette.accent else MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = viewModel::saveOfflinePin,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Actualizar PIN")
                }
            }
        }
    }
}
