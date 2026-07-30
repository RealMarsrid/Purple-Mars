package io.purple.mars.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class PurpleMarsDestination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Filled.Home),
    Chat("chat", "Chat", Icons.Filled.ChatBubble),
    Emotes("emotes", "Emotes", Icons.Filled.EmojiEmotions),
    Settings("settings", "Settings", Icons.Filled.Settings)
}
