package com.example.a1234567889.logic

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import androidx.core.content.FileProvider
import com.example.a1234567889.models.CollectedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a [CollectedCard] (its photo with the rank frame drawn on top, exactly
 * like it looks in the gallery/reveal UI) into a single PNG bitmap and shares it
 * through the system share sheet, using the app's FileProvider.
 */
object CardShareUtils {

    private const val AUTHORITY = "com.example.a1234567889.fileprovider"

    /** Builds the composed card bitmap: photo (cropped to the frame's aspect ratio) + frame on top. */
    private fun renderCardBitmap(context: Context, card: CollectedCard): Bitmap? {
        val frameName = CardRewardLogic.frameRes(card.rank)
        val frameResId = context.resources.getIdentifier(frameName, "drawable", context.packageName)
        val photoResId = context.resources.getIdentifier(card.photoRes, "drawable", context.packageName)
        if (frameResId == 0) return null

        val frameBitmap = BitmapFactory.decodeResource(context.resources, frameResId) ?: return null
        val w = frameBitmap.width
        val h = frameBitmap.height

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        if (photoResId != 0) {
            val photoBitmap = BitmapFactory.decodeResource(context.resources, photoResId)
            if (photoBitmap != null) {
                // Center-crop the photo into the frame's aspect ratio (same effect as ContentScale.Crop).
                val srcRatio = photoBitmap.width.toFloat() / photoBitmap.height
                val dstRatio = w.toFloat() / h
                val srcRect = if (srcRatio > dstRatio) {
                    val cropWidth = (photoBitmap.height * dstRatio).toInt()
                    val x0 = (photoBitmap.width - cropWidth) / 2
                    Rect(x0, 0, x0 + cropWidth, photoBitmap.height)
                } else {
                    val cropHeight = (photoBitmap.width / dstRatio).toInt()
                    val y0 = (photoBitmap.height - cropHeight) / 2
                    Rect(0, y0, photoBitmap.width, y0 + cropHeight)
                }
                canvas.drawBitmap(photoBitmap, srcRect, Rect(0, 0, w, h), null)
                photoBitmap.recycle()
            }
        }

        canvas.drawBitmap(frameBitmap, Rect(0, 0, w, h), Rect(0, 0, w, h), null)
        frameBitmap.recycle()
        return output
    }

    /** Renders, saves to cache and shares the card via the system share sheet. */
    suspend fun shareCard(context: Context, card: CollectedCard) {
        val uri = withContext(Dispatchers.IO) {
            try {
                val bitmap = renderCardBitmap(context, card) ?: return@withContext null
                val dir = File(context.cacheDir, "cards").apply { mkdirs() }
                val file = File(dir, "card_${card.id}.png")
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
                bitmap.recycle()
                FileProvider.getUriForFile(context, AUTHORITY, file)
            } catch (e: Exception) {
                null
            }
        } ?: return

        val rankLabel = CardRewardLogic.rankLabel(card.rank)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$rankLabel карточка «${card.sourceTitle}» из моей коллекции пазлов!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Поделиться карточкой").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
