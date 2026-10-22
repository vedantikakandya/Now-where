package com.example.now_where.ui.theme


import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext

/**
 * Whether touch haptics are on. HomeScreen provides the real value from the saved setting.
 * Dialogs live in their own window but still inherit this local, so it also reaches them.
 */
val LocalHapticsEnabled = compositionLocalOf { true }

/**
 * Vibrates once on every finger-down inside this element, if haptics are ON.
 *
 * It watches the Initial pass and never consumes anything, so buttons, scrolling and
 * every other gesture inside keep working as before. Put it on:
 *  - the root of HomeScreen (covers the whole app screen), and
 *  - the root content of each Dialog (a Dialog is a separate window, so the root can't see it).
 */
fun Modifier.touchHaptics(): Modifier = composed {
    val context = LocalContext.current
    val enabled by rememberUpdatedState(LocalHapticsEnabled.current)

    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            VibrationHelper.performTouchHaptic(context, enabled)
            // Wait until every finger is up, so dragging or a second finger doesn't re-trigger
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
            } while (event.changes.any { it.pressed })
        }
    }
}