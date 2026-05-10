package com.sonara.app.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.R
import com.sonara.app.ui.common.LocalDarkMode
import com.sonara.app.ui.common.LocalDynamicColor
import com.sonara.app.ui.components.svg.DynamicColorImageVector
import com.sonara.app.ui.components.svg.vectors.themePicker
import com.sonara.app.ui.components.tab.ColorTabs

@Composable
fun LookAndFeelScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onNavigateDarkThemeScreen: () -> Unit = {}
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val viewModel: SettingsViewModel = viewModel()

    val isDynamicColorEnabled = LocalDynamicColor.current
    val isDarkMode = LocalDarkMode.current
    val themeMode by settingsViewModel.themeMode.collectAsState(initial = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    val collapsedFraction = scrollBehavior.state.collapsedFraction
                    val expandedFontSize = 33.sp
                    val collapsedFontSize = 20.sp

                    val fontSize = lerp(expandedFontSize, collapsedFontSize, collapsedFraction)
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = stringResource(R.string.look_and_feel),
                        maxLines = 1,
                        fontSize = fontSize,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = 0.05.em
                    )
                },
                navigationIcon = {
                    BackButton(onBackClick = {
                        // Only navigate if the current lifecycle state is RESUMED.
                        // This prevents multiple popBackStack calls during rapid clicks,
                        // which can lead to a blank screen or an invalid navigation state.
                        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            onBackClick()
                        }
                    })
                },
                scrollBehavior = scrollBehavior,
            )
        }) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding
        ) {
            item {
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 100.dp, vertical = 25.dp),
                    imageVector = DynamicColorImageVector.themePicker(),
                    contentDescription = null
                )
            }

            item {
                ColorTabs(
                    modifier = Modifier.padding(20.dp),
                    settingsViewModel = viewModel
                )
            }

            item {
                BooleanItemCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    checked = isDynamicColorEnabled,
                    icon = Icons.Rounded.Colorize,
                    titleText = stringResource(R.string.dynamic_colors),
                    descriptionText = stringResource(R.string.des_dynamic_colors),
                    onClick = { settingsViewModel.setDynamicColorEnabled(!isDynamicColorEnabled) }
                )
            }

            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )
            }

            item {
                Text(
                    text = stringResource(R.string.additional_settings),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 25.dp)
                        .animateItem()
                )
            }

            item {
                ItemCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    icon = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    titleText = stringResource(R.string.dark_theme),
                    descriptionText = when (themeMode) {
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> stringResource(R.string.system)
                        AppCompatDelegate.MODE_NIGHT_YES -> stringResource(R.string.on)
                        AppCompatDelegate.MODE_NIGHT_NO -> stringResource(R.string.off)
                        else -> ""
                    },
                    onClick = onNavigateDarkThemeScreen
                )
            }
        }
    }
}

@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
    IconButton(onClick = onBackClick) {
        Icon(
            modifier = modifier,
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BooleanItemCard(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onClick: () -> Unit = {},
    icon: ImageVector? = null,
    titleText: String = "",
    descriptionText: String = "",
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    enabled: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = shape,
        onClick = { if (enabled) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 17.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (titleText.isNotEmpty()) {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                }

                if (descriptionText.isNotEmpty()) {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    onClick()
                })
        }
    }
}

@Composable
private fun ItemCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: ImageVector? = null,
    titleText: String = "",
    descriptionText: String = "",
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    enabled: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = shape,
        onClick = { if (enabled) onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    )
    {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 17.dp)
                .alpha(if (enabled) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                if (titleText.isNotEmpty()) {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                }

                if (descriptionText.isNotEmpty()) {
                    Text(
                        text = descriptionText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }
        }
    }
}