package com.example.a1234567889.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    imageUri: Uri,
    onSave: (Uri, Float, Float, Float, Float, Int) -> Unit, // Uri, zoom, offsetX, offsetY, rotation, viewportPx
    onCancel: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var viewportPx by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена")
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(imageUri, scale, offset.x, offset.y, rotation, viewportPx) }) {
                        Icon(Icons.Default.Check, contentDescription = "Применить", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { rotation = (rotation - 90f) % 360f }) {
                        Icon(Icons.Default.RotateLeft, "Повернуть влево")
                    }
                    IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                        Icon(Icons.Default.RotateRight, "Повернуть вправо")
                    }
                    IconButton(onClick = { 
                        scale = 1f
                        offset = androidx.compose.ui.geometry.Offset.Zero
                        rotation = 0f
                    }) {
                        Icon(Icons.Default.Refresh, "Сброс")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Preview Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .onSizeChanged { viewportPx = it.width }
                    .clip(RoundedCornerShape(4.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(4.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale *= zoom
                            offset += pan
                        }
                    }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y,
                            rotationZ = rotation
                        ),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Mask for the square crop area
            CanvasMask()
            
            Text(
                "Перемещайте и масштабируйте изображение внутри рамки",
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun CanvasMask() {
    // Semi-transparent overlay outside the square
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val squareSize = size.width
        val top = (size.height - squareSize) / 2
        
        // Top rect
        drawRect(Color.Black.copy(alpha = 0.5f), size = androidx.compose.ui.geometry.Size(size.width, top))
        // Bottom rect
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = androidx.compose.ui.geometry.Offset(0f, top + squareSize), size = androidx.compose.ui.geometry.Size(size.width, size.height - (top + squareSize)))
    }
}
