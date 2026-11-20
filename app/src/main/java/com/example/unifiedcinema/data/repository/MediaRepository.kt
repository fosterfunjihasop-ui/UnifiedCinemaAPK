package com.example.unifiedcinema.data.repository

import android.content.Context
import com.example.unifiedcinema.data.model.AnimeItem
import com.example.unifiedcinema.data.model.DatabaseResponse
import com.example.unifiedcinema.data.model.Episode
import com.example.unifiedcinema.data.model.MovieItem
import com.example.unifiedcinema.data.model.SeriesItem
import com.example.unifiedcinema.data.model.VideoSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.InputStreamReader

class MediaRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    
    private var seriesCache: Map<String, SeriesItem>? = null
    private var moviesCache: Map<String, MovieItem>? = null
    private var animeCache: Map<String, AnimeItem>? = null

    suspend fun loadSeriesDatabase(): Map<String, SeriesItem> {
        return withContext(Dispatchers.IO) {
            if (seriesCache != null) return@withContext seriesCache!!
            try {
                val inputStream = context.assets.open("series_database.json")
                val reader = InputStreamReader(inputStream, "UTF-8")
                val jsonString = reader.readText()
                reader.close()
                val data = json.decodeFromString<Map<String, SeriesItem>>(jsonString)
                seriesCache = data
                data
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun loadMoviesDatabase(): Map<String, MovieItem> {
        return withContext(Dispatchers.IO) {
            if (moviesCache != null) return@withContext moviesCache!!
            try {
                val inputStream = context.assets.open("movies_database.json")
                val reader = InputStreamReader(inputStream, "UTF-8")
                val jsonString = reader.readText()
                reader.close()
                val data = json.decodeFromString<Map<String, MovieItem>>(jsonString)
                moviesCache = data
                data
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun loadAnimeDatabase(): Map<String, AnimeItem> {
        return withContext(Dispatchers.IO) {
            if (animeCache != null) return@withContext animeCache!!
            try {
                val inputStream = context.assets.open("anime_database.json")
                val reader = InputStreamReader(inputStream, "UTF-8")
                val jsonString = reader.readText()
                reader.close()
                val data = json.decodeFromString<Map<String, AnimeItem>>(jsonString)
                animeCache = data
                data
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun getSeries(id: String): SeriesItem? {
        val db = loadSeriesDatabase()
        return db[id]
    }

    suspend fun getSeriesByPostId(postId: String): SeriesItem? {
        val db = loadSeriesDatabase()
        return db.values.firstOrNull { it.post_id == postId }
    }

    suspend fun getMovie(id: String): MovieItem? {
        val db = loadMoviesDatabase()
        var movie = db[id]
        if (movie == null) {
            movie = db.values.firstOrNull { it.id.startsWith(id) || id in it.id }
        }
        if (movie == null) {
            movie = db.values.firstOrNull { it.post_id == id }
        }
        return movie
    }

    suspend fun getAnime(id: String): AnimeItem? {
        val db = loadAnimeDatabase()
        return db[id]
    }

    suspend fun getAnimeByPostId(postId: String): AnimeItem? {
        val db = loadAnimeDatabase()
        return db.values.firstOrNull { it.post_id == postId }
    }

    suspend fun searchSeries(query: String): List<SeriesItem> {
        val db = loadSeriesDatabase()
        val lowerQuery = query.lowercase()
        return db.values.filter { 
            it.title.lowercase().contains(lowerQuery) || 
            it.id.lowercase().contains(lowerQuery)
        }.toList()
    }

    suspend fun searchMovies(query: String): List<MovieItem> {
        val db = loadMoviesDatabase()
        val lowerQuery = query.lowercase()
        return db.values.filter { 
            it.title.lowercase().contains(lowerQuery) || 
            it.id.lowercase().contains(lowerQuery)
        }.toList()
    }

    suspend fun searchAnime(query: String): List<AnimeItem> {
        val db = loadAnimeDatabase()
        val lowerQuery = query.lowercase()
        return db.values.filter { 
            it.title.lowercase().contains(lowerQuery) || 
            it.id.lowercase().contains(lowerQuery)
        }.toList()
    }

    suspend fun getAllSeries(): List<SeriesItem> {
        return loadSeriesDatabase().values.toList()
    }

    suspend fun getAllMovies(): List<MovieItem> {
        return loadMoviesDatabase().values.toList()
    }

    suspend fun getAllAnime(): List<AnimeItem> {
        return loadAnimeDatabase().values.toList()
    }

    fun detectContentType(itemId: String?): ContentType? {
        if (itemId == null || itemId.isBlank()) return null
        
        val seriesDb = seriesCache ?: runBlocking { loadSeriesDatabase() }
        val moviesDb = moviesCache ?: runBlocking { loadMoviesDatabase() }
        val animeDb = animeCache ?: runBlocking { loadAnimeDatabase() }

        for (series in seriesDb.values) {
            if (series.id == itemId || series.post_id == itemId) {
                return ContentType.SERIES
            }
        }

        for (movie in moviesDb.values) {
            if (movie.id == itemId || movie.id.startsWith(itemId) || itemId in movie.id || movie.post_id == itemId) {
                return ContentType.MOVIES
            }
        }

        for (anime in animeDb.values) {
            if (anime.id == itemId || anime.post_id == itemId) {
                return ContentType.ANIME
            }
        }

        return null
    }

    fun getEpisodeSource(
        contentType: ContentType,
        itemId: String?,
        postId: String?,
        episode: String,
        language: String = "Thai"
    ): VideoSource? {
        return when (contentType) {
            ContentType.SERIES -> {
                val series = if (itemId != null) getSeries(itemId) else getSeriesByPostId(postId ?: "")
                val epObj = series?.episodes?.get(episode)
                val source = epObj?.sources?.get(language) ?: epObj?.sources?.values?.firstOrNull()
                source
            }
            ContentType.MOVIES -> {
                val movie = if (itemId != null) getMovie(itemId) else null
                val source = movie?.sources?.get("main") ?: movie?.sources?.get(language) 
                    ?: movie?.sources?.get("Thai") ?: movie?.sources?.get("พากย์ไทย") ?: movie?.sources?.get("ซับไทย")
                    ?: movie?.sources?.values?.firstOrNull()
                source
            }
            ContentType.ANIME -> {
                val anime = if (itemId != null) getAnime(itemId) else getAnimeByPostId(postId ?: "")
                val epObj = anime?.episodes?.get(episode)
                val source = epObj?.sources?.get(language) ?: epObj?.sources?.values?.firstOrNull()
                source
            }
        }
    }

    fun clearCache() {
        seriesCache = null
        moviesCache = null
        animeCache = null
    }

    companion object {
        @Volatile private var INSTANCE: MediaRepository? = null

        fun getInstance(context: Context): MediaRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MediaRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

enum class ContentType {
    SERIES, MOVIES, ANIME
}