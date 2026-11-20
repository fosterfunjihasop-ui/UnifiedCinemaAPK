package com.example.unifiedcinema.di

import android.content.Context
import com.example.unifiedcinema.data.repository.MediaRepository
import com.example.unifiedcinema.network.StreamResolver
import com.example.unifiedcinema.network.UnifiedApiService
import okhttp3.OkHttpClient

object AppContainer {
    @Volatile private var INSTANCE: AppContainer? = null

    val mediaRepository: MediaRepository
    val streamResolver: StreamResolver
    val unifiedApiService: UnifiedApiService
    val okHttpClient: OkHttpClient

    private constructor(context: Context) {
        okHttpClient = StreamResolver.createClient()
        mediaRepository = MediaRepository.getInstance(context)
        streamResolver = StreamResolver(okHttpClient)
        unifiedApiService = UnifiedApiService(mediaRepository, streamResolver)
    }

    fun getInstance(context: Context): AppContainer {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: AppContainer(context.applicationContext).also { INSTANCE = it }
        }
    }
}