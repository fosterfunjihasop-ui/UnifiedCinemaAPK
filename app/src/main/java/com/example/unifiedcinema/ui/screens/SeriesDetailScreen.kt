package com.example.unifiedcinema.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedcinema.R
import com.example.unifiedcinema.data.model.SeriesItem
import com.example.unifiedcinema.data.repository.ContentType
import com.example.unifiedcinema.di.AppContainer
import com.example.unifiedcinema.network.UnifiedApiService
import com.example.unifiedcinema.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeriesDetailScreen : androidx.appcompat.app.AppCompatActivity() {
    
    companion object {
        private const val EXTRA_SERIES_ID = "series_id"
        private const val EXTRA_SERIES_TITLE = "series_title"
        private const val EXTRA_POSTER = "poster"
        
        fun createIntent(
            context: Context,
            seriesId: String,
            seriesTitle: String,
            poster: String? = null
        ): Intent {
            return Intent(context, SeriesDetailScreen::class.java).apply {
                putExtra(EXTRA_SERIES_ID, seriesId)
                putExtra(EXTRA_SERIES_TITLE, seriesTitle)
                poster?.let { putExtra(EXTRA_POSTER, it) }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val seriesId = intent.getStringExtra(EXTRA_SERIES_ID) ?: return finish()
        val seriesTitle = intent.getStringExtra(EXTRA_SERIES_TITLE) ?: ""
        val poster = intent.getStringExtra(EXTRA_POSTER)
        
        androidx.compose.ui.platform.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            UnifiedCinemaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    SeriesDetailContent(
                        seriesId = seriesId,
                        seriesTitle = seriesTitle,
                        poster = poster
                    )
                }
            }
        }
    }
}

@Composable
fun SeriesDetailContent(
    seriesId: String,
    seriesTitle: String,
    poster: String?
) {
    val container = AppContainer.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val apiService = container.unifiedApiService
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    var series by remember { mutableStateOf<SeriesItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var focusedEpisodeIndex by remember { mutableStateOf(0) }
    
    androidx.compose.runtime.LaunchedEffect(seriesId) {
        loadSeriesDetail()
    }
    
    fun loadSeriesDetail() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.getSeriesDetail(seriesId)
                series = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        com.example.unifiedcinema.ui.components.ComponentsKt.LoadingIndicator(modifier = Modifier.fillMaxSize())
    } else if (errorMessage != null) {
        com.example.unifiedcinema.ui.components.ComponentsKt.ErrorMessage(
            message = errorMessage!!,
            onRetry = loadSeriesDetail,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        series?.let { series ->
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top
            ) {
                // Header with poster and info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF1E1E1E))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Poster
                        poster?.let { url ->
                            androidx.compose.foundation.Image(
                                painter = painterResource(id = R.drawable.tv_banner),
                                contentDescription = seriesTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(280.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(start = 24.dp)
                            )
                        }
                        
                        // Info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 24.dp, bottom = 24.dp, end = 24.dp)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = seriesTitle,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
                            )
                            
                            series.genre?.let {
                                Text(text = it, fontSize = 16.sp, color = Color(0xFFBB86FC))
                            }
                            
                            series.platform?.let {
                                Text(text = "แพลตฟอร์ม: $it", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            series.category?.let {
                                Text(text = "ประเภท: $it", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            series.episodes_count?.let {
                                Text(text = "จำนวนตอน: $it ตอน", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            series.languages?.joinToString(", ")?.let { langs ->
                                Text(text = "ภาษา: $langs", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            // Play first episode button
                            androidx.compose.material3.FilledButton(
                                onClick = {
                                    playEpisode(series, "1", "Thai")
                                },
                                colors = androidx.compose.material3.ButtonDefaults.filledButtonColors(
                                    containerColor = Color(0xFFBB86FC)
                                ),
                                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.Black
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "เล่นตอนแรก", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
                
                // Episodes list
                Text(
                    text = "รายการตอน",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(24.dp, 16.dp)
                )
                
                series.episodes?.let { episodes ->
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
                    ) {
                        items(episodes.entries.sortedBy { it.key.toIntOrNull() ?: 0 }) { entry ->
                            EpisodeItem(
                                episode = entry.value,
                                series = series,
                                isFocused = false,
                                onClick = { playEpisode(series, entry.key, "Thai") }
                            )
                        }
                    }
                }
            }
        }
    }
    
    fun playEpisode(series: SeriesItem, episode: String, language: String) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val epObj = series.episodes?.get(episode)
        val source = epObj?.sources?.get(language) ?: epObj?.sources?.values?.firstOrNull()
        
        source?.let { videoSource ->
            videoSource.m3u8?.let { m3u8Url ->
                val intent = PlayerActivity.createIntent(
                    context = context,
                    videoUrl = m3u8Url,
                    contentType = ContentType.SERIES,
                    title = "${series.title} - ${epObj?.title ?: "ตอนที่ $episode"}",
                    poster = series.poster,
                    episode = episode,
                    seriesId = series.id,
                    postId = series.post_id,
                    language = language
                )
                context.startActivity(intent)
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: com.example.unifiedcinema.data.model.Episode,
    series: SeriesItem,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val hasSource = episode.sources?.isNotEmpty() == true
    
    com.example.unifiedcinema.ui.components.ComponentsKt.FocusableCard(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        isFocused = isFocused,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode number
            Text(
                text = "EP ${episode.getEpisodeNumber()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFBB86FC),
                modifier = Modifier.width(80.dp)
            )
            
            // Title
            Text(
                text = episode.title ?: "ตอนที่ ${episode.getEpisodeNumber()}",
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            // Status indicator
            if (hasSource) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.PlayCircle,
                    contentDescription = "Available",
                    tint = Color(0xFF03DAC5),
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = "Not available",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}