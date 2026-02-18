package family.petak.ombi.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
                modifier = Modifier.clickableListItem { viewModel.setTheme(value) }
            )
        }

        HorizontalDivider()

        // Logout
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
            leadingContent = {
                Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier.clickableListItem { showLogoutDialog = true }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(onLogout) }) {
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

// Extension to make ListItem clickable without adding full button semantics
private fun Modifier.clickableListItem(onClick: () -> Unit): Modifier =
    this.then(androidx.compose.foundation.clickable(onClick = onClick))
