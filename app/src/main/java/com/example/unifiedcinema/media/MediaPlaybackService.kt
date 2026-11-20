package com.example.unifiedcinema.media

import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommandGroup
import androidx.media3.session.SessionResult

class MediaPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionCommands(getAvailableCommands())
            .setId("UnifiedCinemaMediaSession")
            .build()
    }

    override fun onGetSession(controllerInfo: MediaController.ControllerInfo): MediaSession {
        return mediaSession!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        exoPlayer?.run {
            release()
            exoPlayer = null
        }
        super.onDestroy()
    }

    companion object {
        private const val COMMAND_PLAY = "play"
        private const val COMMAND_PAUSE = "pause"
        private const val COMMAND_SEEK_TO = "seek_to"

        private fun getAvailableCommands(): SessionCommandGroup {
            return SessionCommandGroup(
                SessionCommand(COMMAND_PLAY),
                SessionCommand(COMMAND_PAUSE),
                SessionCommand(COMMAND_SEEK_TO),
                *MediaSession.COMMAND_GROUP_DEFAULT.toArray()
            )
        }
    }
}