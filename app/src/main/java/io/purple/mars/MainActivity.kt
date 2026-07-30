package io.purple.mars

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.purple.mars.ui.PurpleMarsDestination
import io.purple.mars.ui.ChatScreen
import io.purple.mars.ui.EmotesScreen
import io.purple.mars.ui.HomeScreen
import io.purple.mars.ui.PlayerScreen
import io.purple.mars.ui.SettingsScreen
import io.purple.mars.ui.theme.PurpleMarsTheme
import io.purple.mars.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(ThemePreference.get(this)) }
            PurpleMarsTheme(themeMode = themeMode) {
                PurpleMarsApp(
                    themeMode = themeMode,
                    onThemeChanged = { themeMode = it }
                )
            }
        }
    }
}

@Composable
fun PurpleMarsApp(
    themeMode: io.purple.mars.ui.theme.ThemeMode,
    onThemeChanged: (io.purple.mars.ui.theme.ThemeMode) -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                PurpleMarsDestination.values().forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = PurpleMarsDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(PurpleMarsDestination.Home.route) {
                HomeScreen(onStreamClick = { channel -> navController.navigate("player/$channel") })
            }
            composable(PurpleMarsDestination.Chat.route) { ChatScreen() }
            composable(PurpleMarsDestination.Emotes.route) { EmotesScreen() }
            composable(PurpleMarsDestination.Settings.route) {
                SettingsScreen(onThemeChanged = onThemeChanged)
            }
            composable(
                route = "player/{channel}",
                arguments = listOf(navArgument("channel") { type = NavType.StringType })
            ) { backStackEntry ->
                val channel = backStackEntry.arguments?.getString("channel") ?: ""
                PlayerScreen(channel)
            }
        }
    }
}
