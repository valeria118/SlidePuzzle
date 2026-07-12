package com.example.a1234567889.logic

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Lab item 16 — "Сохранение скриншотов. Работа с файловой системой".
 *
 * Saves a captured [Bitmap] in two places:
 *  1. The public Pictures gallery (via MediaStore on API 29+, or a direct file write +
 *     legacy WRITE_EXTERNAL_STORAGE permission on older devices) — so it shows up
 *     in the user's Gallery / Photos app.
 *  2. The app's private file-system directory ([Context.filesDir]) — a plain
 *     [FileOutputStream] write, demonstrating direct filesystem access.
 */
object ScreenshotUtil {

    private const val SUBFOLDER = "SlidePuzzle"

    fun saveScreenshot(context: Context, bitmap: Bitmap): Uri? {
        val filename = "puzzle_${System.currentTimeMillis()}.png"

        // (2) Raw file-system copy, always performed regardless of API level.
        try {
            val internalDir = File(context.filesDir, "screenshots").apply { if (!exists()) mkdirs() }
            val internalFile = File(internalDir, filename)
            FileOutputStream(internalFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        } catch (_: Exception) {
            // Non-fatal: the gallery copy below is the one the user actually cares about.
        }

        // (1) Public gallery copy.
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + SUBFOLDER)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { u ->
                    context.contentResolver.openOutputStream(u)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                uri
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), SUBFOLDER)
                if (!picturesDir.exists()) picturesDir.mkdirs()
                val file = File(picturesDir, filename)
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Opens the system share sheet (Intent.ACTION_SEND) for the saved screenshot. */
    fun shareScreenshot(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться скриншотом").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
