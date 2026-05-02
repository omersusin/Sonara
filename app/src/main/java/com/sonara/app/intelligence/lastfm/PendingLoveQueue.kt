package com.sonara.app.intelligence.lastfm

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class PendingLove(
    val id: String = "${System.currentTimeMillis()}_${(0..9999).random()}",
    val title: String, val artist: String, val loved: Boolean,
    val retries: Int = 0, val lastError: String = ""
)

object PendingLoveQueue {
    private const val MAX_RETRIES = 5
    private const val PREF_NAME = "sonara_pending_loves"
    private var prefs: SharedPreferences? = null

    fun init(ctx: Context) { prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

    fun enqueue(title: String, artist: String, loved: Boolean) {
        val list = load()
        list.add(PendingLove(title = title, artist = artist, loved = loved))
        save(list)
    }

    fun dequeue(id: String) { save(load().filter { it.id != id }.toMutableList()) }

    fun markFailed(id: String, err: String) {
        save(load().map {
            if (it.id == id) it.copy(retries = it.retries + 1, lastError = err) else it
        }.filter { it.retries < MAX_RETRIES }.toMutableList())
    }

    fun peekAll(): List<PendingLove> = load()
    fun pendingCount(): Int = load().size

    private fun load(): MutableList<PendingLove> = try {
        val arr = JSONArray(prefs?.getString("queue", "[]") ?: "[]")
        MutableList(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            PendingLove(o.getString("id"), o.getString("title"), o.getString("artist"),
                o.getBoolean("loved"), o.optInt("retries"), o.optString("lastError"))
        }
    } catch (_: Exception) { mutableListOf() }

    private fun save(list: MutableList<PendingLove>) {
        val arr = JSONArray()
        list.forEach { p -> arr.put(JSONObject().apply {
            put("id", p.id); put("title", p.title); put("artist", p.artist)
            put("loved", p.loved); put("retries", p.retries); put("lastError", p.lastError)
        }) }
        prefs?.edit()?.putString("queue", arr.toString())?.apply()
    }
}
