package com.example.unifiedcinema.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedcinema.data.model.AnimeItem
import com.example.unifiedcinema.data.model.MovieItem
import com.example.unifiedcinema.data.model.SeriesItem
import com.example.unifiedcinema.data.repository.ContentType
import com.example.unifiedcinema.di.AppContainer
import com.example.unifiedcinema.network.UnifiedApiService
import com.example.unifiedcinema.player.PlayerActivity
import com.example.unifiedcinema.ui.components.ComponentsKt.EmptyState
import com.example.unifiedcinema.ui.components.ComponentsKt.ErrorMessage
import com.example.unifiedcinema.ui.components.ComponentsKt.LoadingIndicator
import com.example.unifiedcinema.ui.components.ComponentsKt.MediaCard
import com.example.unifiedcinema.ui.components.ComponentsKt.SearchBar
import com.example.unifiedcinema.ui.components.ComponentsKt.TabButton
import com.example.unifiedcinema.ui.theme.UnifiedCinemaTheme
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    container: AppContainer = AppContainer.getInstance(androidx.compose.ui.platform.LocalContext.current)
) {
    val apiService = container.unifiedApiService
    val scope = remember { androidx.compose.runtime.CoroutineScope(androidx.compose.runtime.Dispatchers.Main) }
    
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    val tabs = listOf("ซีรีส์", "หนัง", "อนิเมะ")
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Toolbar with tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1E1E),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Search bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearchClick = { isSearching = true }
                )
                
                androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp)))
                
                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    tabs.forEachIndexed { index, tab ->
                        TabButton(
                            text = tab,
                            isSelected = selectedTab == index,
                            onClick = { 
                                selectedTab = index
                                searchQuery = ""
                                isSearching = false
                            }
                        )
                    }
                }
            }
        }
        
        // Content
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SeriesTab(apiService = apiService, scope = scope, searchQuery = searchQuery, isSearching = isSearching)
                1 -> MoviesTab(apiService = apiService, scope = scope, searchQuery = searchQuery, isSearching = isSearching)
                2 -> AnimeTab(apiService = apiService, scope = scope, searchQuery = searchQuery, isSearching = isSearching)
            }
        }
    }
}

@Composable
fun SeriesTab(
    apiService: UnifiedApiService,
    scope: androidx.compose.runtime.CoroutineScope,
    searchQuery: String,
    isSearching: Boolean
) {
    var seriesList by remember { mutableStateOf<List<SeriesItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var focusedIndex by remember { mutableStateOf(0) }
    
    androidx.compose.runtime.LaunchedEffect(searchQuery, isSearching) {
        if (isSearching && searchQuery.isNotBlank()) {
            loadSeries(searchQuery)
        } else if (!isSearching) {
            loadAllSeries()
        }
    }
    
    fun loadAllSeries() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.getSeriesList()
                seriesList = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    fun loadSeries(query: String) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.searchSeries(query)
                seriesList = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
    } else if (errorMessage != null) {
        ErrorMessage(
            message = errorMessage!!,
            onRetry = { if (searchQuery.isNotBlank() && isSearching) loadSeries(searchQuery) else loadAllSeries() },
            modifier = Modifier.fillMaxSize()
        )
    } else if (seriesList.isEmpty()) {
        EmptyState(message = "ไม่พบซีรีส์", modifier = Modifier.fillMaxSize())
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
            ) {
                items(seriesList) { series ->
                    MediaCard(
                        title = series.title,
                        imageUrl = series.poster,
                        episodeCount = series.episodes_count,
                        genre = series.genre,
                        isFocused = false, // TV focus handled by LazyColumn
                        onClick = {
                            // Navigate to detail screen
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val intent = com.example.unifiedcinema.ui.screens.SeriesDetailScreen.createIntent(
                                context = context,
                                seriesId = series.id,
                                seriesTitle = series.title,
                                poster = series.poster
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MoviesTab(
    apiService: UnifiedApiService,
    scope: androidx.compose.runtime.CoroutineScope,
    searchQuery: String,
    isSearching: Boolean
) {
    var moviesList by remember { mutableStateOf<List<MovieItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(searchQuery, isSearching) {
        if (isSearching && searchQuery.isNotBlank()) {
            loadMovies(searchQuery)
        } else if (!isSearching) {
            loadAllMovies()
        }
    }
    
    fun loadAllMovies() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.getMoviesList()
                moviesList = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    fun loadMovies(query: String) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.searchMovies(query)
                moviesList = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
    } else if (errorMessage != null) {
        ErrorMessage(
            message = errorMessage!!,
            onRetry = { if (searchQuery.isNotBlank() && isSearching) loadMovies(searchQuery) else loadAllMovies() },
            modifier = Modifier.fillMaxSize()
        )
    } else if (moviesList.isEmpty()) {
        EmptyState(message = "ไม่พบหนัง", modifier = Modifier.fillMaxSize())
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
            ) {
                items(moviesList) { movie ->
                    com.example.unifiedcinema.ui.components.ComponentsKt.MediaCard(
                        title = movie.title,
                        imageUrl = movie.poster,
                        year = movie.year,
                        genre = movie.genre,
                        isFocused = false,
                        onClick = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val intent = com.example.unifiedcinema.ui.screens.MovieDetailScreen.createIntent(
                                context = context,
                                movieId = movie.id,
                                movieTitle = movie.title,
                                poster = movie.poster
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeTab(
    apiService: UnifiedApiService,
    scope: androidx.compose.runtime.CoroutineScope,
    searchQuery: String,
    isSearching: Boolean
) {
    var animeList by remember { mutableStateOf<List<AnimeItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    androidx.compose.runtime.LaunchedEffect(searchQuery, isSearching) {
        if (isSearching && searchQuery.isNotBlank()) {
            loadAnime(searchQuery)
        } else if (!isSearching) {
            loadAllAnime()
        }
    }
    
    fun loadAllAnime() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.getAnimeList()
                animeList = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    fun loadAnime(query: String) {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = apiService.searchAnime(query)
                animeList = result
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "เกิดข้อผิดพลาด"
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        LoadingIndicator(modifier = Modifier.fillMaxSize())
    } else if (errorMessage != null) {
        ErrorMessage(
            message = errorMessage!!,
            onRetry = { if (searchQuery.isNotBlank() && isSearching) loadAnime(searchQuery) else loadAllAnime() },
            modifier = Modifier.fillMaxSize()
        )
    } else if (animeList.isEmpty()) {
        EmptyState(message = "ไม่พบอนิเมะ", modifier = Modifier.fillMaxSize())
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 100.dp)
            ) {
                items(animeList) { anime ->
                    com.example.unifiedcinema.ui.components.ComponentsKt.MediaCard(
                        title = anime.title,
                        imageUrl = anime.poster,
                        episodeCount = anime.episodes_count,
                        genre = anime.genre,
                        isFocused = false,
                        onClick = {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val intent = com.example.unifiedcinema.ui.screens.AnimeDetailScreen.createIntent(
                                context = context,
                                animeId = anime.id,
                                animeTitle = anime.title,
                                poster = anime.poster
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}