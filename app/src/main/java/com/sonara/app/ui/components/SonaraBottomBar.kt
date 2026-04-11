package com.sonara.app.ui.components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sonara.app.ui.navigation.Screen
import com.sonara.app.ui.theme.*

private data class NavItem(val screen: Screen, val icon: ImageVector)

@Composable
fun SonaraBottomBar(navController: NavController) {
    val items = listOf(NavItem(Screen.Dashboard, Icons.Rounded.Home), NavItem(Screen.Equalizer, Icons.Rounded.Tune),
        NavItem(Screen.Insights, Icons.Rounded.BarChart), NavItem(Screen.Settings, Icons.Rounded.Settings))
    val backStack by navController.currentBackStackEntryAsState()
    val cur = backStack?.destination?.route; val p = MaterialTheme.colorScheme.primary
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 0.dp) {
        items.forEach { item -> val sel = cur == item.screen.route
            NavigationBarItem(selected = sel, onClick = { if (cur != item.screen.route) navController.navigate(item.screen.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                icon = { Icon(item.icon, item.screen.label) }, label = { Text(item.screen.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = p, selectedTextColor = p, unselectedIconColor = SonaraTextTertiary, unselectedTextColor = SonaraTextTertiary, indicatorColor = p.copy(0.12f)))
        }
    }
}
