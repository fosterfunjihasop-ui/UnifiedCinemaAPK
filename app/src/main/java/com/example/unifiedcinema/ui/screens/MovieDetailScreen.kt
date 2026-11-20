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
import com.example.unifiedcinema.data.model.MovieItem
import com.example.unifiedcinema.data.repository.ContentType
import com.example.unifiedcinema.di.AppContainer
import com.example.unifiedcinema.network.UnifiedApiService
import com.example.unifiedcinema.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieDetailScreen : androidx.appcompat.app.AppCompatActivity() {
    
    companion object {
        private const val EXTRA_MOVIE_ID = "movie_id"
        private const val EXTRA_MOVIE_TITLE = "movie_title"
        private const val EXTRA_POSTER = "poster"
        
        fun createIntent(
            context: Context,
            movieId: String,
            movieTitle: String,
            poster: String? = null
        ): Intent {
            return Intent(context, MovieDetailScreen::class.java).apply {
                putExtra(EXTRA_MOVIE_ID, movieId)
                putExtra(EXTRA_MOVIE_TITLE, movieTitle)
                poster?.let { putExtra(EXTRA_POSTER, it) }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: return finish()
        val movieTitle = intent.getStringExtra(EXTRA_MOVIE_TITLE) ?: ""
        val poster = intent.getStringExtra(EXTRA_POSTER)
        
        androidx.compose.ui.platform.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContent {
            UnifiedCinemaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    MovieDetailContent(
                        movieId = movieId,
                        movieTitle = movieTitle,
                        poster = poster
                    )
                }
            }
        }
    }
}

@Composable
fun MovieDetailContent(
    movieId: String,
    movieTitle: String,
    poster: String?
) {
    val container = AppContainer.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val apiService = container.unifiedApiService
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    var movie by remember { mutableStateOf<MovieItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(movieId) {
        loadMovieDetail()
    }
    
    fun loadMovieDetail() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.getMovieDetail(movieId)
                movie = result
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
            onRetry = loadMovieDetail,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        movie?.let { movie ->
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
                                contentDescription = movieTitle,
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
                                text = movieTitle,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 3,
                                overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis
                            )
                            
                            movie.year?.let {
                                Text(text = "ปี: $it", fontSize = 16.sp, color = Color(0xFFBB86FC))
                            }
                            
                            movie.genre?.let {
                                Text(text = "แนว: $it", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            movie.platform?.let {
                                Text(text = "แพลตฟอร์ม: $it", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            movie.imdb?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Star,
                                        contentDescription = "IMDb",
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "IMDb: ${String.format("%.1f", it)}", fontSize = 14.sp, color = Color(0xFFFFD700), fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            movie.quality?.let {
                                Text(text = "คุณภาพ: $it", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            movie.languages?.joinToString(", ")?.let { langs ->
                                Text(text = "ภาษา: $langs", fontSize = 14.sp, color = Color(0xFF999999))
                            }
                            
                            // Play button
                            androidx.compose.material3.FilledButton(
                                onClick = {
                                    playMovie(movie)
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
                                    Text(text = "เล่นหนัง", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
                
                // Description
                movie.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 14.sp,
                        color = Color(0xFFCCCCCC),
                        modifier = Modifier.padding(24.dp, 16.dp).fillMaxWidth()
                    )
                }
                
                // Cast
                movie.cast?.let { cast ->
                    Text(
                        text = "นักแสดง: $cast",
                        fontSize = 14.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(24.dp, 8.dp).fillMaxWidth()
                    )
                }
            }
        }
    }
    
    fun playMovie(movie: MovieItem) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val source = movie.sources?.get("main") ?: movie.sources?.get("Thai") ?: movie.sources?.get("พากย์ไทย") ?: movie.sources?.get("ซับไทย") ?: movie.sources?.values?.firstOrNull()
        
        source?.let { videoSource ->
            videoSource.m3u8?.let { m3u8Url ->
                val intent = PlayerActivity.createIntent(
                    context = context,
                    videoUrl = m3u8Url,
                    contentType = ContentType.MOVIES,
                    title = movie.title,
                    poster = movie.poster
                )
                context.startActivity(intent)
            }
        }
    }
}