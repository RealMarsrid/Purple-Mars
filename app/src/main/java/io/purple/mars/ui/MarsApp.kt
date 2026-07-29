package io.purple.mars.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MarsApp() {

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9147FF),
            background = Color.Black
        )
    ) {

        Surface {

            Text(
                text = "Welcome to Purple Mars 🪐💜"
            )
        }
    }
}
