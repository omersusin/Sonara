/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.ai.cloud

import android.content.Context
import java.util.UUID

object AnonymousIdentity {
    private const val PREFS = "sonara_identity"
    private const val KEY_ID = "anonymous_id"
    private const val KEY_BATCH = "batch_counter"
    private const val ROTATE_EVERY = 1  // VULN-16: Rotate every upload for privacy

    fun getId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_ID, null)
        val batch = prefs.getInt(KEY_BATCH, 0)

        // Rotate identity periodically for privacy
        if (id == null || batch >= ROTATE_EVERY) {
            id = UUID.randomUUID().toString().replace("-", "").take(16)  // VULN-16: 16 chars
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
        prefs.edit().putString(KEY_ID, UUID.randomUUID().toString().replace("-", "").take(16)).putInt(KEY_BATCH, 0).apply()
    }
}
