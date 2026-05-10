package com.sonara.app.ui.screens.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material3.Card
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonara.app.R

@Composable
fun DarkThemeScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val lifecycleOwner = LocalLifecycleOwner.current

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val isHighContrastDarkModeEnabled by settingsViewModel.highContrastDarkMode.collectAsState(
        initial = false
    )
    val themeMode by settingsViewModel.themeMode.collectAsState(initial = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

    val themeModes = listOf(
        stringResource(R.string.system) to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        stringResource(R.string.on) to AppCompatDelegate.MODE_NIGHT_YES,
        stringResource(R.string.off) to AppCompatDelegate.MODE_NIGHT_NO
    )

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
                        text = stringResource(R.string.dark_theme),
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
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(25.dp)
                )
            }

            itemsIndexed(themeModes) { index, mode ->

                val roundedCornerShape = getCardCornerByIndex(index, themeModes.size)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp, vertical = 1.dp),
                    onClick = { settingsViewModel.setThemeMode(mode.second) },
                    shape = roundedCornerShape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            text = mode.first,
                            style = MaterialTheme.typography.titleMedium
                        )

                        RadioButton(
                            selected = themeMode == mode.second,
                            onClick = { settingsViewModel.setThemeMode(mode.second) })
                    }
                }
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
                BooleanItemCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    checked = isHighContrastDarkModeEnabled,
                    onClick = { settingsViewModel.setHighContrastDarkTheme(!isHighContrastDarkModeEnabled) },
                    icon = Icons.Rounded.Contrast,
                    titleText = stringResource(R.string.high_contrast_dark_mode),
                    descriptionText = stringResource(R.string.des_high_contrast_dark_mode)
                )
            }
        }
    }
}

private fun getCardCornerByIndex(index: Int, size: Int): RoundedCornerShape {
    return when (index) {
        0 -> RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )

        size - 1 -> RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 4.dp,
            bottomStart = 20.dp,
            bottomEnd = 20.dp
        )

        else -> RoundedCornerShape(4.dp)
    }
}