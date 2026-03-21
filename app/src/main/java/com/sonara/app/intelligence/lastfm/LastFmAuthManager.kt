package com.sonara.app.intelligence.lastfm

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.sonara.app.BuildConfig
import com.sonara.app.data.SonaraLogger
import com.sonara.app.data.preferences.SecureSecrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Last.fm OAuth bağlantı yöneticisi.
 *
 * Key kaynakları (öncelik sırasıyla):
 * 1. BuildConfig — GitHub Actions secret'larından build time'da inject
 * 2. SecureSecrets — kullanıcının Settings'den girdiği key
 *
 * Her iki yolda da source code'da hardcoded key YOKTUR.
 *
 * Akış:
 * 1. auth.getToken → kullanıcı Last.fm'e yönlendirilir
 * 2. Kullanıcı izin verir → deep link ile app'e döner
 * 3. auth.getSession → session key alınır, SecureSecrets'a yazılır
 * 4. Artık scrobble, love, nowPlaying çalışır
 */
class LastFmAuthManager(private val context: Context) {

    companion object {
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
    private var pendingToken: String? = null

    init {
        if (secrets.getLastFmSessionKey().isNotBlank()) {
            _authState.value = AuthState.CONNECTED
        }
    }

    // ═══ Key Resolution ═══
    // BuildConfig varsa onu kullan, yoksa kullanıcının girdiği key'i kullan
    private fun resolveApiKey(): String {
        val buildKey = BuildConfig.LASTFM_API_KEY
        if (buildKey.isNotBlank()) return buildKey
        return secrets.getLastFmApiKey()
    }

    private fun resolveSharedSecret(): String {
        val buildSecret = BuildConfig.LASTFM_SHARED_SECRET
        if (buildSecret.isNotBlank()) return buildSecret
        return secrets.getLastFmSharedSecret()
    }

    /** Key mevcut mu? (BuildConfig veya kullanıcı girişi) */
    fun hasApiKey(): Boolean = resolveApiKey().isNotBlank()

    /** Hangi kaynaktan geliyor? */
    fun keySource(): String = when {
        BuildConfig.LASTFM_API_KEY.isNotBlank() -> "built-in"
        secrets.getLastFmApiKey().isNotBlank() -> "user"
        else -> "none"
    }

    /**
     * Auth akışını başlat.
     * API key yoksa null döner — Settings'te kullanıcıyı uyar.
     */
    suspend fun startAuth(): Intent? {
        val apiKey = resolveApiKey()
        if (apiKey.isBlank()) {
            _authState.value = AuthState.ERROR
            _errorMessage.value = "API key gerekli. Settings'den girin veya uygulamayı güncelleyin."
            SonaraLogger.w("LastFmAuth", "No API key available")
            return null
        }

        _authState.value = AuthState.AUTHENTICATING
        _errorMessage.value = ""

        return withContext(Dispatchers.IO) {
            try {
                val params = sortedMapOf(
                    "method" to "auth.getToken",
                    "api_key" to apiKey
                )
                val sig = generateSignature(params, resolveSharedSecret())
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig)
                body.add("format", "json")

                val request = Request.Builder().url(API_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: throw Exception("Boş yanıt")

                val obj = JSONObject(json)
                if (obj.has("error")) {
                    throw Exception(obj.optString("message", "Token alınamadı"))
                }

                val token = obj.getString("token")
                pendingToken = token
                SonaraLogger.i("LastFmAuth", "Token alındı, web auth açılıyor")

                Intent(Intent.ACTION_VIEW, Uri.parse("${AUTH_URL}?api_key=$apiKey&token=$token&cb=$CALLBACK_URL"))
            } catch (e: Exception) {
                SonaraLogger.e("LastFmAuth", "Token hatası: ${e.message}")
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message ?: "Bağlantı hatası"
                null
            }
        }
    }

    /** Deep link callback sonrası session key al */
    suspend fun handleCallback(token: String? = null): Boolean {
        val useToken = token ?: pendingToken ?: return false
        val apiKey = resolveApiKey()
        val sharedSecret = resolveSharedSecret()

        if (apiKey.isBlank()) {
            _authState.value = AuthState.ERROR
            _errorMessage.value = "API key bulunamadı"
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val params = sortedMapOf(
                    "method" to "auth.getSession",
                    "api_key" to apiKey,
                    "token" to useToken
                )
                val sig = generateSignature(params, sharedSecret)
                val body = FormBody.Builder()
                params.forEach { (k, v) -> body.add(k, v) }
                body.add("api_sig", sig)
                body.add("format", "json")

                val request = Request.Builder().url(API_URL).post(body.build()).build()
                val response = client.newCall(request).execute()
                val json = response.body?.string() ?: throw Exception("Boş yanıt")
                val obj = JSONObject(json)

                if (obj.has("error")) {
                    val msg = obj.optString("message", "Session alınamadı")
                    SonaraLogger.e("LastFmAuth", "Session hatası: $msg")
                    _authState.value = AuthState.ERROR
                    _errorMessage.value = msg
                    return@withContext false
                }

                val session = obj.getJSONObject("session")
                val sessionKey = session.getString("key")
                val name = session.getString("name")

                // Session key'i güvenli depoya yaz
                secrets.setLastFmSessionKey(sessionKey)
                // Kullanılabilir API key/secret'ı da depola (sonraki kullanımlar için)
                if (secrets.getLastFmApiKey().isBlank()) {
                    secrets.setLastFmApiKey(apiKey)
                }
                if (secrets.getLastFmSharedSecret().isBlank()) {
                    secrets.setLastFmSharedSecret(sharedSecret)
                }

                _username.value = name
                _authState.value = AuthState.CONNECTED
                _errorMessage.value = ""
                pendingToken = null

                SonaraLogger.i("LastFmAuth", "Bağlantı başarılı: $name")
                true
            } catch (e: Exception) {
                SonaraLogger.e("LastFmAuth", "Session hatası: ${e.message}")
                _authState.value = AuthState.ERROR
                _errorMessage.value = e.message ?: "Bağlantı hatası"
                false
            }
        }
    }

    fun disconnect() {
        secrets.setLastFmSessionKey("")
        _authState.value = AuthState.DISCONNECTED
        _username.value = ""
        _errorMessage.value = ""
        SonaraLogger.i("LastFmAuth", "Bağlantı kesildi")
    }

    fun reconnect() {
        disconnect()
        // UI tekrar startAuth() çağıracak
    }

    fun isConnected(): Boolean = secrets.getLastFmSessionKey().isNotBlank()

    /** Scrobble/love işlemleri için aktif API key */
    fun getActiveApiKey(): String = resolveApiKey()
    fun getActiveSecret(): String = resolveSharedSecret()
    fun getSessionKey(): String = secrets.getLastFmSessionKey()

    /** Pending scrobble sayısı için Session bilgisi */
    fun getConnectionInfo(): ConnectionInfo {
        return ConnectionInfo(
            isConnected = isConnected(),
            username = _username.value,
            keySource = keySource(),
            hasSession = secrets.getLastFmSessionKey().isNotBlank()
        )
    }

    data class ConnectionInfo(
        val isConnected: Boolean,
        val username: String,
        val keySource: String,
        val hasSession: Boolean
    )

    private fun generateSignature(params: Map<String, String>, secret: String): String {
        val raw = params.entries.sortedBy { it.key }
            .joinToString("") { "${it.key}${it.value}" } + secret
        return md5(raw)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
