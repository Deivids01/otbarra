package com.davidlg.iptvplayer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davidlg.iptvplayer.data.AppPrefs
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.EpgStore
import com.davidlg.iptvplayer.data.IptvRepository
import com.davidlg.iptvplayer.data.JsonKv
import com.davidlg.iptvplayer.data.Playlist
import com.davidlg.iptvplayer.data.Profile
import com.davidlg.iptvplayer.data.TmdbRepository
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Pantallas de nivel superior. Main = OttShell o ClassicShell según el estilo del perfil. */
enum class Screen { Profiles, Main, Settings, Player }

/** Pestañas de la barra superior del estilo OTT (igual que .ottTab de la versión web). */
enum class OttTab { Start, Movie, Series, Anime, Live, Epg, Fav }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val prefs = AppPrefs(app)
    private val repo = IptvRepository(app)

    // ---------------- Navegación ----------------
    var screen by mutableStateOf(Screen.Profiles)
    /** "launch" = al abrir la app (Atrás sale); "switch" = cambio de perfil desde dentro (Atrás vuelve). */
    var profileMode by mutableStateOf("launch")
    var ottTab by mutableStateOf(OttTab.Start)
    var pendingVodDetailsId by mutableStateOf<String?>(null)
        private set
    var returnTo: Screen = Screen.Main

    fun openVodDetails(channel: Channel) {
        pendingVodDetailsId = channel.id
        ottTab = when {
            channel.group.contains("anime", true) -> OttTab.Anime
            channel.group.contains("serie", true) || channel.url.contains("/series/", true) ||
                Regex("\\bs\\d{1,2}\\s?e\\d{1,3}\\b|temporada|season|episodio", RegexOption.IGNORE_CASE).containsMatchIn(channel.name) -> OttTab.Series
            else -> OttTab.Movie
        }
    }
    fun consumePendingVodDetails() { pendingVodDetailsId = null }

    // ---------------- Perfiles ----------------
    val profiles = MutableStateFlow(prefs.profiles())
    val activeProfile = MutableStateFlow(profiles.value.first { it.id == prefs.activeProfileId })

    fun showProfileSwitcher() { profileMode = "switch"; screen = Screen.Profiles }

    fun selectProfile(id: String) {
        prefs.setActiveProfile(id)
        activeProfile.value = profiles.value.first { it.id == id }
        refreshProfileState()
        ottTab = OttTab.Start
        screen = Screen.Main
    }

    fun addProfile(name: String, av: Int): Boolean {
        if (profiles.value.size >= AppPrefs.MAX_PROFILES) return false
        val id = "p" + UUID.randomUUID().toString().replace("-", "").take(12)
        profiles.value = profiles.value + Profile(id, name.ifBlank { "Perfil" }, av)
        prefs.saveProfiles(profiles.value, prefs.activeProfileId)
        return true
    }

    fun editProfile(id: String, name: String, av: Int) {
        profiles.value = profiles.value.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }, av = av) else it }
        prefs.saveProfiles(profiles.value, prefs.activeProfileId)
        if (activeProfile.value.id == id) activeProfile.value = profiles.value.first { it.id == id }
    }

    /** No se puede eliminar el último perfil. */
    fun deleteProfile(id: String): Boolean {
        if (profiles.value.size <= 1) return false
        prefs.purgeProfile(id)
        val left = profiles.value.filter { it.id != id }
        profiles.value = left
        val act = if (prefs.activeProfileId == id) left[0].id else prefs.activeProfileId
        prefs.saveProfiles(left, act)
        activeProfile.value = left.first { it.id == act }
        refreshProfileState()
        return true
    }

    // ---------------- Ajustes por perfil (mismas claves que la web) ----------------
    /** davidlgVodSettings: style ("ott"|"classic"), theme, fastZap ("on"|"off"), titles, tmdbKey, sort... */
    val vodSettings = MutableStateFlow(prefs.getKv("davidlgVodSettings"))
    /** playbackPrefs: clock, audio, subs, quality, fps, channelQuality. */
    val playbackPrefs = MutableStateFlow(prefs.getKv("playbackPrefs"))
    val recents = MutableStateFlow(prefs.recents())

    val isClassic: Boolean get() = vodSettings.value.str("style", "ott") == "classic"

    fun putVodSetting(k: String, v: Any?) {
        val n = vodSettings.value.with(k, v)
        prefs.putKv("davidlgVodSettings", n); vodSettings.value = n
    }

    fun putPlaybackPref(k: String, v: Any?) {
        val n = playbackPrefs.value.with(k, v)
        prefs.putKv("playbackPrefs", n); playbackPrefs.value = n
    }

    private fun refreshProfileState() {
        vodSettings.value = prefs.getKv("davidlgVodSettings")
        playbackPrefs.value = prefs.getKv("playbackPrefs")
        recents.value = prefs.recents()
        vodMetadataVersion++
    }

    // ---------------- Listas y canales ----------------
    val playlists = MutableStateFlow(prefs.playlists())
    val activeIds = MutableStateFlow(prefs.activeIds().ifEmpty { prefs.playlists().take(1).map { it.id } })
    /** "all" o el id de una lista (filtro de fuente del inicio). */
    val sourceFilter = MutableStateFlow(prefs.sourceFilter())

    /** Todos los canales en vivo / VOD de las listas activas (sin filtro de fuente). */
    val allLive = MutableStateFlow<List<Channel>>(emptyList())
    val allVod = MutableStateFlow<List<Channel>>(emptyList())
    /** Con el filtro de fuente aplicado. Los números de canal se recalculan sobre esta lista. */
    val live = MutableStateFlow<List<Channel>>(emptyList())
    val vod = MutableStateFlow<List<Channel>>(emptyList())

    val status = MutableStateFlow("")
    val errors = MutableStateFlow<List<String>>(emptyList())
    val epgVersion: StateFlow<Int> = EpgStore.version

    private var byId: Map<String, Channel> = emptyMap()
    private val tvgByPlaylist = HashMap<String, String>()

    init { reload(false) }

    fun reload(force: Boolean) {
        viewModelScope.launch {
            val all = playlists.value
            val act = activeIds.value.filter { id -> all.any { it.id == id } }.ifEmpty { all.take(1).map { it.id } }
            val pls = all.filter { it.id in act }
            if (pls.isEmpty()) {
                allLive.value = emptyList(); allVod.value = emptyList(); applyFilter(); return@launch
            }
            status.value = "Cargando listas…"
            val errs = ArrayList<String>()
            val liveAcc = ArrayList<Channel>()
            val vodAcc = ArrayList<Channel>()
            for (pl in pls) {
                try {
                    val r = withContext(Dispatchers.IO) { repo.loadPlaylist(pl, force) }
                    tvgByPlaylist[pl.id] = r.urlTvg
                    for (c0 in r.channels) {
                        val c = c0.copy(sourceName = pl.name)
                        if (c.isVod) vodAcc.add(c) else liveAcc.add(c)
                    }
                } catch (e: Exception) {
                    errs.add("${pl.name}: ${e.message ?: e.javaClass.simpleName}")
                }
            }
            allLive.value = liveAcc
            allVod.value = vodAcc
            errors.value = errs
            applyFilter()
            status.value = ""
            loadEpg(force, pls)
        }
    }

    private fun applyFilter() {
        val f = sourceFilter.value
        val l = if (f == "all") allLive.value else allLive.value.filter { it.sourceId == f }
        val v = if (f == "all") allVod.value else allVod.value.filter { it.sourceId == f }
        live.value = l.mapIndexed { i, c -> c.copy(number = i + 1) }
        vod.value = v.mapIndexed { i, c -> c.copy(number = i + 1) }
        byId = (live.value + vod.value).associateBy { it.id }
    }

    fun setSourceFilter(id: String) {
        sourceFilter.value = id; prefs.saveSourceFilter(id); applyFilter()
    }

    fun cycleSource() {
        val opts = listOf("all") + activeIds.value
        val i = opts.indexOf(sourceFilter.value).coerceAtLeast(0)
        setSourceFilter(opts[(i + 1) % opts.size])
    }

    private fun loadEpg(force: Boolean, pls: List<Playlist>) {
        viewModelScope.launch {
            val urls = pls.mapNotNull { pl ->
                val u = pl.epgUrl.ifBlank { tvgByPlaylist[pl.id] ?: "" }
                if (u.isBlank()) null else pl.id to u
            }
            if (urls.isEmpty()) return@launch
            withContext(Dispatchers.IO) { repo.loadEpg(urls, force) }
        }
    }

    fun addPlaylist(p: Playlist) {
        playlists.value = playlists.value + p
        prefs.savePlaylists(playlists.value)
        if (activeIds.value.size < AppPrefs.MAX_PROFILES) { activeIds.value = activeIds.value + p.id; prefs.saveActiveIds(activeIds.value) }
        reload(true)
    }

    fun updatePlaylist(p: Playlist) {
        playlists.value = playlists.value.map { if (it.id == p.id) p else it }
        prefs.savePlaylists(playlists.value)
        reload(true)
    }

    fun removePlaylist(id: String) {
        playlists.value = playlists.value.filter { it.id != id }
        activeIds.value = activeIds.value.filter { it != id }
        prefs.savePlaylists(playlists.value); prefs.saveActiveIds(activeIds.value)
        if (sourceFilter.value == id) setSourceFilter("all")
        reload(false)
    }

    /** Activa/desactiva una lista (máximo 5 activas, como en la web). */
    fun toggleActive(id: String) {
        val cur = activeIds.value
        activeIds.value = if (id in cur) cur.filter { it != id } else (cur + id).take(5)
        prefs.saveActiveIds(activeIds.value)
        reload(false)
    }

    fun clearAllSources() {
        prefs.clearAllSources(); repo.clearCache()
        playlists.value = emptyList(); activeIds.value = emptyList(); sourceFilter.value = "all"
        reload(false)
    }

    fun clearCacheAndReload() { repo.clearCache(); reload(true) }

    // ---------------- Recientes ----------------
    fun markWatched(c: Channel) {
        if (c.isVod) return
        val l = recents.value.toMutableList()
        l.remove(c.id); l.add(0, c.id)
        recents.value = l.take(30); prefs.saveRecents(recents.value)
    }

    fun recentChannels(): List<Channel> = recents.value.mapNotNull { byId[it] }.filter { !it.isVod }
    fun channelById(id: String): Channel? = byId[id]

    // ---------------- Reproducción ----------------
    var current by mutableStateOf<Channel?>(null)
    /** Canal seleccionado en la vista OTT; persiste al cambiar de motor o recomponer pantallas. */
    var selectedLiveChannelId by mutableStateOf<String?>(null)
        private set
    fun selectLiveChannel(channel: Channel?) { selectedLiveChannelId = channel?.id }
    var queue: List<Channel> = emptyList()
    /** Posición inicial (ms) para reanudar VOD; la consume el reproductor. */
    var startPositionMs: Long = 0L

    fun play(c: Channel, from: List<Channel>, startMs: Long = 0L) {
        if (screen != Screen.Player) returnTo = screen
        current = c; queue = from; startPositionMs = startMs
        if (!c.isVod) selectedLiveChannelId = c.id
        screen = Screen.Player
    }

    fun zap(delta: Int) {
        val q = queue; val c = current ?: return
        if (q.isEmpty()) return
        val i = q.indexOfFirst { it.id == c.id }.let { if (it < 0) 0 else it }
        current = q[((i + delta) % q.size + q.size) % q.size]
        current?.takeIf { !it.isVod }?.let { selectedLiveChannelId = it.id }
    }

    fun closePlayer() { screen = returnTo }

    // ---------------- Catálogo editorial VOD independiente de M3U ----------------
    val tmdbLookupStatus = MutableStateFlow("")

    fun lookupVodOnTmdb(channel: Channel) {
        val apiKey = vodSettings.value.str("tmdbKey", "").trim()
        if (apiKey.isBlank()) {
            tmdbLookupStatus.value = "Configura tu clave de TMDB en Ajustes > Catálogo."
            return
        }
        viewModelScope.launch {
            tmdbLookupStatus.value = "Buscando información…"
            try {
                val found = withContext(Dispatchers.IO) { TmdbRepository.search(channel.name, apiKey) }
                if (found.isNullOrEmpty()) tmdbLookupStatus.value = "No se encontró una coincidencia para este título."
                else {
                    val existing = vodMetadata(channel.id)
                    val merged = linkedMapOf<String, String>()
                    listOf("synopsis", "poster", "backdrop", "year", "genre", "rating", "duration").forEach { key ->
                        val currentValue = existing.optString(key, "")
                        if (currentValue.isNotBlank()) merged[key] = currentValue
                    }
                    found.forEach { (key, value) -> if (merged[key].isNullOrBlank()) merged[key] = value }
                    saveVodMetadata(channel.id, merged)
                    tmdbLookupStatus.value = "Información de TMDB guardada. Puedes editarla cuando quieras."
                }
            } catch (e: Exception) {
                tmdbLookupStatus.value = "No se pudo consultar TMDB: ${e.message ?: "error de conexión"}"
            }
        }
    }

    var vodMetadataVersion by mutableStateOf(0)
        private set

    /** Devuelve los metadatos locales que el usuario haya editado para este título. */
    fun vodMetadata(id: String): org.json.JSONObject = try {
        org.json.JSONObject(prefs.getString("davidlgVodMetadata", "{}")).optJSONObject(id)
            ?: org.json.JSONObject()
    } catch (_: Exception) { org.json.JSONObject() }

    fun saveVodMetadata(id: String, fields: Map<String, String>) {
        val root = try { org.json.JSONObject(prefs.getString("davidlgVodMetadata", "{}")) }
            catch (_: Exception) { org.json.JSONObject() }
        val item = org.json.JSONObject()
        fields.forEach { (key, value) -> if (value.isNotBlank()) item.put(key, value.trim()) }
        root.put(id, item)
        prefs.putString("davidlgVodMetadata", root.toString())
        vodMetadataVersion++
    }

    // ---------------- Progreso VOD (davidlgVodProgress) ----------------
    fun vodProgress(id: String): Long = try { prefs.getKv("davidlgVodProgress").json().optLong(id, 0L) } catch (_: Exception) { 0L }
    fun saveVodProgress(id: String, posMs: Long, durMs: Long) {
        val o = prefs.getKv("davidlgVodProgress").json()
        if (durMs > 0 && posMs > durMs * 0.95) o.remove(id) else o.put(id, posMs)
        prefs.putString("davidlgVodProgress", o.toString())
    }
}
