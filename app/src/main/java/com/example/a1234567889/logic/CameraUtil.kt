package com.example.a1234567889.logic

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Lab item 17 — "Работа с камерой. Вызов камеры, получение фото и видео. Разрешения".
 *
 * Launches the system Camera app via [ActivityResultContracts.TakePicture] /
 * [ActivityResultContracts.CaptureVideo], writing the result into a FileProvider-backed
 * Uri (no raw CAMERA-permission file access needed on modern Android — only the
 * runtime CAMERA permission has to be granted first).
 */
object CameraUtil {

    fun hasCameraPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Re-uses the already-declared `gallery_images` FileProvider path (files/gallery/). */
    fun createImageUri(context: Context): Uri {
        val dir = File(context.filesDir, "gallery").apply { if (!exists()) mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Re-uses the already-declared `card_shares` FileProvider path (cache/cards/). */
    fun createVideoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "cards").apply { if (!exists()) mkdirs() }
        val file = File(dir, "video_${System.currentTimeMillis()}.mp4")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}

/**
 * Remembers a ready-to-call lambda that launches the camera for a still photo.
 * [onPhotoTaken] receives the saved content Uri on success.
 */
@Composable
fun rememberCameraPhotoLauncher(onPhotoTaken: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingUri?.let(onPhotoTaken)
    }
    return {
        val uri = CameraUtil.createImageUri(context)
        pendingUri = uri
        launcher.launch(uri)
    }
}

/**
 * Remembers a ready-to-call lambda that launches the camera for a short video clip.
 * [onVideoTaken] receives the saved content Uri on success.
 */
@Composable
fun rememberCameraVideoLauncher(onVideoTaken: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        if (success) pendingUri?.let(onVideoTaken)
    }
    return {
        val uri = CameraUtil.createVideoUri(context)
        pendingUri = uri
        launcher.launch(uri)
    }
}
