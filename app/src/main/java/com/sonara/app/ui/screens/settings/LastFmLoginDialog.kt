package com.sonara.app.ui.screens.settings

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.sonara.app.ui.theme.SonaraCard
import com.sonara.app.ui.theme.SonaraError
import com.sonara.app.ui.theme.SonaraInfo
import com.sonara.app.ui.theme.SonaraTextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LastFmLoginDialog(vm: SettingsViewModel, onDismiss: () -> Unit) {
    val state by vm.uiState.collectAsState()

    var showWebView by remember { mutableStateOf(false) }
    var authUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Close the dialog automatically when auth succeeds
    LaunchedEffect(state.lastFmConnected) {
        if (state.lastFmConnected) onDismiss()
    }

    Dialog(onDismissRequest = {
        if (!isLoading) onDismiss()
    }) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = SonaraCard,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (!showWebView) {
                    Text("Connect Last.fm", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.lastFmUsernameInput,
                        onValueChange = { vm.updateLastFmUsernameInput(it) },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = tfColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.lastFmPasswordInput,
                        onValueChange = { vm.updateLastFmPasswordInput(it) },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = tfColors()
                    )

                    if (state.lastFmAuthError.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.lastFmAuthError,
                            color = SonaraError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            isLoading = true
                            vm.directLoginLastFm()
                        },
                        enabled = state.lastFmUsernameInput.isNotBlank()
                                && state.lastFmPasswordInput.isNotBlank()
                                && !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Log In")
                        }
                    }

                    // Reset loading when auth state changes (error or success)
                    LaunchedEffect(state.lastFmAuthError, state.lastFmConnected) {
                        if (isLoading && (state.lastFmAuthError.isNotBlank() || state.lastFmConnected)) {
                            isLoading = false
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            isLoading = true
                            vm.getLastFmAuthUrl { url ->
                                isLoading = false
                                if (url != null) {
                                    authUrl = url
                                    showWebView = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Login via Browser instead", color = SonaraInfo)
                        }
                    }
                } else {
                    Text("Authorize on Last.fm", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    ) {
                        val currentAuthUrl = authUrl
                        if (currentAuthUrl != null) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(
                                                view: WebView,
                                                request: WebResourceRequest
                                            ): Boolean {
                                                val url = request.url.toString()
                                                if (url.startsWith("sonara://lastfm-auth")) {
                                                    val token = request.url.getQueryParameter("token")
                                                    if (token != null) {
                                                        vm.handleLastFmWebViewCallback(token)
                                                    }
                                                    return true
                                                }
                                                return false
                                            }
                                        }
                                        loadUrl(currentAuthUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { showWebView = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Back to Login", color = SonaraTextSecondary)
                    }
                }

                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { if (!isLoading) onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = SonaraTextSecondary)
                }
            }
        }
    }
}
