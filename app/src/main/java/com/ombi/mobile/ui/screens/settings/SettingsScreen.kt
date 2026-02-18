package com.ombi.mobile.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Account section
        SettingsSectionHeader("Account")

        ListItem(
            headlineContent = { Text(uiState.username ?: "Signed in") },
            supportingContent = { Text(uiState.serverUrl) },
            leadingContent = { Icon(Icons.Default.AccountCircle, null) }
        )

        HorizontalDivider()

        // Theme section
        SettingsSectionHeader("Appearance")

        val themes = listOf("system" to "System default", "dark" to "Dark", "light" to "Light")
        val themeIcons = mapOf(
            "system" to Icons.Default.PhoneAndroid,
            "dark" to Icons.Default.DarkMode,
            "light" to Icons.Default.LightMode
        )
        themes.forEach { (value, label) ->
            ListItem(
                headlineContent = { Text(label) },
                leadingContent = { Icon(themeIcons[value]!!, null) },
                trailingContent = {
                    RadioButton(
                        selected = uiState.theme == value,
                        onClick = { viewModel.setTheme(value) }
                    )
                },
                modifier = Modifier.clickable { viewModel.setTheme(value) }
            )
        }

        HorizontalDivider()

        // Logout
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
            leadingContent = {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier.clickable { showLogoutDialog = true }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.logout(onLogout)
                }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
