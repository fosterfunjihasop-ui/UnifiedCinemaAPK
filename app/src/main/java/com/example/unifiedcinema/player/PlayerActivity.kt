package com.example.unifiedcinema.player

import android.app.PictureInPictureParams
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.example.unifiedcinema.data.repository.ContentType
import com.example.unifiedcinema.network.StreamResolver
import com.example.unifiedcinema.R

class PlayerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_CONTENT_TYPE = "content_type"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_POSTER = "poster"
        private const val EXTRA_HEADERS = "headers"
        private const val EXTRA_EPISODE = "episode"
        private const val EXTRA_SERIES_ID = "series_id"
        private const val EXTRA_POST_ID = "post_id"
        private const val EXTRA_LANGUAGE = "language"

        fun createIntent(
            context: Context,
            videoUrl: String,
            contentType: ContentType,
            title: String? = null,
            poster: String? = null,
            headers: Map<String, String> = emptyMap(),
            episode: String? = null,
            seriesId: String? = null,
            postId: String? = null,
            language: String = "Thai"
        ): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_CONTENT_TYPE, contentType.name)
                title?.let { putExtra(EXTRA_TITLE, it) }
                poster?.let { putExtra(EXTRA_POSTER, it) }
                putExtra(EXTRA_HEADERS, HashMap(headers))
                episode?.let { putExtra(EXTRA_EPISODE, it) }
                seriesId?.let { putExtra(EXTRA_SERIES_ID, it) }
                postId?.let { putExtra(EXTRA_POST_ID, it) }
                putExtra(EXTRA_LANGUAGE, language)
            }
        }
    }

    private lateinit var playerView: PlayerView
    private var exoPlayer: ExoPlayer? = null
    private var videoUrl: String = ""
    private var contentType: ContentType = ContentType.SERIES
    private var customHeaders: Map<String, String> = emptyMap()
    private var title: String? = null
    private var episode: String? = null
    private var seriesId: String? = null
    private var postId: String? = null
    private var language: String = "Thai"
    private var wasPlayingBeforePause = false
    
    // UI Elements
    private lateinit var loadingProgress: ProgressBar
    private lateinit var errorOverlay: View
    private lateinit var errorMessage: TextView
    private lateinit var retryButton: Button
    private lateinit var titleText: TextView

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force landscape and fullscreen
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        
        setContentView(R.layout.activity_player)
        
        playerView = findViewById(R.id.player_view)
        loadingProgress = findViewById(R.id.loading_progress)
        errorOverlay = findViewById(R.id.error_overlay)
        errorMessage = findViewById(R.id.error_message)
        retryButton = findViewById(R.id.retry_button)
        
        // Find controller views (from custom_player_controller.xml)
        titleText = playerView.findViewById(R.id.title_text)
        
        playerView.controllerShowTimeoutMs = 5000
        playerView.controllerHideOnTouch = true
        playerView.useController = true
        
        parseIntent()
        setupPlayer()
        setupSystemUi()
        setupErrorRetry()
    }

    private fun parseIntent() {
        intent.extras?.let { extras ->
            videoUrl = extras.getString(EXTRA_VIDEO_URL) ?: ""
            contentType = ContentType.valueOf(extras.getString(EXTRA_CONTENT_TYPE) ?: "SERIES")
            title = extras.getString(EXTRA_TITLE)
            poster = extras.getString(EXTRA_POSTER)
            episode = extras.getString(EXTRA_EPISODE)
            seriesId = extras.getString(EXTRA_SERIES_ID)
            postId = extras.getString(EXTRA_POST_ID)
            language = extras.getString(EXTRA_LANGUAGE) ?: "Thai"
            
            // Get headers from intent
            val headersMap = extras.getSerializable(EXTRA_HEADERS) as? Map<String, String> ?: emptyMap()
            // Merge with default headers for content type
            customHeaders = StreamResolver.getHeadersForType(contentType) + headersMap
        }
    }

    private fun setupPlayer() {
        showLoading(true)
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(
                DefaultHttpDataSource.RequestProperties().apply {
                    customHeaders.forEach { (key, value) ->
                        set(key, value)
                    }
                }
            )
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent(StreamResolver.getHeadersForType(contentType)["User-Agent"])

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { player ->
                playerView.player = player
                
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                showLoading(false)
                                hideSystemUi()
                            }
                            Player.STATE_ENDED -> {
                                finish()
                            }
                            Player.STATE_BUFFERING -> {
                                showLoading(true)
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        showLoading(false)
                        val errorMsg = when (error.errorCode) {
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "ข้อผิดพลาดเครือข่าย: ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้"
                            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "รูปแบบวิดีโอไม่รองรับ"
                            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "ตำแหน่งอ่านข้อมูลผิดพลาด"
                            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "สตรีมสดหมดอายุ กรุณาลองใหม่"
                            else -> "เกิดข้อผิดพลาดในการเล่นวิดีโอ: ${error.message ?: error.errorCode}"
                        }
                        showError(errorMsg)
                        Log.e("PlayerActivity", "Playback error: ${error.errorCode} - ${error.message}")
                    }
                    
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            showLoading(false)
                        }
                    }
                })
                
                // Prepare media item
                val mediaItem = MediaItem.Builder()
                    .setUri(videoUrl)
                    .setMimeType(MimeType.getMimeType(videoUrl))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title ?: "Video")
                            .build()
                    )
                    .setRequestHeaders(customHeaders)
                    .build()
                
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
                
                // Set volume control to media stream
                setVolumeControlStream(AudioManager.STREAM_MUSIC)
            }
    }

    private fun setupSystemUi() {
        hideSystemUi()
        
        // For Android TV, show/hide controller on focus changes
        playerView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                playerView.showController()
            }
        }
        
        // Update title in controller
        title?.let { titleText.text = it }
    }

    private fun setupErrorRetry() {
        retryButton.setOnClickListener {
            errorOverlay.visibility = View.GONE
            loadingProgress.visibility = View.VISIBLE
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true
        }
    }

    private fun showLoading(show: Boolean) {
        loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorOverlay.visibility = View.VISIBLE
        loadingProgress.visibility = View.GONE
    }

    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun showSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsets.Type.systemBars())
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasPlayingBeforePause) {
            exoPlayer?.playWhenReady = true
            wasPlayingBeforePause = false
        }
        hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        wasPlayingBeforePause = exoPlayer?.playWhenReady == true
        exoPlayer?.playWhenReady = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture mode on home gesture
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            // Hide controller in PiP mode
            playerView.hideController()
        } else {
            // Show controller when returning from PiP
            playerView.showController()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        ioExecutor.shutdown()
        scope.cancel()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> {
                // Play/Pause toggle
                exoPlayer?.let { player ->
                    if (player.playWhenReady) {
                        player.playWhenReady = false
                    } else {
                        player.playWhenReady = true
                        hideSystemUi()
                    }
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                // Seek backward 10 seconds
                exoPlayer?.seekTo(Math.max(0, exoPlayer!!.currentPosition - 10000))
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Seek forward 10 seconds
                exoPlayer?.seekTo(exoPlayer!!.currentPosition + 10000)
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Volume up
                adjustVolume(1)
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Volume down
                adjustVolume(-1)
                playerView.showController()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                exoPlayer?.let { player ->
                    player.playWhenReady = !player.playWhenReady
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                // Next episode
                playNextEpisode()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                // Previous episode
                playPreviousEpisode()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun adjustVolume(direction: Int) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val newVolume = (currentVolume + direction).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    override fun onBackPressed() {
        // Exit fullscreen player
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            // Exit PiP mode
            val pipParams = PictureInPictureParams.Builder().build()
            setPictureInPictureParams(pipParams)
        } else {
            finish()
        }
    }

    private fun playNextEpisode() {
        episode?.let { ep ->
            val nextEp = (ep.toIntOrNull() ?: 0) + 1
            loadEpisode(nextEp.toString())
        }
    }

    private fun playPreviousEpisode() {
        episode?.let { ep ->
            val prevEp = (ep.toIntOrNull() ?: 1) - 1
            if (prevEp >= 1) {
                loadEpisode(prevEp.toString())
            }
        }
    }

    private fun loadEpisode(ep: String) {
        // This would need to call the API to get the new stream URL
        // For now, just show a toast
        android.widget.Toast.makeText(this, "โหลดตอนที่ $ep...", android.widget.Toast.LENGTH_SHORT).show()
        // TODO: Implement API call to resolve new episode stream
    }
}

object MimeType {
    fun getMimeType(url: String): String {
        return when {
            url.endsWith(".m3u8") || url.contains(".m3u8") -> "application/x-mpegURL"
            url.endsWith(".mpd") || url.contains(".mpd") -> "application/dash+xml"
            url.endsWith(".mp4") -> "video/mp4"
            url.endsWith(".webm") -> "video/webm"
            else -> "application/x-mpegURL" // Default to HLS
        }
    }
}