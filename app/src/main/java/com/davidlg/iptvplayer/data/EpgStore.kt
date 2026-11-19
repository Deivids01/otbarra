package com.davidlg.iptvplayer.data

import android.util.Xml
import kotlinx.coroutines.flow.MutableStateFlow
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.URL
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object EpgStore {
    @Volatile private var programs: Map<String, List<Program>> = emptyMap()
    @Volatile private var names: Map<String, String> = emptyMap()
    val version = MutableStateFlow(0)

    private val MARKS = Regex("""\p{M}+""")
    private val BRACKETS = Regex("""[\[(].*?[\])]""")
    private val QUALITY = Regex("""\b(uhd|fhd|hd|sd|4k|hevc|h265|h264|hq|lq)\b""")
    private val NON_ALNUM = Regex("""[^a-z0-9]+""")

    fun norm(s: String): String {
        var x = Normalizer.normalize(s, Normalizer.Form.NFD).replace(MARKS, "").lowercase()
        x = x.replace(BRACKETS, " ").replace(QUALITY, " ").replace(NON_ALNUM, "")
        return x
    }

    class Parsed(val programs: HashMap<String, ArrayList<Program>>, val names: HashMap<String, String>)

    fun parse(stream: InputStream, into: Parsed = Parsed(HashMap(), HashMap()), baseUrl: String = ""): Parsed {
        val now = System.currentTimeMillis()
        val lo = now - 3 * 3600_000L
        val hi = now + 48 * 3600_000L
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val p = Xml.newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(stream, null)
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                when (p.name) {
                    "channel" -> {
                        val id = p.getAttributeValue(null, "id") ?: ""
                        while (true) {
                            ev = p.next()
                            if (ev == XmlPullParser.END_DOCUMENT) break
                            if (ev == XmlPullParser.END_TAG && p.name == "channel") break
                            if (ev == XmlPullParser.START_TAG && p.name == "display-name") {
                                val k = norm(p.nextText())
                                if (k.isNotEmpty() && id.isNotEmpty() && !into.names.containsKey(k)) into.names[k] = id
                            }
                        }
                    }
                    "programme" -> {
                        val ch = p.getAttributeValue(null, "channel") ?: ""
                        val s = parseTime(p.getAttributeValue(null, "start") ?: "", sdf)
                        val e = parseTime(p.getAttributeValue(null, "stop") ?: "", sdf)
                        var title = ""
                        var desc = ""
                        var icon = ""
                        while (true) {
                            ev = p.next()
                            if (ev == XmlPullParser.END_DOCUMENT) break
                            if (ev == XmlPullParser.END_TAG && p.name == "programme") break
                            if (ev == XmlPullParser.START_TAG) {
                                if (p.name == "title" && title.isEmpty()) title = p.nextText().trim()
                                else if (p.name == "desc" && desc.isEmpty()) desc = p.nextText().trim()
                                else if (p.name == "icon" && icon.isEmpty()) {
                                    val rawIcon = p.getAttributeValue(null, "src")?.trim().orEmpty()
                                    icon = if (rawIcon.isBlank() || baseUrl.isBlank()) rawIcon else try { URL(URL(baseUrl), rawIcon).toString() } catch (_: Exception) { rawIcon }
                                }
                            }
                        }
                        if (ch.isNotEmpty() && title.isNotEmpty() && !(e != 0L && e < lo) && !(s != 0L && s > hi)) {
                            into.programs.getOrPut(ch) { ArrayList() }.add(Program(title, desc.take(420), s, e, icon))
                        }
                    }
                }
            }
            ev = p.next()
        }
        return into
    }

    private fun parseTime(s: String, sdf: SimpleDateFormat): Long {
        val t = s.trim()
        if (t.length < 14) return 0L
        val base = try { sdf.parse(t.substring(0, 14))?.time ?: return 0L } catch (_: Exception) { return 0L }
        val rest = t.substring(14).trim()
        var off = 0L
        if (rest.length >= 3 && (rest[0] == '+' || rest[0] == '-')) {
            val sign = if (rest[0] == '-') -1 else 1
            val digits = rest.substring(1).replace(":", "")
            val hh = digits.take(2).toIntOrNull() ?: 0
            val mm = digits.drop(2).take(2).toIntOrNull() ?: 0
            off = sign * (hh * 3600_000L + mm * 60_000L)
        }
        return base - off
    }

    fun publish(parsed: Parsed) {
        val sorted = HashMap<String, List<Program>>()
        for ((k, v) in parsed.programs) {
            v.sortBy { it.start }
            sorted[k] = v
        }
        programs = sorted
        names = parsed.names
        version.value = version.value + 1
    }

    fun idFor(c: Channel): String? {
        if (programs.isEmpty()) return null
        if (c.tvgId.isNotEmpty() && programs.containsKey(c.tvgId)) return c.tvgId
        for (k in listOf(c.tvgName, c.name, c.tvgId)) {
            val nk = norm(k)
            if (nk.isNotEmpty()) {
                val id = names[nk]
                if (id != null && programs.containsKey(id)) return id
            }
        }
        return null
    }

    fun now(c: Channel): Program? {
        val id = idFor(c) ?: return null
        val t = System.currentTimeMillis()
        val list = programs[id] ?: return null
        
        return list.firstOrNull { prog ->
            val stopTime = if (prog.stop > 0L) prog.stop else (prog.start + 3600_000L)
            t >= prog.start && t < stopTime
        }
    }

    fun next(c: Channel): Program? {
        val id = idFor(c) ?: return null
        val t = System.currentTimeMillis()
        return programs[id]?.firstOrNull { it.start > t }
    }

    /** Programación ordenada para construir la cuadrícula EPG. */
    fun schedule(c: Channel, from: Long, until: Long): List<Program> {
        val id = idFor(c) ?: return emptyList()
        return programs[id].orEmpty().filter { it.stop > from && it.start < until }.sortedBy { it.start }
    }

    fun progress(p: Program?): Float {
        if (p == null) return 0f
        val d = p.stop - p.start
        if (d <= 0L) return 0f
        return ((System.currentTimeMillis() - p.start).toFloat() / d).coerceIn(0f, 1f)
    }
}
