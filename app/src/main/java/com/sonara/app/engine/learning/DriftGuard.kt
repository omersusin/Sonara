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

package com.sonara.app.engine.learning

import com.sonara.app.data.dao.UserFeedbackDao

class DriftGuard(private val feedbackDao: UserFeedbackDao) {
    data class GuardResult(val allow: Boolean, val reason: String? = null, val suggestReset: Boolean = false)

    suspend fun check(suggestedGenre: String, correctedGenre: String?, confidence: Float): GuardResult {
        if (correctedGenre == null || correctedGenre == suggestedGenre) return if (confidence >= 0.3f) GuardResult(true) else GuardResult(false, "Low confidence pseudo-label")
        val rate = feedbackDao.acceptanceRate(suggestedGenre)
        if (rate < 0.15f) { val c = feedbackDao.forGenre(suggestedGenre, 200); if (c.size >= 200) return GuardResult(false, "Genre over-rejected", suggestReset = true) }
        val recent = feedbackDao.forGenre(suggestedGenre, 10); val now = System.currentTimeMillis()
        val contradictions = recent.filter { it.timestamp > now - 30 * 60 * 1000L && it.correctedGenre != null && it.correctedGenre != correctedGenre }
        if (contradictions.size >= 3) return GuardResult(false, "Contradictory corrections")
        return GuardResult(true)
    }

    suspend fun pruneStaleData(maxAgeDays: Int = 90) { feedbackDao.prune(System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000)) }
}
