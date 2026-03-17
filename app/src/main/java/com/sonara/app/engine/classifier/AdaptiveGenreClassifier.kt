package com.sonara.app.engine.classifier

import android.content.Context
import android.util.Log
import kotlin.math.pow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import kotlin.math.sqrt

class AdaptiveGenreClassifier(private val context: Context) {
    companion object {
        private const val TAG = "AdaptiveClf"
        private const val MODEL_FILE = "genre_model.json"
        private const val NGRAM = 3
        private const val VOCAB = 2048
    }

    data class Centroid(val genre: String, var weights: FloatArray, var samples: Int, var totalWeight: Float)

    private var centroids = mutableMapOf<String, Centroid>()
    private var totalSamples = 0
    private val gson = Gson()

    init { load() }

    fun classify(title: String, artist: String, album: String = ""): Pair<String, Float> {
        if (centroids.isEmpty()) return "other" to 0f
        val feat = features(title, artist, album)
        var best = "other"; var bestSim = -1f; var second = -1f
        for ((g, c) in centroids) { val sim = cosine(feat, c.weights); if (sim > bestSim) { second = bestSim; bestSim = sim; best = g } else if (sim > second) second = sim }
        val margin = if (second >= 0) bestSim - second else bestSim
        val sf = centroids[best]?.let { (it.samples.toFloat() / 5f).coerceAtMost(1f) } ?: 0f
        val conf = (bestSim * 0.4f + margin * 0.3f + sf * 0.3f).coerceIn(0f, 1f)
        return best to conf
    }

    fun train(genre: String, title: String, artist: String, album: String = "", weight: Float = 1f) {
        val g = normalize(genre); val feat = features(title, artist, album)
        val c = centroids.getOrPut(g) { Centroid(g, FloatArray(VOCAB), 0, 0f) }
        val lr = weight * (0.3f * kotlin.math.pow(0.995, (totalSamples / 50).toDouble()).toFloat()).coerceAtLeast(0.01f)
        val old = c.totalWeight; val nw = old + lr
        if (nw > 0) { for (i in c.weights.indices) c.weights[i] = (c.weights[i] * old + feat[i] * lr) / nw }
        c.samples++; c.totalWeight = nw; totalSamples++
        Log.d(TAG, "Trained '$g': ${c.samples} samples")
        if (totalSamples % 10 == 0) save()
    }

    fun correct(wrong: String, correct: String, title: String, artist: String, album: String = "") {
        val feat = features(title, artist, album)
        centroids[normalize(wrong)]?.let { c -> val pw = -0.5f * 0.3f; val nt = c.totalWeight + pw; if (nt > 0.1f) { for (i in c.weights.indices) c.weights[i] = (c.weights[i] * c.totalWeight + feat[i] * pw) / nt; c.totalWeight = nt } }
        train(correct, title, artist, album, weight = 4f)
        save()
    }

    fun getStats(): Map<String, Any> = mapOf("total" to totalSamples, "genres" to centroids.size, "details" to centroids.map { (g, c) -> "$g:${c.samples}" })

    fun save() {
        try {
            val data = mapOf("centroids" to centroids.mapValues { (_, c) -> mapOf("genre" to c.genre, "weights" to c.weights.toList(), "samples" to c.samples, "totalWeight" to c.totalWeight) }, "totalSamples" to totalSamples)
            File(context.filesDir, MODEL_FILE).writeText(gson.toJson(data))
        } catch (e: Exception) { Log.e(TAG, "Save: ${e.message}") }
    }

    private fun load() {
        try {
            val f = File(context.filesDir, MODEL_FILE); if (!f.exists()) return
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(f.readText(), type)
            totalSamples = (data["totalSamples"] as? Double)?.toInt() ?: 0
            val cData = data["centroids"] as? Map<*, *> ?: return
            centroids.clear()
            for ((g, v) in cData) {
                val m = v as? Map<*, *> ?: continue
                val wList = (m["weights"] as? List<*>)?.mapNotNull { (it as? Double)?.toFloat() } ?: continue
                centroids[g.toString()] = Centroid(g.toString(), wList.toFloatArray(), (m["samples"] as? Double)?.toInt() ?: 0, (m["totalWeight"] as? Double)?.toFloat() ?: 0f)
            }
            Log.i(TAG, "Loaded: ${centroids.size} genres, $totalSamples samples")
        } catch (e: Exception) { Log.w(TAG, "Load: ${e.message}") }
    }

    private fun features(title: String, artist: String, album: String): FloatArray {
        val out = FloatArray(VOCAB); val text = "$artist $title $album".lowercase().trim()
        if (text.length < NGRAM) { for (c in text) out[(c.code * 31 % VOCAB + VOCAB) % VOCAB] += 1f }
        else { for (i in 0..text.length - NGRAM) { val h = text.substring(i, i + NGRAM).hashCode(); out[(h % VOCAB + VOCAB) % VOCAB] += 1f } }
        val words = text.split(Regex("\\s+")); for (w in words) { if (w.length >= 2) out[(("W_$w").hashCode() % VOCAB + VOCAB) % VOCAB] += 2f }
        val norm = sqrt(out.sumOf { (it * it).toDouble() }).toFloat(); if (norm > 0) for (i in out.indices) out[i] /= norm
        return out
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f; for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        val d = sqrt(na) * sqrt(nb); return if (d > 0) dot / d else 0f
    }

    private fun normalize(g: String): String = g.lowercase().trim().let { when {
        it.contains("hip") && it.contains("hop") -> "hip-hop"; it.contains("r&b") || it.contains("rnb") -> "r&b"
        it.contains("electro") || it == "edm" || it == "house" || it == "techno" -> "electronic"
        it.contains("metal") -> "metal"; it.contains("rock") || it == "alternative" -> "rock"; it.contains("pop") -> "pop"
        it == "classical" || it == "orchestra" -> "classical"; it.contains("jazz") -> "jazz"
        it.contains("blues") -> "blues"; it.contains("reggae") -> "reggae"; it.contains("latin") -> "latin"
        it == "country" -> "country"; it == "soul" || it == "funk" -> "soul"; else -> it
    } }
}
