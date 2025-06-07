package com.example.photoswipecleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.example.photoswipecleaner.ui.theme.PhotoSwipeCleanerTheme
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasAllFilesAccess()) {
            requestAllFilesPermission(this)
        }

        setContent {
            PhotoSwipeCleanerTheme {
                val imageFiles = remember { mutableStateListOf<File>() }
                val scope = rememberCoroutineScope()
                var currentIndex by remember { mutableStateOf(0) }
                var lastDeletedFile by remember { mutableStateOf<File?>(null) }

                var totalDeleted by remember { mutableStateOf(0) }
                var totalFreedBytes by remember { mutableStateOf(0L) }

                LaunchedEffect(Unit) {
                    imageFiles.clear()
                    imageFiles.addAll(getAllImages(this@MainActivity))
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0A)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🧼 Photo Swipe Cleaner",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )

                        Text(
                            text = "$totalDeleted deleted • ${"%.1f".format(totalFreedBytes / (1024f * 1024f))} MB cleaned",
                            color = Color(0xFFAAAAAA),
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (currentIndex >= imageFiles.size) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎉 All done!", color = Color.White, fontSize = 22.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("$totalDeleted photos deleted", color = Color.Gray)
                                Text("${"%.1f".format(totalFreedBytes / (1024f * 1024f))} MB cleaned", color = Color.Gray)
                            }
                        } else {
                            val currentImage = imageFiles[currentIndex]
                            val offsetX = remember { Animatable(0f) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .pointerInput(currentImage) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                when {
                                                    offsetX.value > 300f -> {
                                                        scope.launch {
                                                            offsetX.animateTo(1000f, tween(300))
                                                            offsetX.snapTo(0f)
                                                            lastDeletedFile = null
                                                            currentIndex++
                                                        }
                                                    }
                                                    offsetX.value < -300f -> {
                                                        val deleted = currentImage
                                                        val size = deleted.length()
                                                        if (deleted.delete()) {
                                                            lastDeletedFile = deleted
                                                            totalDeleted++
                                                            totalFreedBytes += size
                                                        }
                                                        scope.launch {
                                                            offsetX.animateTo(-1000f, tween(300))
                                                            offsetX.snapTo(0f)
                                                            currentIndex++
                                                        }
                                                    }
                                                    else -> {
                                                        scope.launch {
                                                            offsetX.animateTo(0f, tween(300))
                                                        }
                                                    }
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                scope.launch {
                                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (offsetX.value > 50f) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Keep",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(24.dp)
                                            .size(36.dp)
                                            .graphicsLayer {
                                                alpha = (offsetX.value / 300f).coerceIn(0f, 1f)
                                            }
                                    )
                                } else if (offsetX.value < -50f) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(24.dp)
                                            .size(36.dp)
                                            .graphicsLayer {
                                                alpha = (-offsetX.value / 300f).coerceIn(0f, 1f)
                                            }
                                    )
                                }

                                GlideImage(
                                    model = currentImage,
                                    contentDescription = "Photo",
                                    modifier = Modifier
                                        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                        .graphicsLayer {
                                            alpha = (1f - (kotlin.math.abs(offsetX.value) / 1000f)).coerceIn(0.6f, 1f)
                                            rotationZ = (offsetX.value / 40f).coerceIn(-8f, 8f)
                                        }
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.85f)
                                        .clip(RoundedCornerShape(18.dp))
                                        .shadow(10.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            lastDeletedFile?.let { deletedFile ->
                                Button(
                                    onClick = {
                                        val restored = File(deletedFile.absolutePath)
                                        restored.outputStream().use { output ->
                                            deletedFile.inputStream().copyTo(output)
                                        }
                                        imageFiles.add(currentIndex, restored)
                                        lastDeletedFile = null
                                        totalDeleted--
                                        totalFreedBytes -= deletedFile.length()
                                        currentIndex--
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Undo Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
