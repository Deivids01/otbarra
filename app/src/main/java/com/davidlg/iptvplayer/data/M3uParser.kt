package com.davidlg.iptvplayer.data

import org.json.JSONObject
import java.net.URLDecoder
import java.text.Normalizer

/** Port del parser M3U de la versión web (KODIPROP, EXTVLCOPT, EXTHTTP, cabeceras con |). */
object M3uParser {
    class Result(val channels: List<Channel>, val urlTvg: String)

    private val ATTR = Regex("""([A-Za-z0-9_-]+)=(?:"([^"]*)"|'([^']*)')""")
    private val TVG_URL = Regex("""(?:url-tvg|x-tvg-url|tvg-url)=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val VOD_URL = Regex("""(^|/)(movie|movies|film|films|vod|series|serie|episod|season)(/|\?|$)""")
    private val VOD_EXT = Regex("""\.(mp4|mkv|avi|mov|wmv|flv|webm)(\?|$)""")
    private val VOD_NAME = Regex("""\b(s\d{1,2}e\d{1,3}|temporada\s*\d+|season\s*\d+|episodio\s*\d+)\b""")
    private val MARKS = Regex("""\p{M}+""")

    private class Draft(
        var name: String, var tvgId: String, var tvgName: String,
        var group: String, var logo: String,
    ) {
        val props = HashMap<String, String>()
    }

    private fun norm(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(MARKS, "").lowercase()

    private fun isVodEntry(name: String, url: String, kind: String): Boolean {
        if (kind == "tv") return false
        if (kind == "vod") return true
        val n = norm(name)
        val u = norm(url)
        return VOD_URL.containsMatchIn(u) || VOD_EXT.containsMatchIn(u) || VOD_NAME.containsMatchIn(n)
    }

    private fun firstNonBlank(vararg v: String?): String = v.firstOrNull { !it.isNullOrBlank() } ?: ""

    fun parse(text: String, sourceId: String, kind: String): Result {
        val out = ArrayList<Channel>()
        var tvg = ""
        var d: Draft? = null

        for (raw in text.lineSequence()) {
            val l = raw.trim()
            if (l.isEmpty()) continue
            val cur = d

            if (l.startsWith("#EXTM3U", ignoreCase = true)) {
                val m = TVG_URL.find(l)
                if (m != null) tvg = m.groupValues[1].trim()
            } else if (l.startsWith("#EXTINF")) {
                val c = l.indexOf(',')
                val info = if (c >= 0) l.substring(0, c) else l
                val name = if (c >= 0) l.substring(c + 1).trim() else "Canal"
                val attrs = HashMap<String, String>()
                for (m in ATTR.findAll(info)) {
                    attrs[m.groupValues[1]] = firstNonBlank(m.groupValues[2], m.groupValues[3])
                }
                d = Draft(
                    name = name.ifBlank { "Canal" },
                    tvgId = attrs["tvg-id"] ?: "",
                    tvgName = attrs["tvg-name"] ?: "",
                    group = firstNonBlank(attrs["group-title"], attrs["group"]).ifBlank { "General" },
                    logo = firstNonBlank(attrs["tvg-logo"], attrs["poster"], attrs["cover"]),
                ).also { draft ->
                    // Preserve EXTINF metadata. Playback URL and catalogue metadata stay separate.
                    attrs.forEach { (key, value) -> draft.props[key.lowercase()] = value }
                }
            } else if (cur != null && l.startsWith("#EXTGRP:")) {
                val g = l.substring(8).trim()
                if (g.isNotEmpty() && (cur.group.isBlank() || cur.group == "General")) cur.group = g
            } else if (cur != null && l.startsWith("#EXTHTTP:")) {
                try {
                    val h = JSONObject(l.substring(9))
                    for (k in h.keys()) {
                        val v = h.optString(k)
                        when (k.lowercase()) {
                            "user-agent" -> cur.props["user_agent"] = v
                            "referer", "referrer" -> cur.props["referer"] = v
                            "origin" -> cur.props["origin"] = v
                        }
                    }
                } catch (_: Exception) {}
            } else if (cur != null && l.startsWith("#KODIPROP:")) {
                val q = l.substring(10)
                val eq = q.indexOf('=')
                if (eq > 0) cur.props[q.substring(0, eq).trim().lowercase()] = q.substring(eq + 1).trim()
            } else if (cur != null && l.startsWith("#EXTVLCOPT:")) {
                val q = l.substring(11)
                val eq = q.indexOf('=')
                if (eq > 0) {
                    val k = q.substring(0, eq).trim().lowercase()
                    val v = q.substring(eq + 1).trim()
                    when (k) {
                        "http-referrer", "http-referer" -> cur.props["referer"] = v
                        "http-user-agent" -> cur.props["user_agent"] = v
                        "http-origin", "origin" -> cur.props["origin"] = v
                    }
                }
            } else if (cur != null && !l.startsWith("#")) {
                var u = l
                val bar = u.indexOf('|')
                if (bar > 0 && Regex("^(user-agent|referer|referrer|origin|cookie)=", RegexOption.IGNORE_CASE)
                        .containsMatchIn(u.substring(bar + 1))
                ) {
                    for (part in u.substring(bar + 1).split('&')) {
                        val z = part.indexOf('=')
                        if (z <= 0) continue
                        val k = part.substring(0, z).lowercase()
                        val v = try { URLDecoder.decode(part.substring(z + 1), "UTF-8") } catch (_: Exception) { part.substring(z + 1) }
                        when (k) {
                            "user-agent" -> cur.props["user_agent"] = v
                            "referer", "referrer" -> cur.props["referer"] = v
                            "origin" -> cur.props["origin"] = v
                        }
                    }
                    u = u.substring(0, bar)
                }
                out.add(
                    Channel(
                        id = sourceId + "-" + (u + cur.name).hashCode(),
                        number = 0,
                        name = cur.name,
                        tvgId = cur.tvgId,
                        tvgName = cur.tvgName,
                        group = cur.group,
                        logo = cur.logo,
                        url = u,
                        props = cur.props,
                        isVod = isVodEntry(cur.name, u, kind),
                        sourceId = sourceId,
                    )
                )
                d = null
            }
        }
        return Result(out, tvg)
    }
}
