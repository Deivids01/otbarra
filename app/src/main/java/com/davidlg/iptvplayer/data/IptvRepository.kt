package com.davidlg.iptvplayer.data

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

class IptvRepository(private val ctx: Context) {
    
    // ---------- Red ----------
    private fun download(url: String, dest: File) {
        var u = url.trim()
        var redirects = 0
        while (true) {
            val conn = URL(u).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Accept", "*/*")
            val code = conn.responseCode
            if (code in 300..399 && redirects < 6) {
                val loc = conn.getHeaderField("Location") ?: throw IOException("Redirección sin destino")
                u = URL(URL(u), loc).toString()
                conn.disconnect()
                redirects++
                continue
            }
            if (code !in 200..299) {
                conn.disconnect()
                throw IOException("HTTP $code")
            }
            val tmp = File(dest.parentFile, dest.name + ".tmp")
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            conn.disconnect()
            if (tmp.length() == 0L) { tmp.delete(); throw IOException("Respuesta vacía") }
            dest.delete()
            tmp.renameTo(dest)
            return
        }
    }

    private fun openMaybeGz(f: File): InputStream {
        val b = BufferedInputStream(f.inputStream(), 64 * 1024)
        b.mark(2)
        val a = b.read()
        val c = b.read()
        b.reset()
        return if (a == 0x1f && c == 0x8b) GZIPInputStream(b, 64 * 1024) else b
    }

    private fun dir(): File = File(ctx.filesDir, "cache").apply { mkdirs() }

    /** Carga la lista desde caché si es reciente; si no, descarga. Si la red falla, usa la caché. */
    fun loadPlaylist(pl: Playlist, force: Boolean): M3uParser.Result {
        val f = File(dir(), "m3u_${pl.id}.txt")
        val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < 6 * 3600_000L
        if (force || !fresh) {
            try { download(pl.m3uUrl, f) } catch (e: Exception) { if (!f.exists()) throw e }
        }
        val text = openMaybeGz(f).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val kind = when (pl.contentType) { "tv" -> "tv"; "vod" -> "vod"; else -> "both" }
        return M3uParser.parse(text, pl.id, kind)
    }

    /** Descarga y fusiona las guías EPG de todas las listas. */
    fun loadEpg(urls: List<Pair<String, String>>, force: Boolean) {
        val parsed = EpgStore.Parsed(HashMap(), HashMap())
        var any = false
        for ((key, raw) in urls) {
            val list = raw.split(Regex("""[\s,]+(?=https?://)""")).map { it.trim() }.filter { it.isNotEmpty() }
            for ((i, u) in list.withIndex()) {
                try {
                    val f = File(dir(), "epg_${key}_$i.xml")
                    val fresh = f.exists() && System.currentTimeMillis() - f.lastModified() < 4 * 3600_000L
                    if (force || !fresh) {
                        try { download(u, f) } catch (e: Exception) { if (!f.exists()) throw e }
                    }
                    openMaybeGz(f).use { EpgStore.parse(it, parsed, u) }
                    any = true
                } catch (_: Exception) { /* una guía caída no debe romper el resto */ }
            }
        }
        if (any) EpgStore.publish(parsed)
    }

    fun clearCache() { dir().listFiles()?.forEach { it.delete() } }

    companion object {
        const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
    }
}
