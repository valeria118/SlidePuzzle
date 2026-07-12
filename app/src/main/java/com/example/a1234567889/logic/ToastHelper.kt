package com.example.a1234567889.logic

import android.content.Context
import android.view.Gravity
import android.widget.Toast

/**
 * Lab item 3 — "Всплывающие сообщения Toast. Настройка положения и длительности".
 *
 * A thin wrapper around [Toast] that lets every call site (and the demo controls in
 * SettingsScreen) configure the on-screen *position* (gravity) and *duration* of the
 * message, instead of always relying on the default bottom/short Toast.
 */
object ToastHelper {

    enum class Position { TOP, CENTER, BOTTOM }

    /** Shows a Toast at the default (bottom, short) position — convenience overload. */
    fun show(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows a Toast with a custom [position] and [durationMs].
     * Android's Toast only natively supports LENGTH_SHORT (~2s) / LENGTH_LONG (~3.5s),
     * so for arbitrary custom durations we schedule a matching `cancel()` ourselves.
     */
    fun show(
        context: Context,
        message: String,
        position: Position = Position.BOTTOM,
        durationMs: Long = Toast.LENGTH_SHORT.toLong()
    ) {
        val isLong = durationMs > 2500
        val toast = Toast.makeText(context, message, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT)
        when (position) {
            Position.TOP -> toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 120)
            Position.CENTER -> toast.setGravity(Gravity.CENTER, 0, 0)
            Position.BOTTOM -> toast.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 160)
        }
        toast.show()

        // For an explicit custom duration that doesn't match either Android default,
        // cancel the toast ourselves once that time has elapsed.
        val defaultDurationMs = if (isLong) 3500L else 2000L
        if (durationMs != defaultDurationMs && durationMs > 0) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toast.cancel()
            }, durationMs)
        }
    }
}
