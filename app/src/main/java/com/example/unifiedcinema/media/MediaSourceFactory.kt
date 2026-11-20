package com.example.unifiedcinema.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.hls.HlsMediaSource
import com.example.unifiedcinema.data.repository.ContentType
import com.example.unifiedcinema.network.StreamResolver
import okhttp3.OkHttpClient

@UnstableApi
class CustomHttpDataSource(
    private val contentType: ContentType,
    private val okHttpClient: OkHttpClient,
    private val userAgent: String? = null
) : DefaultHttpDataSource.Factory() {

    override fun createDataSource(): DataSource {
        return object : DefaultHttpDataSource(
            DefaultHttpDataSource.RequestProperties().apply {
                StreamResolver.getHeadersForType(contentType).forEach { (k, v) ->
                    set(k, v)
                }
                userAgent?.let { set("User-Agent", it) }
            },
            okHttpClient
        ) {}
    }
}

class MediaSourceFactory(
    private val okHttpClient: OkHttpClient = StreamResolver.createClient()
) {

    fun createMediaSource(
        mediaItem: MediaItem,
        contentType: ContentType,
        customHeaders: Map<String, String> = emptyMap()
    ): MediaSource {
        val headers = StreamResolver.getHeadersForType(contentType) + customHeaders
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                DefaultHttpDataSource.RequestProperties().apply {
                    headers.forEach { (k, v) -> set(k, v) }
                }
            )
            .setOkHttpClient(okHttpClient)
            .setAllowCrossProtocolRedirects(true)

        return when {
            mediaItem.mediaType == MediaItem.MEDIA_TYPE_HLS -> {
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
            mediaItem.mediaType == MediaItem.MEDIA_TYPE_DASH -> {
                // DASH not used in this app
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            }
        }
    }

    fun createHlsMediaSource(
        m3u8Url: String,
        contentType: ContentType,
        customHeaders: Map<String, String> = emptyMap()
    ): HlsMediaSource {
        val mediaItem = MediaItem.fromUri(m3u8Url)
        val headers = StreamResolver.getHeadersForType(contentType) + customHeaders
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                DefaultHttpDataSource.RequestProperties().apply {
                    headers.forEach { (k, v) -> set(k, v) }
                }
            )
            .setOkHttpClient(okHttpClient)
            .setAllowCrossProtocolRedirects(true)

        return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }
}

class MediaItemBuilder {

    companion object {
        fun buildHlsMediaItem(
            m3u8Url: String,
            contentType: ContentType,
            title: String? = null,
            subtitleTracks: List<MediaItem.SubtitleConfiguration>? = null,
            drmConfiguration: MediaItem.DrmConfiguration? = null
        ): MediaItem {
            val headers = StreamResolver.getHeadersForType(contentType)
            
            return MediaItem.Builder()
                .setUri(m3u8Url)
                .setMimeType("application/x-mpegURL")
                .setMediaType(MediaItem.MEDIA_TYPE_HLS)
                .setRequestHeaders(headers)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title ?: "Video")
                        .build()
                )
                .apply {
                    subtitleTracks?.forEach { addSubtitleConfiguration(it) }
                    drmConfiguration?.let { setDrmConfiguration(it) }
                }
                .build()
        }

        fun buildProgressiveMediaItem(
            videoUrl: String,
            contentType: ContentType,
            mimeType: String = "video/mp4",
            title: String? = null
        ): MediaItem {
            val headers = StreamResolver.getHeadersForType(contentType)
            
            return MediaItem.Builder()
                .setUri(videoUrl)
                .setMimeType(mimeType)
                .setMediaType(MediaItem.MEDIA_TYPE_PROGRESSIVE)
                .setRequestHeaders(headers)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(title ?: "Video")
                        .build()
                )
                .build()
        }
    }
}