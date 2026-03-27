package com.sonara.app.ai.cloud

import android.content.Context
import java.util.UUID

object AnonymousIdentity {
    private const val PREFS = "sonara_identity"
    private const val KEY_ID = "anonymous_id"

    fun getId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ID, null)
        if (id == null) { id = UUID.randomUUID().toString().replace("-", "").take(8); prefs.edit().putString(KEY_ID, id).apply() }
        return id
    }

    fun resetIdentity(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ID, UUID.randomUUID().toString().replace("-", "").take(8)).apply()
    }
}
