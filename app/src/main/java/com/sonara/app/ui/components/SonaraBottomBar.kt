package com.sonara.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sonara.app.ui.navigation.Screen
import com.sonara.app.ui.theme.*

private data class NavItem(val screen: Screen, val icon: ImageVector, val label: String)

@Composable
fun SonaraBottomBar(navController: NavController) {
    val items = listOf(
        NavItem(Screen.Dashboard, Icons.Rounded.Home,     Screen.Dashboard.label),
        NavItem(Screen.Equalizer, Icons.Rounded.Tune,     Screen.Equalizer.label),
        NavItem(Screen.Insights,  Icons.Rounded.BarChart, Screen.Insights.label),
        NavItem(Screen.Settings,  Icons.Rounded.Settings, Screen.Settings.label)
    )

    val backStack    by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route
    val primary       = MaterialTheme.colorScheme.primary

    Column {
        HorizontalDivider(thickness = 0.5.dp, color = SonaraDivider.copy(alpha = 0.4f))

        // MD3 Expressive: NavigationBar daha az yüksek, indicator daha yuvarlak
        NavigationBar(
            containerColor  = SonaraSurface,
            tonalElevation  = 0.dp,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.screen.route

                // Expressive: seçili ikon hafifçe büyüsün (spring bounce)
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    label = "navIconScale_${item.label}"
                )

                NavigationBarItem(
                    selected = selected,
                    onClick  = {
                        if (currentRoute != item.screen.route) {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector     = item.icon,
                            contentDescription = item.label,
                            modifier        = Modifier
                                .size(24.dp)
                                .scale(iconScale)
                        )
                    },
                    label  = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = primary,
                        selectedTextColor   = primary,
                        unselectedIconColor = SonaraTextTertiary,
                        unselectedTextColor = SonaraTextTertiary,
                        // Expressive: indicator daha geniş ve pill benzeri
                        indicatorColor      = primary.copy(alpha = 0.14f)
                    )
                )
            }
        }
    }
}
