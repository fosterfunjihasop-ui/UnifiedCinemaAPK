package com.example.unifiedcinema.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoSource(
    val iframe: String? = null,
    val video_id: String? = null,
    val m3u8: String? = null,
    val m3u8_fallback: String? = null,
    val embed: String? = null,
    val vdo_id: String? = null,
    val player_origin: String? = null,
    val tracks: List<SubtitleTrack>? = null
)

@Serializable
data class SubtitleTrack(
    val file: String,
    val label: String,
    val default: Boolean = false
)

@Serializable
data class Episode(
    val ep: Any,
    val title: String? = null,
    val page_url: String? = null,
    val sources: Map<String, VideoSource>? = null
) {
    fun getEpisodeNumber(): Int {
        return when (ep) {
            is Int -> ep
            is String -> ep.toIntOrNull() ?: 0
            else -> 0
        }
    }
}

@Serializable
data class SeriesItem(
    val id: String,
    val title: String,
    val url: String? = null,
    val poster: String? = null,
    val post_id: String? = null,
    val languages: List<String>? = null,
    val intro_skip: Int? = null,
    val outro_skip: Int? = null,
    val episodes: Map<String, Episode>? = null,
    val last_check: String? = null,
    val episodes_count: Int? = null,
    val platform: String? = null,
    val category: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val sequels: List<SequelInfo>? = null
)

@Serializable
data class SequelInfo(
    val id: String,
    val title: String
)

@Serializable
data class MovieItem(
    val id: String,
    val post_id: String? = null,
    val title: String,
    val url: String? = null,
    val poster: String? = null,
    val year: Int? = null,
    val imdb: Double? = null,
    val platform: String? = null,
    val region: String? = null,
    val genres: List<String>? = null,
    val genre: String? = null,
    val category: String? = null,
    val languages: List<String>? = null,
    val is_zoom: Boolean? = null,
    val zoom_clear: Boolean? = null,
    val resolution: String? = null,
    val quality: String? = null,
    val actors: List<String>? = null,
    val cast: String? = null,
    val description: String? = null,
    val published: String? = null,
    val homepage_boost: Double? = null,
    val sources: Map<String, VideoSource>? = null,
    val last_check: String? = null,
    val popularity_score: Double? = null,
    val recommended: Boolean? = null,
    val popularity_rank: Int? = null
)

@Serializable
data class AnimeItem(
    val id: String,
    val title: String,
    val url: String? = null,
    val poster: String? = null,
    val post_id: String? = null,
    val platform: String? = null,
    val category: String? = null,
    val genre: String? = null,
    val description: String? = null,
    val languages: List<String>? = null,
    val intro_skip: Int? = null,
    val outro_skip: Int? = null,
    val episodes: Map<String, Episode>? = null,
    val last_check: String? = null,
    val episodes_count: Int? = null,
    val sequels: List<SequelInfo>? = null
)

@Serializable
data class DatabaseResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)