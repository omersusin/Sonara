package com.sonara.app.data.preferences

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage for sensitive tokens (API keys, session keys).
 * Uses Android Keystore for encryption.
 */
class SecureSecrets(private val context: Context) {
    private val TAG = "SecureSecrets"
    private val KEYSTORE_ALIAS = "sonara_secrets_key"
    private val PREFS_NAME = "sonara_secure"
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray())
            val combined = iv + encrypted
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encrypt failed, refusing to store")
            ""
        }
    }

    private fun decrypt(encoded: String): String {
        if (encoded.isBlank()) return ""
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted))
        } catch (e: Exception) {
            Log.e(TAG, "Decrypt failed, returning empty")
            ""
        }
    }

    fun getLastFmApiKey(): String = decrypt(prefs.getString("lfm_api_key", "") ?: "")
    fun setLastFmApiKey(value: String) { prefs.edit().putString("lfm_api_key", encrypt(value)).apply() }

    fun getLastFmSharedSecret(): String = decrypt(prefs.getString("lfm_shared_secret", "") ?: "")
    fun setLastFmSharedSecret(value: String) { prefs.edit().putString("lfm_shared_secret", encrypt(value)).apply() }

    fun getLastFmSessionKey(): String = decrypt(prefs.getString("lfm_session_key", "") ?: "")
    fun setLastFmSessionKey(value: String) { prefs.edit().putString("lfm_session_key", encrypt(value)).apply() }

    /** GitHub PAT for cloud learning sync */
    fun getGitHubTokenInstance(): String = decrypt(prefs.getString("github_token", "") ?: "")
    fun setGitHubToken(value: String) { prefs.edit().putString("github_token", encrypt(value)).apply() }

    // ═══ AI Provider Keys (VULN-02 fix: encrypted storage) ═══
    fun getGeminiApiKey(): String = decrypt(prefs.getString("gemini_api_key", "") ?: "")
    fun setGeminiApiKey(value: String) { prefs.edit().putString("gemini_api_key", encrypt(value)).apply() }

    fun getOpenRouterApiKey(): String = decrypt(prefs.getString("openrouter_api_key", "") ?: "")
    fun setOpenRouterApiKey(value: String) { prefs.edit().putString("openrouter_api_key", encrypt(value)).apply() }

    fun getGroqApiKey(): String = decrypt(prefs.getString("groq_api_key", "") ?: "")
    fun setGroqApiKey(value: String) { prefs.edit().putString("groq_api_key", encrypt(value)).apply() }

    fun clearAll() { prefs.edit().clear().apply() }

    companion object {
        /**
         * Static accessor for DailySyncWorker (no instance needed).
         * Reads GitHub token from SecureSecrets.
         */
        fun getGitHubToken(context: Context): String? {
            val secrets = SecureSecrets(context)
            val token = secrets.getGitHubTokenInstance()
            return token.ifBlank { null }
        }
    }
}
