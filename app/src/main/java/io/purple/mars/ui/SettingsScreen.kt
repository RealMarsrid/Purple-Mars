package io.purple.mars.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.purple.mars.ui.theme.ThemeMode
import io.purple.mars.ui.theme.ThemePreference

@Composable
fun SettingsScreen(onThemeChanged: (ThemeMode) -> Unit) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(ThemePreference.get(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "⚙️ Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Column(modifier = Modifier.padding(top = 24.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Dark mode uses true black (AMOLED)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ThemeMode.values().forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                ThemePreference.set(context, mode)
                                onThemeChanged(mode)
                            }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = {
                            selectedMode = mode
                            ThemePreference.set(context, mode)
                            onThemeChanged(mode)
                        }
                    )
                    Text(
                        text = when (mode) {
                            ThemeMode.SYSTEM -> "Follow system"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark (AMOLED)"
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 20.dp))

        Column {
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Twitch login is coming soon — needs a verified Twitch developer app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(onClick = { /* wired up once Twitch OAuth client id is available */ }, enabled = false) {
                Text("Log in with Twitch")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 20.dp))

        Column {
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Purple Mars", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
            Text(
                "Version 1.0 (debug build)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No ads. Ever.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
