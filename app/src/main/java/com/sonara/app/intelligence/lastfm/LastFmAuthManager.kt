package com.sonara.app.intelligence.lastfm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.sonara.app.BuildConfig
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
    private val TOKEN_EXPIRY_MS = 10 * 60 * 1000L  // VULN-22: 10 min token expiry

    init {
        val sessionKey = secrets.getLastFmSessionKey()
        Log.d(TAG, "Init — checking auth state")
        if (sessionKey.isNotBlank()) {
            _authState.value = AuthState.CONNECTED
            Log.d(TAG, "Auth state → CONNECTED")
            // Restore cached username immediately so screens load without waiting for network
            val cached = authPrefs.getString("cached_username", "") ?: ""
            if (cached.isNotBlank()) _username.value = cached
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Init — session key found, loading username...")
                    loadUsername()
                } catch (e: Exception) {
                    Log.e(TAG, "Init — loadUsername failed: ${e.message}", e)
                }
            }
        } else if (pendingToken != null) {
            // VULN-22: Check if pending token is expired
            val tokenAge = System.currentTimeMillis() - authPrefs.getLong("pending_token_time", 0)
            if (tokenAge > TOKEN_EXPIRY_MS) {
                pendingToken = null
                Log.d(TAG, "Pending token expired, clearing")
            } else {
                _authState.value = AuthState.AUTHENTICATING
                Log.d(TAG, "Auth state → AUTHENTICATING (pending token exists)")
            }
        }
    }

    // User key takes priority; fall back to the app-bundled keys from BuildConfig.
    private fun resolveApiKey(): String {
        val user = secrets.getLastFmApiKey()
        if (user.isNotBlank()) return user
        return BuildConfig.LASTFM_API_KEY.ifBlank { "" }
    }

    private fun resolveSharedSecret(): String {
        val user = secrets.getLastFmSharedSecret()
        if (user.isNotBlank()) return user
        return BuildConfig.LASTFM_SHARED_SECRET.ifBlank { "" }
    }

    fun hasApiKey(): Boolean = resolveApiKey().isNotBlank()

    fun keySource(): String = when {
        secrets.getLastFmApiKey().isNotBlank() -> "user"
        else -> "none"
    }

    suspend fun startAuth(): Intent? {
        val url = getAuthUrl() ?: return null
        return Intent(Intent.ACTION_VIEW, Uri.parse(url))
    }

    suspend fun getAuthUrl(): String? {
        Log.d(TAG, "getAuthUrl() called")
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
                authPrefs.edit().putLong("pending_token_time", System.currentTimeMillis()).apply()
                authNonce = java.util.UUID.randomUUID().toString().take(16)
                Uri.parse(AUTH_URL).buildUpon()
                    .appendQueryParameter("api_key", apiKey)
                    .appendQueryParameter("token", token)
                    .appendQueryParameter("cb", CALLBACK_URL)
                    .build().toString()
            } catch (e: Exception) {
                SonaraLogger.e("LastFmAuth", "Token error: ${e.message}")
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message ?: "Connection error"
                null
            }
        }
    }

    suspend fun handleCallback(token: String? = null, state: String? = null): Boolean {
        Log.d(TAG, "handleCallback() called")
        val useToken = token ?: pendingToken ?: return false
        // VULN-26: CSRF state validation
        if (authNonce != null && state != null && state != authNonce) {
            Log.e(TAG, "CSRF check failed: state mismatch")
            _authState.value = AuthState.ERROR
            _errorMessage.value = "Security validation failed"
            return false
        }
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
                authPrefs.edit().putString("cached_username", name).apply()
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


    /**
     * Direct login: username + API key + secret → auth.getMobileSession
     * No browser redirect needed.
     */
    suspend fun directLogin(username: String, password: String): Boolean {
        val apiKey = resolveApiKey()
        val sharedSecret = resolveSharedSecret()
        if (apiKey.isBlank() || sharedSecret.isBlank()) {
            _authState.value = AuthState.ERROR
            _errorMessage.value = "Save API key and secret first"
            return false
        }
        _authState.value = AuthState.AUTHENTICATING
        _errorMessage.value = ""

        return withContext(Dispatchers.IO) {
            try {
                val params = sortedMapOf(
                    "method" to "auth.getMobileSession",
                    "api_key" to apiKey,
                    "username" to username,
                    "password" to password
                )
                val sig = generateSignature(params, sharedSecret)
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig).add("format", "json")

                val request = Request.Builder().url(API_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: throw Exception("Empty response")
                val obj = JSONObject(json)

                if (obj.has("error")) {
                    val msg = obj.optString("message", "Login failed")
                    _authState.value = AuthState.ERROR
                    _errorMessage.value = msg
                    return@withContext false
                }

                val session = obj.getJSONObject("session")
                val sessionKey = session.getString("key")
                val name = session.getString("name")

                secrets.setLastFmSessionKey(sessionKey)
                _username.value = name
                authPrefs.edit().putString("cached_username", name).apply()
                _authState.value = AuthState.CONNECTED
                _errorMessage.value = ""
                pendingToken = null
                authNonce = null
                SonaraLogger.i("LastFmAuth", "Direct login success: $name")
                true
            } catch (e: Exception) {
                SonaraLogger.e("LastFmAuth", "Direct login failed: ${e.message}")
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message ?: "Connection error"
                false
            }
        }
    }

    fun disconnect() {
        secrets.setLastFmSessionKey("")
        authPrefs.edit().putString("cached_username", "").apply()
        _authState.value = AuthState.DISCONNECTED
        _username.value = ""; _errorMessage.value = ""
        pendingToken = null
        authNonce = null
    }

    fun hasPendingAuth(): Boolean = pendingToken != null
    fun reconnect() { disconnect() }
    fun ensureConnectedState() {
        if (secrets.getLastFmSessionKey().isNotBlank() && _authState.value != AuthState.CONNECTED) {
            _authState.value = AuthState.CONNECTED
            Log.d(TAG, "State restored to CONNECTED")
        }
    }

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
            val body = FormBody.Builder()
                .add("method", "user.getInfo")
                .add("api_key", apiKey)
                .add("sk", sk)
                .add("format", "json")
                .build()
            val request = Request.Builder().url(API_URL).post(body).build()
            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return
            val obj = JSONObject(json)
            if (!obj.has("error")) {
                val name = obj.optJSONObject("user")?.optString("name", "") ?: ""
                if (name.isNotBlank()) {
                    _username.value = name
                    authPrefs.edit().putString("cached_username", name).apply()
                }
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
