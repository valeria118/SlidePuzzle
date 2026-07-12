package com.example.a1234567889.logic

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Lab item 14 — "Вызов сторонних приложений: телефон, браузер, email, Google Play. Intent".
 *
 * Every function defensively catches [ActivityNotFoundException] so a missing
 * handler app (e.g. no email client on an emulator) never crashes the game —
 * it just shows an explanatory Toast instead (ties in lab item 3 too).
 */
object IntentHelper {

    private fun safeStart(context: Context, intent: Intent, errorMessage: String) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK.takeIf { context !is android.app.Activity } ?: 0)
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            ToastHelper.show(context, errorMessage)
        }
    }

    /** Opens the phone dialer pre-filled with [phoneNumber] (does not call automatically). */
    fun dialPhone(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        safeStart(context, intent, "Не найдено приложение для звонков")
    }

    /** Opens [url] in the user's default browser. */
    fun openBrowser(context: Context, url: String) {
        val safeUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
        safeStart(context, intent, "Не найден браузер")
    }

    /** Opens an email client with [to], [subject] and [body] pre-filled. */
    fun sendEmail(context: Context, to: String, subject: String = "", body: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        safeStart(context, intent, "Не найдено приложение почты")
    }

    /** Opens the app's own Play Store listing (falls back to the browser store page). */
    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            openBrowser(context, "https://play.google.com/store/apps/details?id=$packageName")
        }
    }

    /** Generic "Share text" intent — used for invite codes, results, etc. */
    fun shareText(context: Context, text: String, title: String = "Поделиться") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        safeStart(context, Intent.createChooser(intent, title), "Нет приложений для отправки")
    }
}
