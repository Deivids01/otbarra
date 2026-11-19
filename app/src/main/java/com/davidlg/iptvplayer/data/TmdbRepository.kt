package com.davidlg.iptvplayer.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/** Enriquecimiento opcional del catálogo con TMDB; nunca modifica la URL de reproducción. */
object TmdbRepository {
    private val genres = mapOf(
        28 to "Acción", 12 to "Aventura", 16 to "Animación", 35 to "Comedia", 80 to "Crimen",
        99 to "Documental", 18 to "Drama", 10751 to "Familiar", 14 to "Fantasía", 36 to "Historia",
        27 to "Terror", 10402 to "Música", 9648 to "Misterio", 10749 to "Romance", 878 to "Ciencia ficción",
        10770 to "Película de TV", 53 to "Suspenso", 10752 to "Bélica", 37 to "Western",
        10759 to "Acción y aventura", 10762 to "Infantil", 10763 to "Noticias", 10764 to "Reality",
        10765 to "Ciencia ficción y fantasía", 10766 to "Telenovela", 10767 to "Talk show", 10768 to "Bélica y política"
    )

    fun search(title: String, apiKey: String): Map<String, String>? {
        if (title.isBlank() || apiKey.isBlank()) return null
        val encoded = URLEncoder.encode(title, "UTF-8")
        val bearer = apiKey.startsWith("eyJ")
        val endpoint = if (bearer) {
            "https://api.themoviedb.org/3/search/multi?query=$encoded&language=es-MX&include_adult=false"
        } else {
            "https://api.themoviedb.org/3/search/multi?api_key=${URLEncoder.encode(apiKey, "UTF-8")}&query=$encoded&language=es-MX&include_adult=false"
        }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (bearer) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val results = JSONObject(body).optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val candidates = (0 until results.length()).map { results.getJSONObject(it) }
            val best = candidates.firstOrNull { it.optString("media_type") == "movie" || it.optString("media_type") == "tv" } ?: return null
            val overview = best.optString("overview", "").trim()
            val posterPath = best.optString("poster_path", "")
            val backdropPath = best.optString("backdrop_path", "")
            val date = best.optString("release_date").ifBlank { best.optString("first_air_date") }
            val genreIds = best.optJSONArray("genre_ids")
            val genreNames = if (genreIds == null) emptyList() else (0 until genreIds.length()).mapNotNull { genres[genreIds.optInt(it)] }
            buildMap {
                if (overview.isNotBlank()) put("synopsis", overview)
                if (posterPath.isNotBlank()) put("poster", "https://image.tmdb.org/t/p/w500$posterPath")
                if (backdropPath.isNotBlank()) put("backdrop", "https://image.tmdb.org/t/p/w1280$backdropPath")
                if (date.length >= 4) put("year", date.take(4))
                if (genreNames.isNotEmpty()) put("genre", genreNames.joinToString(", "))
                val rating = best.optDouble("vote_average", 0.0)
                if (rating > 0.0) put("rating", String.format(java.util.Locale.US, "%.1f/10", rating))
            }.takeIf { it.isNotEmpty() }
        } finally {
            connection.disconnect()
        }
    }
}
