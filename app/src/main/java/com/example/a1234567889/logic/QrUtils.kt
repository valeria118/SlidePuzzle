package com.example.a1234567889.logic

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Lab item 13 — "Чтение QR-кодов".
 *
 * Two halves:
 *  - [generateQrBitmap] turns any string (an invite code, a profile link, ...) into a
 *    scannable QR bitmap using the ZXing core encoder.
 *  - [rememberQrScanLauncher] wraps zxing-android-embedded's camera-based scanner
 *    Activity behind the modern AndroidX Activity Result API, ready to `.launch()`
 *    from a Composable button.
 */
object QrUtils {

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun defaultScanOptions(prompt: String = "Наведите камеру на QR-код"): ScanOptions =
        ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt(prompt)
            .setBeepEnabled(true)
            .setOrientationLocked(false)
}

/**
 * Remembers an [androidx.activity.result.ActivityResultLauncher] that opens the camera-based
 * QR scanner and calls [onResult] with the decoded text (or null if the user cancelled).
 */
@Composable
fun rememberQrScanLauncher(onResult: (String?) -> Unit) =
    rememberLauncherForActivityResult(ScanContract()) { result: ScanIntentResult ->
        onResult(result.contents)
    }
