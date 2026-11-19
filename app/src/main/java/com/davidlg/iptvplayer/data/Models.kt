package com.davidlg.iptvplayer.data

/** contentType igual que en la versión web: "mixed" (TV + VOD), "tv" (solo TV en vivo) o "vod" (solo VOD). */
data class Playlist(
    val id: String,
    val name: String,
    val m3uUrl: String,
    val epgUrl: String = "",
    val contentType: String = "mixed",
)

data class Channel(
    val id: String,
    val number: Int,
    val name: String,
    val tvgId: String,
    val tvgName: String,
    val group: String,
    val logo: String,
    val url: String,
    val props: Map<String, String>,
    val isVod: Boolean,
    val sourceId: String,
    val sourceName: String = "",
)

data class Program(
    val title: String,
    val desc: String,
    val start: Long,
    val stop: Long,
    val icon: String = "",
)

data class Profile(val id: String, val name: String, val av: Int)


/**
 * Metadata presentation helpers. The M3U remains a source of playback URLs, not
 * the owner of the user's editorial catalogue. Local overrides are stored by
 * MainViewModel and take precedence over these source attributes.
 */
fun Channel.sourceMeta(vararg keys: String): String {
    for (key in keys) {
        val value = props.entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
        if (!value.isNullOrBlank()) return value.trim()
    }
    return ""
}

fun Channel.sourceSynopsis(): String = sourceMeta(
    "description", "plot", "synopsis", "summary", "desc", "tvg-description", "description_es", "overview"
)
fun Channel.sourcePoster(): String = sourceMeta("poster", "cover", "tvg-logo", "logo", "image")
fun Channel.sourceBackdrop(): String = sourceMeta("backdrop", "fanart", "background", "tvg-background", "backdrop_path")
fun Channel.sourceYear(): String = sourceMeta("year", "release-year", "releasedate", "date")
fun Channel.sourceGenre(): String = sourceMeta("genre", "genres", "category")
fun Channel.sourceDuration(): String = sourceMeta("duration", "runtime", "length")
fun Channel.sourceRating(): String = sourceMeta("rating", "imdb-rating", "score", "vote_average")
