package com.ombi.mobile.ui.screens.serversetup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * First-launch screen that collects the user's Ombi server URL.
 *
 * Shown when no server URL is stored in [com.ombi.mobile.data.preferences.UserPreferences].
 * The URL is validated (must include a scheme) before being saved; the keyboard's
 * Done action and the "Connect" button both trigger [ServerSetupViewModel.save].
 *
 * On success, [onSetupComplete] navigates to the Login screen.
 */
@Composable
fun ServerSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: ServerSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connect to Ombi", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enter the URL of your Ombi server.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = uiState.url,
            onValueChange = viewModel::onUrlChange,
            label = { Text("Server URL") },
            placeholder = { Text("https://ombi.example.com") },
            isError = uiState.error != null,
            supportingText = uiState.error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { viewModel.save(onSetupComplete) }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Warn when the URL uses cleartext HTTP. Traffic (including the login
        // token) would travel unencrypted; https:// is strongly preferred.
        if (uiState.url.trim().startsWith("http://", ignoreCase = true)) {
            Spacer(Modifier.height(16.dp))
            CleartextWarning()
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.save(onSetupComplete) },
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Connect")
            }
        }
    }
}

/**
 * A warning card shown when the entered server URL uses cleartext `http://`.
 * Advises the user that their connection — including the login token — would
 * be unencrypted.
 */
@Composable
private fun CleartextWarning() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                "This connection is not encrypted. Your login and data will be " +
                    "sent in cleartext. Use https:// if your server supports it.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
