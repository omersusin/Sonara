@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.sonara.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sonara.app.ui.navigation.Screen
import com.sonara.app.ui.theme.SonaraDivider
import com.sonara.app.ui.theme.SonaraSurface
import com.sonara.app.ui.theme.SonaraTextTertiary

private data class NavItem(val screen: Screen, val icon: ImageVector)

@Composable
fun SonaraBottomBar(navController: NavController) {
    val items = listOf(
        NavItem(Screen.Dashboard, Icons.Rounded.Home),
        NavItem(Screen.Equalizer, Icons.Rounded.Tune),
        NavItem(Screen.Insights, Icons.Rounded.BarChart),
        NavItem(Screen.Settings, Icons.Rounded.Settings)
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val primary = MaterialTheme.colorScheme.primary

    Column {
        HorizontalDivider(thickness = 0.5.dp, color = SonaraDivider.copy(alpha = 0.5f))
        NavigationBar(containerColor = SonaraSurface, tonalElevation = 0.dp) {
            items.forEach { item ->
                val selected = currentRoute == item.screen.route
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.2f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                    label = "nav_icon_scale"
                )
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            item.icon,
                            contentDescription = item.screen.label,
                            modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                        )
                    },
                    label = { Text(item.screen.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = primary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        unselectedIconColor = SonaraTextTertiary,
                        unselectedTextColor = SonaraTextTertiary,
                        indicatorColor = primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}
