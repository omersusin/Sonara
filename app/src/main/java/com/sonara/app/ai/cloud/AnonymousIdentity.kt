package com.sonara.app.ai.cloud

import android.content.Context
import java.util.UUID

object AnonymousIdentity {
    private const val PREFS = "sonara_identity"
    private const val KEY_ID = "anonymous_id"
    private const val KEY_BATCH = "batch_counter"
    private const val ROTATE_EVERY = 10  // Rotate ID every N batches

    fun getId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ID, null)
        val batch = prefs.getInt(KEY_BATCH, 0)

        // Rotate identity periodically for privacy
        if (id == null || batch >= ROTATE_EVERY) {
            id = UUID.randomUUID().toString().replace("-", "").take(8)
            prefs.edit().putString(KEY_ID, id).putInt(KEY_BATCH, 0).apply()
        }
        return id
    }

    fun incrementBatch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_BATCH, prefs.getInt(KEY_BATCH, 0) + 1).apply()
    }

    fun resetIdentity(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ID, UUID.randomUUID().toString().replace("-", "").take(8)).putInt(KEY_BATCH, 0).apply()
    }
}
