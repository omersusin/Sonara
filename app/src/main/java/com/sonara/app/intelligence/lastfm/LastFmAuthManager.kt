package com.sonara.app.intelligence.lastfm

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sonara.app.BuildConfig
import android.util.Log
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.preferences.SecureSecrets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class LastFmAuthManager(private val context: Context) {
    companion object {
        private const val TAG = "LastFmAuth"
        private const val AUTH_URL = "https://www.last.fm/api/auth/"
        private const val API_URL = "https://ws.audioscrobbler.com/2.0/"
        const val CALLBACK_SCHEME = "sonara"
        const val CALLBACK_HOST = "lastfm-auth"
        const val CALLBACK_URL = "$CALLBACK_SCHEME://$CALLBACK_HOST"
    }

    enum class AuthState { DISCONNECTED, AUTHENTICATING, CONNECTED, ERROR }

    private val _authState = MutableStateFlow(AuthState.DISCONNECTED)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val secrets = SecureSecrets(context)
    private val authPrefs = context.getSharedPreferences("sonara_auth_state", Context.MODE_PRIVATE)
    private var pendingToken: String?
        get() = authPrefs.getString("pending_token", null)
        set(value) { authPrefs.edit().putString("pending_token", value).apply() }
    private var authNonce: String? = null

    init {
        val sessionKey = secrets.getLastFmSessionKey()
        Log.d(TAG, "Init — checking auth state")
        if (sessionKey.isNotBlank()) {
            _authState.value = AuthState.CONNECTED
            Log.d(TAG, "Auth state → CONNECTED")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Init — session key found, loading username...")
                    loadUsername()
                } catch (e: Exception) {
                    Log.e(TAG, "Init — loadUsername failed: ${e.message}", e)
                }
            }
        } else if (pendingToken != null) {
            _authState.value = AuthState.AUTHENTICATING
            Log.d(TAG, "Auth state → AUTHENTICATING (pending token exists)")
        }
    }

    // FIX: Kullanici key ONCE kontrol edilir, BuildConfig sonra
    private fun resolveApiKey(): String {
        // resolveApiKey called
        val userKey = secrets.getLastFmApiKey()
        if (userKey.isNotBlank()) return userKey
        val buildKey = BuildConfig.LASTFM_API_KEY
        if (buildKey.isNotBlank()) return buildKey
        return ""
    }

    private fun resolveSharedSecret(): String {
        val userSecret = secrets.getLastFmSharedSecret()
        if (userSecret.isNotBlank()) return userSecret
        val buildSecret = BuildConfig.LASTFM_SHARED_SECRET
        if (buildSecret.isNotBlank()) return buildSecret
        return ""
    }

    fun hasApiKey(): Boolean = resolveApiKey().isNotBlank()

    fun keySource(): String = when {
        secrets.getLastFmApiKey().isNotBlank() -> "user"
        BuildConfig.LASTFM_API_KEY.isNotBlank() -> "built-in"
        else -> "none"
    }

    suspend fun startAuth(): Intent? {
        Log.d(TAG, "startAuth() called")
        if (_authState.value == AuthState.AUTHENTICATING && pendingToken != null) return null

        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            _authState.value = AuthState.ERROR
            _errorMessage.value = "API key required. Enter in Settings."
            return null
        }

        _authState.value = AuthState.AUTHENTICATING
        _errorMessage.value = ""

        return withContext(Dispatchers.IO) {
            try {
                val params = sortedMapOf("method" to "auth.getToken", "api_key" to apiKey)
                val sig = generateSignature(params, resolveSharedSecret())
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig).add("format", "json")

                val request = Request.Builder().url(API_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: throw Exception("Empty response")
                val obj = JSONObject(json)
                if (obj.has("error")) throw Exception(obj.optString("message", "Token failed"))

                val token = obj.getString("token")
                pendingToken = token
                authNonce = java.util.UUID.randomUUID().toString().take(16)
                Intent(Intent.ACTION_VIEW, Uri.parse("${AUTH_URL}?api_key=$apiKey&token=$token&cb=$CALLBACK_URL"))
            } catch (e: Exception) {
                SonaraLogger.e("LastFmAuth", "Token error: ${e.message}")
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message ?: "Connection error"
                null
            }
        }
    }

    suspend fun handleCallback(token: String? = null): Boolean {
        Log.d(TAG, "handleCallback() called")
        val useToken = token ?: pendingToken ?: return false
        val apiKey = resolveApiKey()
        val sharedSecret = resolveSharedSecret()

        if (apiKey.isBlank()) { _authState.value = AuthState.ERROR; _errorMessage.value = "API key not found"; return false }
        if (sharedSecret.isBlank()) { _authState.value = AuthState.ERROR; _errorMessage.value = "Shared secret required."; return false }

        return withContext(Dispatchers.IO) {
            try {
                val params = sortedMapOf("method" to "auth.getSession", "api_key" to apiKey, "token" to useToken)
                val sig = generateSignature(params, sharedSecret)
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig).add("format", "json")

                val request = Request.Builder().url(API_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: throw Exception("Empty response")
                val obj = JSONObject(json)

                if (obj.has("error")) {
                    val msg = obj.optString("message", "Session failed")
                    _authState.value = AuthState.ERROR; _errorMessage.value = msg
                    return@withContext false
                }

                val session = obj.getJSONObject("session")
                val sessionKey = session.getString("key")
                val name = session.getString("name")

                secrets.setLastFmSessionKey(sessionKey)
                if (secrets.getLastFmApiKey().isBlank()) secrets.setLastFmApiKey(apiKey)
                if (secrets.getLastFmSharedSecret().isBlank()) secrets.setLastFmSharedSecret(sharedSecret)

                _username.value = name
                _authState.value = AuthState.CONNECTED
                _errorMessage.value = ""
                pendingToken = null
                authNonce = null
                true
            } catch (e: Exception) {
                _authState.value = AuthState.ERROR; _errorMessage.value = e.message ?: "Error"
                false
            }
        }
    }

    fun disconnect() {
        secrets.setLastFmSessionKey("")
        _authState.value = AuthState.DISCONNECTED
        _username.value = ""; _errorMessage.value = ""
        pendingToken = null
        authNonce = null
    }

    fun hasPendingAuth(): Boolean = pendingToken != null
    fun reconnect() { disconnect() }
    fun isConnected(): Boolean = secrets.getLastFmSessionKey().isNotBlank()
    fun getActiveApiKey(): String = resolveApiKey()
    fun getActiveSecret(): String = resolveSharedSecret()
    fun getSessionKey(): String = secrets.getLastFmSessionKey()

    fun getConnectionInfo(): ConnectionInfo = ConnectionInfo(
        isConnected = isConnected(), username = _username.value,
        keySource = keySource(), hasSession = secrets.getLastFmSessionKey().isNotBlank()
    )

    data class ConnectionInfo(val isConnected: Boolean, val username: String, val keySource: String, val hasSession: Boolean)

    private suspend fun loadUsername() {
        Log.d(TAG, "loadUsername() called")
        try {
            val apiKey = resolveApiKey()
            val sk = secrets.getLastFmSessionKey()
            if (apiKey.isBlank() || sk.isBlank()) return
            val url = "${API_URL}?method=user.getInfo&api_key=$apiKey&sk=$sk&format=json"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return
            val obj = JSONObject(json)
            if (!obj.has("error")) {
                val name = obj.optJSONObject("user")?.optString("name", "") ?: ""
                if (name.isNotBlank()) _username.value = name
            }
        } catch (_: Exception) {}
    }

    private fun generateSignature(params: Map<String, String>, secret: String): String {
        val raw = params.entries.sortedBy { it.key }.joinToString("") { "${it.key}${it.value}" } + secret
        return md5(raw)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
