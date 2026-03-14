package com.sonara.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sonara.app.ui.navigation.Screen
import com.sonara.app.ui.theme.SonaraPrimary
import com.sonara.app.ui.theme.SonaraPrimaryDark
import com.sonara.app.ui.theme.SonaraSurface
import com.sonara.app.ui.theme.SonaraTextMuted

private data class NavItem(val screen: Screen, val icon: ImageVector)

@Composable
fun SonaraBottomBar(navController: NavController) {
    val items = listOf(
        NavItem(Screen.Dashboard, Icons.Rounded.Home),
        NavItem(Screen.Equalizer, Icons.Rounded.Tune),
        NavItem(Screen.Presets, Icons.Rounded.QueueMusic),
        NavItem(Screen.Insights, Icons.Rounded.BarChart),
        NavItem(Screen.Settings, Icons.Rounded.Settings)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar(containerColor = SonaraSurface, contentColor = SonaraTextMuted) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.screen.label) },
                label = { Text(item.screen.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SonaraPrimary,
                    selectedTextColor = SonaraPrimary,
                    unselectedIconColor = SonaraTextMuted,
                    unselectedTextColor = SonaraTextMuted,
                    indicatorColor = SonaraPrimaryDark.copy(alpha = 0.15f)
                )
            )
        }
    }
}
