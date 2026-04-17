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

package com.sonara.app.ai.demo

import android.content.Context
import android.util.Log
import com.sonara.app.ai.classifier.EmbeddedPrototypes
import com.sonara.app.ai.classifier.KnnClassifier
import com.sonara.app.ai.eq.SmartEqGenerator
import com.sonara.app.ai.explanation.ExplanationBuilder
import com.sonara.app.ai.models.*
import com.sonara.app.ai.personalization.Personalizer

class AiDemo(private val context: Context, private val dao: TrainingExampleDao) {
    private val TAG = "SonaraDemo"

    suspend fun runFullDemo() {
        Log.d(TAG, "=== SONARA AI DEMO ===")
        val classifier = KnnClassifier(dao)
        classifier.loadPrototypes(EmbeddedPrototypes.getAll())
        classifier.refreshCache()
        Log.d(TAG, "${dao.getCount()} prototypes loaded")
        val eqGen = SmartEqGenerator()

        val hipHop = AudioFeatureVector(0.31f,0.52f,0.23f,0.13f,0.52f, 0.93f,0.88f,0.73f,0.53f,0.43f,0.52f,0.57f,0.42f,0.27f,0.17f, 0.09f,0.61f,0.29f,0.37f, 0.47f,0.14f,0.39f,0f)
        testSong(classifier, eqGen, "Hip-Hop Test", hipHop)

        val classical = AudioFeatureVector(0.56f,0.49f,0.36f,0.07f,0.32f, 0.18f,0.22f,0.32f,0.48f,0.62f,0.78f,0.77f,0.63f,0.52f,0.43f, 0.16f,0.33f,0.57f,0.16f, 0.13f,0.29f,0.58f,0f)
        testSong(classifier, eqGen, "Classical Test", classical)

        val rock = AudioFeatureVector(0.41f,0.72f,0.36f,0.21f,0.78f, 0.63f,0.77f,0.58f,0.72f,0.82f,0.87f,0.72f,0.57f,0.42f,0.27f, 0.16f,0.63f,0.38f,0.37f, 0.33f,0.21f,0.46f,0f)
        testSong(classifier, eqGen, "Rock Test", rock)

        Log.d(TAG, "=== LEARNING TEST ===")
        val unknown = AudioFeatureVector(0.37f,0.58f,0.29f,0.13f,0.46f, 0.62f,0.58f,0.55f,0.68f,0.75f,0.72f,0.58f,0.42f,0.32f,0.22f, 0.11f,0.48f,0.42f,0.26f, 0.32f,0.18f,0.50f,0f)
        val before = classifier.classify(unknown)
        Log.d(TAG, "Before: ${before.primaryGenre}")
        classifier.learn(unknown, "blues", SonaraMood(-0.1f, 0.4f), 0.48f, "user_corrected", "Test Song", "Demo")
        classifier.refreshCache()
        val similar = AudioFeatureVector(0.36f,0.57f,0.28f,0.12f,0.44f, 0.60f,0.56f,0.53f,0.66f,0.73f,0.70f,0.56f,0.40f,0.30f,0.20f, 0.10f,0.46f,0.40f,0.24f, 0.31f,0.17f,0.52f,0f)
        val after = classifier.classify(similar)
        Log.d(TAG, "After: ${after.primaryGenre} ${if (after.primaryGenre.lowercase() == "blues") "OK" else "needs more"}")

        Log.d(TAG, "=== PERSONALIZATION TEST ===")
        val personalizer = Personalizer(context)
        val rockResult = classifier.classify(rock); val origEq = eqGen.generate(rockResult)
        personalizer.recordFeedback("too_bassy", rockResult, "bluetooth")
        val persEq = personalizer.applyPersonalization(origEq, rockResult, "bluetooth")
        Log.d(TAG, "Bass reduced: ${(persEq.bands[0]+persEq.bands[1])/2 < (origEq.bands[0]+origEq.bands[1])/2}")
        personalizer.reset()
        Log.d(TAG, "=== DEMO COMPLETE ===")
    }

    private suspend fun testSong(classifier: KnnClassifier, eqGen: SmartEqGenerator, name: String, features: AudioFeatureVector) {
        val result = classifier.classify(features); val eq = eqGen.generate(result)
        val explanation = ExplanationBuilder.build(result, eq, name, "Demo")
        Log.d(TAG, "$name: ${result.primaryGenre} | ${result.mood.description} | ${result.confidenceLevel.label} | ${explanation.sourceHonesty}")
    }
}
