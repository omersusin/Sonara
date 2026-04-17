/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = p, 
                    selectedTextColor = p, 
                    unselectedIconColor = SonaraTextTertiary, 
                    unselectedTextColor = SonaraTextTertiary, 
                    indicatorColor = p.copy(0.25f)
                ))
        }
    }
}
