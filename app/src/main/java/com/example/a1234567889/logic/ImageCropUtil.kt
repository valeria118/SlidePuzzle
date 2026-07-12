package com.example.a1234567889.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Crops/edits an image picked by the user from their gallery into a square
 * image ready to be sliced into puzzle tiles, applying the same pan / zoom /
 * rotation the user set up in [com.example.a1234567889.ui.screens.ImageEditorScreen],
 * then stores a private copy of the result so it survives the lifetime of the
 * original (often temporary) content:// Uri granted by the system picker.
 */
object ImageCropUtil {

    private const val OUTPUT_SIZE = 1024

    suspend fun cropAndSave(
        context: Context,
        sourceUri: Uri,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotationDegrees: Float,
        viewportPx: Int
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmapRespectingExif(context, sourceUri) ?: return@withContext null
            val bw = bitmap.width.toFloat()
            val bh = bitmap.height.toFloat()
            val viewport = viewportPx.takeIf { it > 0 }?.toFloat() ?: maxOf(bw, bh)

            // ContentScale.Fit: how the bitmap was scaled/centered into the square viewport.
            val fitScale = minOf(viewport / bw, viewport / bh)
            val dx0 = (viewport - bw * fitScale) / 2f
            val dy0 = (viewport - bh * fitScale) / 2f

            // Mirrors the order of transforms applied in ImageEditorScreen's graphicsLayer:
            // pivot at the viewport center, then user scale -> rotate -> translate.
            val matrix = Matrix()
            matrix.postScale(fitScale, fitScale)
            matrix.postTranslate(dx0, dy0)
            matrix.postTranslate(-viewport / 2f, -viewport / 2f)
            matrix.postScale(scale.coerceAtLeast(0.01f), scale.coerceAtLeast(0.01f))
            matrix.postRotate(rotationDegrees)
            matrix.postTranslate(offsetX, offsetY)
            matrix.postTranslate(viewport / 2f, viewport / 2f)

            // Re-scale from the on-screen viewport size to our fixed output resolution.
            val outScale = OUTPUT_SIZE / viewport
            matrix.postScale(outScale, outScale)

            val output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.BLACK)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(bitmap, matrix, paint)
            bitmap.recycle()

            val dir = File(context.filesDir, "gallery").apply { mkdirs() }
            val file = File(dir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                output.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            output.recycle()
            Uri.fromFile(file)
        } catch (e: Exception) {
            null
        }
    }

    private fun loadBitmapRespectingExif(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val sampleSize = computeSampleSize(context, uri)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        val orientation = try {
            resolver.openInputStream(uri)?.use { ExifInterface(it) }
                ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        return applyExifRotation(bitmap, orientation)
    }

    private fun computeSampleSize(context: Context, uri: Uri): Int = try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        val maxDim = 2200
        while ((opts.outWidth / sample) > maxDim || (opts.outHeight / sample) > maxDim) sample *= 2
        sample
    } catch (e: Exception) {
        1
    }

    private fun applyExifRotation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
