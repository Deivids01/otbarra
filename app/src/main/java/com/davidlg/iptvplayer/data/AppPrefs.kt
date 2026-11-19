package com.davidlg.iptvplayer.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Envoltorio comparable de un JSON de ajustes (para usarlo en StateFlow). */
data class JsonKv(val raw: String = "{}") {
    private val o: JSONObject = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    fun str(k: String, def: String = ""): String = if (o.has(k) && !o.isNull(k)) o.optString(k, def) else def
    fun bool(k: String, def: Boolean = false): Boolean = if (o.has(k)) o.optBoolean(k, def) else def
    fun int(k: String, def: Int = 0): Int = if (o.has(k)) o.optInt(k, def) else def
    fun has(k: String): Boolean = o.has(k) && !o.isNull(k)
    fun json(): JSONObject = JSONObject(raw)
    fun with(k: String, v: Any?): JsonKv { val n = JSONObject(raw); if (v == null) n.remove(k) else n.put(k, v); return JsonKv(n.toString()) }
}

/**
 * Almacenamiento con las mismas claves que la versión web. Igual que en profiles.js: el primer perfil
 * ("p1") conserva las claves de siempre y el resto usa el prefijo davidlgP:<id>: para las claves por perfil.
 */
class AppPrefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("davidlg", Context.MODE_PRIVATE)

    companion object {
        /** Claves propias de cada perfil (PK en profiles.js). */
        val PER_PROFILE = setOf(
            "recentChannels", "davidlgVodFavorites", "davidlgVodProgress", "davidlgVodMetadata",
            "vod_playback_history", "playbackPrefs", "davidlgVodSettings",
        )
        const val REG = "davidlgProfiles"
        const val MAX_PROFILES = 5
    }

    // ---------- Perfiles ----------
    var activeProfileId: String = "p1"
        private set

    init { activeProfileId = loadRegistry().second }

    private fun loadRegistry(): Pair<List<Profile>, String> {
        try {
            val o = JSONObject(sp.getString(REG, "") ?: "")
            val a = o.getJSONArray("list")
            val list = (0 until a.length()).map {
                val p = a.getJSONObject(it); Profile(p.getString("id"), p.optString("name", "Perfil"), p.optInt("av", 0))
            }
            if (list.isNotEmpty()) {
                val act = o.optString("active", list[0].id)
                return list to (if (list.any { it.id == act }) act else list[0].id)
            }
        } catch (_: Exception) {}
        return listOf(Profile("p1", "Mi perfil", 0)) to "p1"
    }

    fun profiles(): List<Profile> = loadRegistry().first

    fun saveProfiles(list: List<Profile>, active: String) {
        val a = JSONArray()
        for (p in list) a.put(JSONObject().put("id", p.id).put("name", p.name).put("av", p.av))
        sp.edit().putString(REG, JSONObject().put("v", 1).put("list", a).put("active", active).toString()).apply()
        activeProfileId = active
    }

    /** Borra los datos propios de un perfil (davidlgP:<id>:*). */
    fun purgeProfile(id: String) {
        val pre = "davidlgP:$id:"
        val e = sp.edit()
        for (k in sp.all.keys) if (k.startsWith(pre)) e.remove(k)
        e.apply()
    }

    fun setActiveProfile(id: String) { saveProfiles(profiles(), id) }

    // ---------- Acceso a claves (con separación por perfil) ----------
    private fun key(k: String) =
        if (k in PER_PROFILE && activeProfileId != "p1") "davidlgP:$activeProfileId:$k" else k

    fun getString(k: String, def: String = ""): String = sp.getString(key(k), def) ?: def
    fun putString(k: String, v: String) { sp.edit().putString(key(k), v).apply() }
    fun remove(k: String) { sp.edit().remove(key(k)).apply() }
    fun getKv(k: String): JsonKv = JsonKv(getString(k, "{}").ifBlank { "{}" })
    fun putKv(k: String, kv: JsonKv) = putString(k, kv.raw)

    // ---------- Listas (globales, no dependen del perfil) ----------
    fun playlists(): List<Playlist> = try {
        val a = JSONArray(sp.getString("iptvPlaylists", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Playlist(o.getString("id"), o.optString("name", "Lista"), o.getString("m3uUrl"), o.optString("epgUrl", ""), o.optString("contentType", "mixed"))
        }
    } catch (_: Exception) { emptyList() }

    fun savePlaylists(list: List<Playlist>) {
        val a = JSONArray()
        for (p in list) a.put(JSONObject().put("id", p.id).put("name", p.name).put("m3uUrl", p.m3uUrl).put("epgUrl", p.epgUrl).put("contentType", p.contentType))
        sp.edit().putString("iptvPlaylists", a.toString()).apply()
    }

    fun activeIds(): List<String> = try {
        val a = JSONArray(sp.getString("activeIptvPlaylists", "[]"))
        (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun saveActiveIds(ids: List<String>) { sp.edit().putString("activeIptvPlaylists", JSONArray(ids).toString()).apply() }

    fun sourceFilter(): String = sp.getString("iptvHomeSourceFilter", "all") ?: "all"
    fun saveSourceFilter(v: String) { sp.edit().putString("iptvHomeSourceFilter", v).apply() }

    // ---------- Recientes (por perfil) ----------
    fun recents(): List<String> = try {
        val a = JSONArray(getString("recentChannels", "[]"))
        (0 until a.length()).map { a.getString(it) }
    } catch (_: Exception) { emptyList() }

    fun saveRecents(l: List<String>) = putString("recentChannels", JSONArray(l).toString())

    fun clearAllSources() { sp.edit().remove("iptvPlaylists").remove("activeIptvPlaylists").remove("iptvHomeSourceFilter").apply() }
}
