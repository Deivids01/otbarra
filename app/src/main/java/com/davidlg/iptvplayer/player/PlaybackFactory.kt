@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.davidlg.iptvplayer.player

import android.content.Context
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.davidlg.iptvplayer.data.Channel
import com.davidlg.iptvplayer.data.IptvRepository
import java.net.URLDecoder

/** Crea el reproductor y prepara cada canal (HLS, DASH/MPD, TS, ClearKey y Widevine). */
object PlaybackFactory {

    fun newPlayer(ctx: Context, bufferMode: String = "default", bitrateMode: String = "auto"): ExoPlayer {
        // "low" reduce el búfer adicional al mínimo práctico; ningún reproductor puede garantizar cero búfer.
        val durations = when (bufferMode) {
            "low" -> intArrayOf(1_000, 3_000, 250, 500)
            "high" -> intArrayOf(30_000, 90_000, 2_000, 5_000)
            else -> intArrayOf(15_000, 50_000, 1_500, 3_000)
        }
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(durations[0], durations[1], durations[2], durations[3])
            .build()
        val selector = DefaultTrackSelector(ctx).apply {
            parameters = buildUponParameters().apply {
                when (bitrateMode) {
                    "low" -> setForceLowestBitrate(true)
                    "high" -> setForceHighestSupportedBitrate(true)
                }
            }.build()
        }
        return ExoPlayer.Builder(ctx).setTrackSelector(selector).setLoadControl(load).build().apply { playWhenReady = true }
    }

    // ---------- Utilidades DRM (equivalentes a player.js de la versión web) ----------
    private fun normHex(s: String): String =
        s.trim().removePrefix("0x").removePrefix("0X").filter { it in "0123456789abcdefABCDEF" }.lowercase()

    private fun b64uToHex(s: String): String = try {
        val bytes = Base64.decode(s.trim(), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        bytes.joinToString("") { "%02x".format(it) }
    } catch (_: Exception) { "" }

    private fun keyToHex(s: String): String {
        val h = normHex(s)
        return if (h.length == 32) h else b64uToHex(s).ifEmpty { h }
    }

    private fun hexToB64u(hex: String): String {
        val b = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun licenseType(c: Channel): String =
        (c.props["inputstream.adaptive.license_type"] ?: c.props["license_type"] ?: "").lowercase()

    private fun licenseValue(c: Channel): String =
        (c.props["inputstream.adaptive.license_key"] ?: c.props["license_key"]
            ?: c.props["clearkey"] ?: c.props["clear_key"] ?: "").trim()

    /** Pares kid:key en hexadecimal (admite varios separados por coma). */
    private fun clearKeys(c: Channel): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        val v = licenseValue(c)
        if (v.isNotEmpty() && !v.startsWith("http", ignoreCase = true)) {
            for (part in v.split(',')) {
                val i = part.indexOf(':')
                if (i <= 0) continue
                val kid = keyToHex(part.substring(0, i))
                val key = keyToHex(part.substring(i + 1))
                if (kid.length == 32 && key.length == 32) out.add(kid to key)
            }
        }
        if (out.isEmpty()) {
            val kid = c.props["kid"]
            val key = c.props["key"]
            if (kid != null && key != null) {
                val a = keyToHex(kid)
                val b = keyToHex(key)
                if (a.length == 32 && b.length == 32) out.add(a to b)
            }
        }
        return out
    }

    private fun parseHeaderString(s: String): Map<String, String> {
        val m = LinkedHashMap<String, String>()
        for (part in s.split('&')) {
            val z = part.indexOf('=')
            if (z <= 0) continue
            val k = part.substring(0, z).trim()
            val v = try { URLDecoder.decode(part.substring(z + 1), "UTF-8") } catch (_: Exception) { part.substring(z + 1) }
            m[k] = v
        }
        return m
    }

    private fun requestHeaders(c: Channel): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        c.props["inputstream.adaptive.stream_headers"]?.let { h.putAll(parseHeaderString(it)) }
        c.props["referer"]?.takeIf { it.isNotBlank() }?.let { h["Referer"] = it }
        c.props["origin"]?.takeIf { it.isNotBlank() }?.let { h["Origin"] = it }
        h.remove("User-Agent")
        return h
    }

    private fun isMpd(c: Channel): Boolean =
        Regex("""\.mpd(?:[?#]|$)""", RegexOption.IGNORE_CASE).containsMatchIn(c.url) ||
            (c.props["inputstream.adaptive.manifest_type"] ?: "").contains("mpd", ignoreCase = true) ||
            c.props.containsKey("inputstream.adaptive.license_type")

    private fun isHls(c: Channel): Boolean =
        Regex("""\.m3u8(?:[?#]|$)""", RegexOption.IGNORE_CASE).containsMatchIn(c.url) ||
            (c.props["inputstream.adaptive.manifest_type"] ?: "").contains("hls", ignoreCase = true)

    /** Prepara y reproduce el canal en el reproductor dado. */
    fun play(ctx: Context, player: ExoPlayer, c: Channel, startMs: Long = 0L) {
        val ua = c.props["user_agent"]?.takeIf { it.isNotBlank() } ?: IptvRepository.UA
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setDefaultRequestProperties(requestHeaders(c))
        val factory = DefaultMediaSourceFactory(ctx).setDataSourceFactory(DefaultDataSource.Factory(ctx, http))

        val item = MediaItem.Builder().setUri(c.url)
        if (isMpd(c)) item.setMimeType(MimeTypes.APPLICATION_MPD)
        else if (isHls(c)) item.setMimeType(MimeTypes.APPLICATION_M3U8)

        val type = licenseType(c)
        val value = licenseValue(c)
        val keys = clearKeys(c)

        if (type.contains("widevine") || type == "com.widevine.alpha") {
            // Formato Kodi: URL|Cabeceras|Cuerpo|Respuesta
            val parts = value.split('|')
            val server = parts.firstOrNull().orEmpty()
            if (server.startsWith("http", ignoreCase = true)) {
                val hdr = if (parts.size > 1) parseHeaderString(parts[1]) else emptyMap()
                val licenseHeaders = LinkedHashMap<String, String>().apply {
                    putAll(hdr)
                    putAll(requestHeaders(c))
                    c.props["user_agent"]?.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
                }
                item.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(server)
                        .setLicenseRequestHeaders(licenseHeaders)
                        .setMultiSession(true)
                        .build()
                )
            }
        } else if (keys.isNotEmpty()) {
            // ClearKey con claves fijas: se responde localmente con un JWK.
            val jwk = keys.joinToString(",") { (kid, key) ->
                """{"kty":"oct","k":"${hexToB64u(key)}","kid":"${hexToB64u(kid)}"}"""
            }
            val json = """{"keys":[$jwk],"type":"temporary"}"""
            item.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).build())
            val mgr = DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(LocalMediaDrmCallback(json.toByteArray()))
            factory.setDrmSessionManagerProvider(DrmSessionManagerProvider { _ -> mgr })
        } else if (type.contains("clearkey") && value.startsWith("http", ignoreCase = true)) {
            item.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.CLEARKEY_UUID).setLicenseUri(value).build()
            )
        }

        val source = factory.createMediaSource(item.build())
        // Reemplazar directamente el MediaSource evita el stop() adicional y reduce el hueco entre canales.
        if (startMs > 0) player.setMediaSource(source, startMs) else player.setMediaSource(source)
        player.prepare()
        player.playWhenReady = true
    }
}
