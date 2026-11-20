package com.example.unifiedcinema.network

import com.example.unifiedcinema.data.model.VideoSource
import com.example.unifiedcinema.data.repository.ContentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class StreamResolver(
    private val okHttpClient: OkHttpClient = StreamResolver.createClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    companion object {
        private const val PLAYER_ORIGIN_SERIES = "https://main.24playerhd.com"
        private const val PLAYER_ORIGIN_ANIME = "https://main.108player.com"
        private const val PLAYER_ORIGIN_MOVIES = "https://vdohls.com"

        private const val USER_AGENT_SERIES = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        private const val USER_AGENT_MOVIES = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val USER_AGENT_ANIME = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

        private const val REFERER_SERIES = "https://main.24playerhd.com/"
        private const val REFERER_MOVIES = "https://player77hdfree.xyz/"
        private const val REFERER_ANIME = "https://main.108player.com/"

        private const val ORIGIN_SERIES = "https://main.24playerhd.com"
        private const val ORIGIN_MOVIES = "https://player77hdfree.xyz"
        private const val ORIGIN_ANIME = "https://main.108player.com"

        fun createClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun getHeadersForType(contentType: ContentType): Map<String, String> {
            return when (contentType) {
                ContentType.SERIES -> mapOf(
                    "User-Agent" to USER_AGENT_SERIES,
                    "Referer" to REFERER_SERIES,
                    "Origin" to ORIGIN_SERIES,
                    "Accept" to "*/*",
                    "Accept-Language" to "th-TH,th;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Connection" to "keep-alive"
                )
                ContentType.MOVIES -> mapOf(
                    "User-Agent" to USER_AGENT_MOVIES,
                    "Referer" to REFERER_MOVIES,
                    "Origin" to ORIGIN_MOVIES,
                    "Accept" to "*/*",
                    "Accept-Language" to "th-TH,th;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Connection" to "keep-alive"
                )
                ContentType.ANIME -> mapOf(
                    "User-Agent" to USER_AGENT_ANIME,
                    "Referer" to REFERER_ANIME,
                    "Origin" to ORIGIN_ANIME,
                    "Accept" to "*/*",
                    "Accept-Language" to "th-TH,th;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Connection" to "keep-alive"
                )
            }
        }

        fun getPlayerOrigin(contentType: ContentType): String {
            return when (contentType) {
                ContentType.SERIES -> PLAYER_ORIGIN_SERIES
                ContentType.MOVIES -> PLAYER_ORIGIN_MOVIES
                ContentType.ANIME -> PLAYER_ORIGIN_ANIME
            }
        }
    }

    data class ResolveResult(
        val success: Boolean,
        val contentType: ContentType? = null,
        val stream: VideoSource? = null,
        val m3u8Url: String? = null,
        val masterPlaylistContent: String? = null,
        val error: String? = null
    )

    data class ResolveRequest(
        val id: String? = null,
        val postId: String? = null,
        val episode: String? = null,
        val language: String = "Thai",
        val contentType: ContentType? = null,
        val referer: String? = null
    )

    suspend fun resolveStream(request: ResolveRequest): ResolveResult {
        return withContext(ioDispatcher) {
            var contentType = request.contentType
            
            if (contentType == null) {
                contentType = detectTypeFromRequest(request)
            }

            if (contentType == null) {
                return@withContext ResolveResult(
                    success = false,
                    error = "Unable to detect content type"
                )
            }

            when (contentType) {
                ContentType.SERIES -> resolveSeries(request, contentType)
                ContentType.MOVIES -> resolveMovie(request, contentType)
                ContentType.ANIME -> resolveAnime(request, contentType)
            }
        }
    }

    private fun detectTypeFromRequest(request: ResolveRequest): ContentType? {
        val id = request.id ?: request.postId
        if (id == null) return null

        if (id.startsWith("doctor-x") || id.startsWith("where-the-mask") || id.startsWith("the-remarried") || 
            id.startsWith("miracles-of") || id.contains("serieday")) {
            return ContentType.SERIES
        }

        if (id.contains("24hd") || id.contains("swords-of-legends") || id.contains("detective-dee") ||
            id.contains("project-hail-mary") || id.contains("catch-the-killer") || id.contains("spider-fairy") ||
            id.contains("toy-story") || id.contains("detective-conan") || id.contains("remarkably-bright") ||
            id.contains("jiaolong") || id.contains("dear-you") || id.contains("milky-subway") ||
            id.contains("spider-man-brand") || id.contains("gintama") || id.contains("voicemails") ||
            id.contains("resident-evil") || id.contains("it-welcome") || id.contains("geomancer") ||
            id.contains("gohan") || id.contains("art-of-the-devil") || id.contains("snoopy")) {
            return ContentType.MOVIES
        }

        if (id.contains("ling-wu") || id.contains("cang-yuan") || id.contains("si-ling") || 
            id.contains("anime108") || id.contains("donghua")) {
            return ContentType.ANIME
        }

        return ContentType.SERIES
    }

    private suspend fun resolveSeries(request: ResolveRequest, contentType: ContentType): ResolveResult {
        val postId = request.postId ?: ""
        val episode = request.episode ?: "1"
        val language = request.language
        val referer = request.referer ?: "https://www.serieday-z.com/"

        val stream = try {
            val apiUrl = "$PLAYER_ORIGIN_SERIES/api/get_stream.php"
            val jsonBody = JSONObject().apply {
                put("post_id", postId)
                put("ep", episode)
                put("lang", language)
                put("referer", referer)
            }.toString()

            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .headers(buildHeaders(getHeadersForType(contentType)))
                .build()

            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val streamData = json.getJSONObject("stream")
                        VideoSource(
                            iframe = streamData.optString("iframe", null),
                            video_id = streamData.optString("video_id", null),
                            m3u8 = streamData.optString("m3u8", null),
                            m3u8_fallback = streamData.optString("m3u8_fallback", null)
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }

        return if (stream != null && stream.video_id != null) {
            ResolveResult(
                success = true,
                contentType = contentType,
                stream = stream
            )
        } else {
            ResolveResult(
                success = false,
                contentType = contentType,
                error = "Unable to resolve stream for series"
            )
        }
    }

    private suspend fun resolveMovie(request: ResolveRequest, contentType: ContentType): ResolveResult {
        val id = request.id ?: ""
        
        val movieApiUrl = "https://www.24hd.today/wp-json/wp/v2/posts/$id"
        
        return try {
            val httpRequest = Request.Builder()
                .url(movieApiUrl)
                .headers(buildHeaders(getHeadersForType(contentType)))
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    // Parse WordPress REST API response to extract embed URL
                    // For now, return the original m3u8 from database
                    ResolveResult(
                        success = true,
                        contentType = contentType,
                        stream = VideoSource()
                    )
                } else {
                    ResolveResult(
                        success = false,
                        contentType = contentType,
                        error = "Movie not found"
                    )
                }
            }
        } catch (e: Exception) {
            ResolveResult(
                success = false,
                contentType = contentType,
                error = "Movie resolve failed: ${e.message}"
            )
        }
    }

    private suspend fun resolveAnime(request: ResolveRequest, contentType: ContentType): ResolveResult {
        val postId = request.postId ?: ""
        val episode = request.episode ?: "1"
        val language = request.language
        val referer = request.referer ?: "https://www.anime108.com/"

        val stream = try {
            val apiUrl = "$PLAYER_ORIGIN_ANIME/api/get_stream.php"
            val jsonBody = JSONObject().apply {
                put("post_id", postId)
                put("ep", episode)
                put("lang", language)
                put("referer", referer)
            }.toString()

            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .headers(buildHeaders(getHeadersForType(contentType)))
                .build()

            okHttpClient.newCall(httpRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return@use null
                    val json = JSONObject(responseBody)
                    if (json.getBoolean("success")) {
                        val streamData = json.getJSONObject("stream")
                        VideoSource(
                            iframe = streamData.optString("iframe", null),
                            video_id = streamData.optString("video_id", null),
                            m3u8 = streamData.optString("m3u8", null),
                            m3u8_fallback = streamData.optString("m3u8_fallback", null),
                            player_origin = PLAYER_ORIGIN_ANIME
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }

        return if (stream != null && stream.video_id != null) {
            ResolveResult(
                success = true,
                contentType = contentType,
                stream = stream
            )
        } else {
            ResolveResult(
                success = false,
                contentType = contentType,
                error = "Unable to resolve stream for anime"
            )
        }
    }

    suspend fun fetchMasterPlaylist(videoId: String, contentType: ContentType): String? {
        return withContext(ioDispatcher) {
            val origin = getPlayerOrigin(contentType)
            val primaryUrl = "$origin/newplaylist/$videoId/$videoId.m3u8"
            val fallbackUrl = "$origin/newplaylist_g/$videoId/$videoId.m3u8"
            val headers = getHeadersForType(contentType)

            fetchM3u8WithFallback(primaryUrl, fallbackUrl, headers)
        }
    }

    private suspend fun fetchM3u8WithFallback(
        primaryUrl: String,
        fallbackUrl: String,
        headers: Map<String, String>
    ): String? {
        return try {
            fetchM3u8(primaryUrl, headers) ?: fetchM3u8(fallbackUrl, headers)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun fetchM3u8(url: String, headers: Map<String, String>): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .headers(buildHeaders(headers))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildHeaders(headersMap: Map<String, String>): okhttp3.Headers {
        val builder = okhttp3.Headers.Builder()
        headersMap.forEach { (key, value) ->
            builder.add(key, value)
        }
        return builder.build()
    }

    suspend fun fetchSubPlaylist(url: String, contentType: ContentType): String? {
        return withContext(ioDispatcher) {
            val headers = getHeadersForType(contentType)
            fetchM3u8(url, headers)
        }
    }

    suspend fun fetchSegment(url: String, contentType: ContentType): ByteArray? {
        return withContext(ioDispatcher) {
            val headers = getHeadersForType(contentType)
            try {
                val request = Request.Builder()
                    .url(url)
                    .headers(buildHeaders(headers))
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.bytes()
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}