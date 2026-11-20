package com.example.unifiedcinema.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedcinema.R

@Composable
fun FocusableCard(
    modifier: Modifier = Modifier,
    isFocused: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val focusColor = if (isFocused) Color(0xFFBB86FC) else Color.Transparent
    val elevation = if (isFocused) 8.dp else 2.dp
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (isFocused) Color(0xFF2C2C2C) else Color(0xFF1E1E1E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .then(
                if (isFocused) {
                    modifier.border(width = 3.dp, color = focusColor, shape = RoundedCornerShape(12.dp))
                } else modifier
            )
    ) {
        content()
    }
}

@Composable
fun PosterImage(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = ""
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2C2C))
    ) {
        // Placeholder - in real app use Coil/Glide
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.tv_banner),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MediaCard(
    modifier: Modifier = Modifier,
    title: String,
    imageUrl: String? = null,
    episodeCount: Int? = null,
    year: Int? = null,
    genre: String? = null,
    isFocused: Boolean = false,
    onClick: () -> Unit
) {
    FocusableCard(
        modifier = modifier.width(200.dp),
        isFocused = isFocused,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Poster
            PosterImage(
                imageUrl = imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(8.dp))
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            
            // Title
            Text(
                text = title,
                maxLines = 2,
                overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = androidx.compose.ui.text.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Meta info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                episodeCount?.let {
                    Text(
                        text = "$it ตอน",
                        fontSize = 12.sp,
                        color = Color(0xFFBB86FC),
                        fontWeight = FontWeight.Medium
                    )
                }
                year?.let {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$it",
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }
            
            genre?.let {
                Text(
                    text = it,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.overflow.TextOverflow.Ellipsis,
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    textAlign = androidx.compose.ui.text.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = "ค้นหาซีรีส์, หนัง, อนิเมะ...", color = Color(0xFF999999)) },
        leadingIcon = { Icon(imageVector = androidx.compose.material.icons.Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotBlank()) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onQueryChange("") }
                        .padding(16.dp)
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        singleLine = true,
        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions.Default.copy(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        keyboardActions = androidx.compose.ui.text.input.KeyboardActions(
            onSearch = onSearchClick
        ),
        colors = androidx.compose.material3.TextFieldDefaults.textFieldColors(
            containerColor = Color(0xFF1E1E1E),
            focusedContainerColor = Color(0xFF2C2C2C),
            textColor = Color.White,
            placeholderTextColor = Color(0xFF999999),
            cursorColor = Color(0xFFBB86FC),
            focusedIndicatorColor = Color(0xFFBB86FC),
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .height(48.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFBB86FC) else Color(0xFF999999)
        )
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    androidx.compose.material3.CircularProgressIndicator(
        modifier = modifier.size(48.dp).padding(32.dp),
        color = Color(0xFFBB86FC),
        strokeWidth = 4.dp
    )
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.ErrorOutline,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFFFF6B6B)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.FilledButton(
            onClick = onRetry,
            colors = androidx.compose.material3.ButtonDefaults.filledButtonColors(
                containerColor = Color(0xFFBB86FC)
            )
        ) {
            Text(text = "ลองใหม่", fontSize = 16.sp, color = Color.Black)
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.MovieFilter,
            contentDescription = "No content",
            modifier = Modifier.size(64.dp),
            tint = Color(0xFF666666)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color(0xFF999999),
            textAlign = androidx.compose.ui.text.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}