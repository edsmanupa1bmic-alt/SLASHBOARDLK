package com.slashboard.keyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.slashboard.keyboard.ui.screens.AboutScreen
import com.slashboard.keyboard.ui.screens.ClipboardScreen
import com.slashboard.keyboard.ui.screens.DictionaryScreen
import com.slashboard.keyboard.ui.screens.LayoutsScreen
import com.slashboard.keyboard.ui.screens.PlaygroundScreen
import com.slashboard.keyboard.ui.screens.SettingsScreen
import com.slashboard.keyboard.ui.screens.SetupWizardScreen
import com.slashboard.keyboard.ui.screens.ThemesScreen
import com.slashboard.keyboard.ui.theme.SlashboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlashboardTheme {
                MainAppScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Playground : Screen("playground", "Sandbox", Icons.Default.Keyboard)
    data object Themes : Screen("themes", "Themes", Icons.Default.Palette)
    data object Layouts : Screen("layouts", "Layouts", Icons.Default.Language)
    data object Clipboard : Screen("clipboard", "Clipboard", Icons.AutoMirrored.Filled.Assignment)
    data object Dictionary : Screen("dictionary", "Shortcuts", Icons.Default.AutoFixHigh)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Setup : Screen("setup", "Setup", Icons.Default.Tune)
    data object About : Screen("about", "About", Icons.Default.Info)
}

val BottomNavItems = listOf(
    Screen.Playground,
    Screen.Themes,
    Screen.Layouts,
    Screen.Clipboard,
    Screen.Dictionary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val prefs = SlashboardApp.instance.preferencesRepository
    val hasCompletedSetup = remember { prefs.hasCompletedSetup() }
    val startDestination = if (hasCompletedSetup) Screen.Playground.route else Screen.Setup.route

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination

    val currentScreenTitle = when (currentRoute) {
        Screen.Playground.route -> "Slashboard By laka"
        Screen.Themes.route -> "Themes"
        Screen.Layouts.route -> "Layouts"
        Screen.Clipboard.route -> "Clipboard"
        Screen.Dictionary.route -> "Shortcuts"
        Screen.Settings.route -> "Settings"
        Screen.Setup.route -> "Keyboard Setup"
        Screen.About.route -> "About"
        else -> "Slashboard By laka"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentScreenTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate(Screen.Setup.route) },
                        modifier = Modifier.testTag("top_setup_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Setup",
                            tint = if (currentRoute == Screen.Setup.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.testTag("top_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentRoute == Screen.Settings.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { navController.navigate(Screen.About.route) },
                        modifier = Modifier.testTag("top_about_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = if (currentRoute == Screen.About.route) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                BottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Playground.route) {
                    PlaygroundScreen(
                        onNavigateToThemes = { navController.navigate(Screen.Themes.route) },
                        onNavigateToLayouts = { navController.navigate(Screen.Layouts.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                        onNavigateToSetup = { navController.navigate(Screen.Setup.route) }
                    )
                }
                composable(Screen.Themes.route) {
                    ThemesScreen()
                }
                composable(Screen.Layouts.route) {
                    LayoutsScreen()
                }
                composable(Screen.Clipboard.route) {
                    ClipboardScreen()
                }
                composable(Screen.Dictionary.route) {
                    DictionaryScreen()
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToSetup = { navController.navigate(Screen.Setup.route) }
                    )
                }
                composable(Screen.Setup.route) {
                    SetupWizardScreen(
                        onNavigateToPlayground = {
                            navController.navigate(Screen.Playground.route) {
                                popUpTo(Screen.Setup.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Screen.About.route) {
                    AboutScreen()
                }
            }
        }
    }
}
