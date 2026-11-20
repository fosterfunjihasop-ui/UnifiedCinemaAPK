package com.example.unifiedcinema.network

import com.example.unifiedcinema.data.model.Episode
import com.example.unifiedcinema.data.model.MovieItem
import com.example.unifiedcinema.data.model.SeriesItem
import com.example.unifiedcinema.data.model.VideoSource
import com.example.unifiedcinema.data.repository.ContentType
import com.example.unifiedcinema.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnifiedApiService(
    private val repository: MediaRepository,
    private val streamResolver: StreamResolver
) {

    data class ApiResolveRequest(
        val id: String? = null,
        val postId: String? = null,
        val episode: String? = null,
        val language: String = "Thai",
        val type: String? = null,
        val referer: String? = null
    )

    data class ApiResolveResponse(
        val success: Boolean,
        val type: String? = null,
        val stream: VideoSource? = null,
        val m3u8: String? = null,
        val masterPlaylist: String? = null,
        val error: String? = null
    )

    data class ApiStatusResponse(
        val status: String = "ok",
        val server: String = "UnifiedCinemaAPK",
        val databases: Map<String, Int>
    )

    data class ApiInfoResponse(
        val ip: String,
        val port: Int,
        val server: String = "UnifiedCinemaAPK",
        val databases: Map<String, Int>
    )

    suspend fun resolve(request: ApiResolveRequest): ApiResolveResponse {
        return withContext(Dispatchers.IO) {
            var contentType: ContentType? = null
            
            if (request.type != null) {
                contentType = when (request.type!!.lowercase()) {
                    "series" -> ContentType.SERIES
                    "movies", "movie" -> ContentType.MOVIES
                    "anime" -> ContentType.ANIME
                    else -> null
                }
            }

            if (contentType == null) {
                val itemId = request.id ?: request.postId
                if (itemId != null) {
                    contentType = repository.detectContentType(itemId)
                }
            }

            if (contentType == null) {
                contentType = ContentType.SERIES
            }

            val resolverRequest = StreamResolver.ResolveRequest(
                id = request.id,
                postId = request.postId,
                episode = request.episode,
                language = request.language,
                contentType = contentType,
                referer = request.referer
            )

            val result = streamResolver.resolveStream(resolverRequest)
            
            if (result.success && result.stream != null) {
                val stream = result.stream!!
                var m3u8Url = stream.m3u8
                var masterPlaylistContent: String? = null

                if (stream.video_id != null) {
                    m3u8Url = stream.m3u8 ?: "${StreamResolver.getPlayerOrigin(contentType)}/newplaylist/${stream.video_id}/${stream.video_id}.m3u8"
                    masterPlaylistContent = streamResolver.fetchMasterPlaylist(stream.video_id!!, contentType)
                }

                ApiResolveResponse(
                    success = true,
                    type = contentType.name.lowercase(),
                    stream = stream,
                    m3u8 = m3u8Url,
                    masterPlaylist = masterPlaylistContent
                )
            } else {
                ApiResolveResponse(
                    success = false,
                    type = contentType.name.lowercase(),
                    error = result.error ?: "Unable to resolve stream"
                )
            }
        }
    }

    suspend fun getStatus(): ApiStatusResponse {
        return withContext(Dispatchers.IO) {
            val seriesCount = repository.loadSeriesDatabase().size
            val moviesCount = repository.loadMoviesDatabase().size
            val animeCount = repository.loadAnimeDatabase().size

            ApiStatusResponse(
                databases = mapOf(
                    "series" to seriesCount,
                    "movies" to moviesCount,
                    "anime" to animeCount
                )
            )
        }
    }

    suspend fun getInfo(localIp: String, port: Int): ApiInfoResponse {
        return withContext(Dispatchers.IO) {
            val seriesCount = repository.loadSeriesDatabase().size
            val moviesCount = repository.loadMoviesDatabase().size
            val animeCount = repository.loadAnimeDatabase().size

            ApiInfoResponse(
                ip = localIp,
                port = port,
                databases = mapOf(
                    "series_count" to seriesCount,
                    "movies_count" to moviesCount,
                    "anime_count" to animeCount
                )
            )
        }
    }

    suspend fun searchAll(query: String): Map<String, List<Any>> {
        return withContext(Dispatchers.IO) {
            mapOf(
                "series" to repository.searchSeries(query),
                "movies" to repository.searchMovies(query),
                "anime" to repository.searchAnime(query)
            )
        }
    }

    suspend fun getSeriesList(): List<SeriesItem> {
        return repository.getAllSeries()
    }

    suspend fun getMoviesList(): List<MovieItem> {
        return repository.getAllMovies()
    }

    suspend fun getAnimeList(): List<AnimeItem> {
        return repository.getAllAnime()
    }

    suspend fun getSeriesDetail(id: String): SeriesItem? {
        return repository.getSeries(id)
    }

    suspend fun getMovieDetail(id: String): MovieItem? {
        return repository.getMovie(id)
    }

    suspend fun getAnimeDetail(id: String): AnimeItem? {
        return repository.getAnime(id)
    }

    suspend fun refreshDatabases(): Map<String, Int> {
        repository.clearCache()
        return withContext(Dispatchers.IO) {
            mapOf(
                "series" to repository.loadSeriesDatabase().size,
                "movies" to repository.loadMoviesDatabase().size,
                "anime" to repository.loadAnimeDatabase().size
            )
        }
    }
}