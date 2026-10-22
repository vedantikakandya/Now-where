package com.example.now_where.ui.visualization.distance

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.TimeNotation
import com.example.now_where.ui.theme.NowWhereColors
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.sqrt

// Placeholders: swap for NowWhereColors members if you have matching ones.
private val AccentColor = Color(0xFF3F7568)
private val NowColor = Color(0xFFC9622F)
private val InkColor = Color(0xFF1E2521)
private val SubColor = Color(0xFF66716B)

// How far ahead the screen can "see". Anything further is pinned to the top.
private const val DEFAULT_WINDOW_MINUTES = 180f

/**
 * What the Distance view is pointing at right now.
 * Kept separate from the drawing so the Timer screen can reuse DistanceScene
 * with its own target (the end of the session).
 */
sealed interface DistanceTarget {
    data class Upcoming(val title: String, val minutesAway: Float) : DistanceTarget
    data class Arrived(val title: String) : DistanceTarget
    data object NothingLeft : DistanceTarget
}

fun DayState.toDistanceTarget(): DistanceTarget {
    val active = events.firstOrNull { it.start <= now && now < it.end }
    if (active != null) return DistanceTarget.Arrived(active.title)

    val next = events.filter { it.start > now }.minByOrNull { it.start }
        ?: return DistanceTarget.NothingLeft

    val seconds = java.time.Duration.between(now, next.start).seconds
    return DistanceTarget.Upcoming(next.title, seconds / 60f)
}

@Composable
fun DistanceVisualization(
    day: DayState,
    modifier: Modifier = Modifier,
    onEventClick: (Event) -> Unit = {},
    windowMinutes: Float = DEFAULT_WINDOW_MINUTES
) {
    val activeOrNext = day.events.firstOrNull { it.start <= day.now && day.now < it.end }
        ?: day.events.filter { it.start > day.now }.minByOrNull { it.start }

    DistanceScene(
        target = day.toDistanceTarget(),
        modifier = modifier,
        onEventClick = { activeOrNext?.let(onEventClick) },
        windowMinutes = windowMinutes
    )
}

@Composable
fun DistanceScene(
    target: DistanceTarget,
    modifier: Modifier = Modifier,
    onEventClick: () -> Unit = {},
    windowMinutes: Float = DEFAULT_WINDOW_MINUTES
) {
    // 0f = event is right on top of you, 1f = event is as far away as the screen shows.
    // sqrt keeps the last few minutes visibly distinct instead of squashing them to nothing.
    val rawDistance = when (target) {
        is DistanceTarget.Upcoming -> sqrt((target.minutesAway / windowMinutes).coerceIn(0f, 1f))
        is DistanceTarget.Arrived -> 0f
        DistanceTarget.NothingLeft -> 1f
    }

    // The event glides toward you as time passes; a soft spring on arrival.
    val distance by animateFloatAsState(
        targetValue = rawDistance,
        animationSpec = if (target is DistanceTarget.Arrived)
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        else
            tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "distance"
    )

    // Pulse ring around NOW, runs forever.
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseValue"
    )

    // Dashes slowly drift toward NOW so the line feels like it is moving.
    val dashTransition = rememberInfiniteTransition(label = "dash")
    val dashPhase by dashTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "dashPhase"
    )

    // Arrival ring that expands once the event is reached.
    val arrivedAmount by animateFloatAsState(
        targetValue = if (target is DistanceTarget.Arrived) 1f else 0f,
        animationSpec = tween(600),
        label = "arrived"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val trackTop = 96.dp
        val trackBottom = maxHeight - 140.dp
        val trackLength = trackBottom - trackTop

        // Event position: pinned at the top when far, sliding down to NOW when close.
        val eventY = trackBottom - trackLength * distance
        val centerX = maxWidth / 2

        Canvas(modifier = Modifier.matchParentSize()) {
            val x = centerX.toPx()
            val nowY = trackBottom.toPx()
            val evY = eventY.toPx()

            if (target !is DistanceTarget.NothingLeft) {
                // Dashed "path" between you and the event
                val dashLen = 10.dp.toPx()
                val gapLen = 8.dp.toPx()
                drawLine(
                    color = NowWhereColors.InkMuted,
                    start = Offset(x, evY),
                    end = Offset(x, nowY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashLen, gapLen),
                        phase = -(dashLen + gapLen) * dashPhase
                    )
                )
                // Solid green part nearest to you: the ground already covered
                drawLine(
                    color = AccentColor.copy(alpha = 0.35f),
                    start = Offset(x, nowY),
                    end = Offset(x, nowY - (nowY - evY) * 0.25f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Event marker
                drawCircle(AccentColor.copy(alpha = 0.18f * arrivedAmount), 34.dp.toPx(), Offset(x, evY))
                drawCircle(NowWhereColors.Background, 11.dp.toPx(), Offset(x, evY))
                drawCircle(AccentColor, 11.dp.toPx(), Offset(x, evY), style = Stroke(3.dp.toPx()))
                drawCircle(AccentColor, 5.dp.toPx() + 3.dp.toPx() * arrivedAmount, Offset(x, evY))
            }

            // NOW marker with pulse ring
            drawCircle(
                NowColor.copy(alpha = 0.85f * (1f - pulse)),
                radius = 9.dp.toPx() + 40.dp.toPx() * pulse,
                center = Offset(x, nowY)
            )
            drawCircle(NowColor, 8.dp.toPx(), Offset(x, nowY))
        }

        // Event title follows the marker
        if (target !is DistanceTarget.NothingLeft) {
            val title = when (target) {
                is DistanceTarget.Upcoming -> target.title
                is DistanceTarget.Arrived -> target.title
                DistanceTarget.NothingLeft -> ""
            }
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (eventY - 56.dp).roundToPx()) }
                    .clickable(onClick = onEventClick)
            )
        }

        // Plain-language caption near NOW
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(text = "now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NowColor)
            Crossfade(targetState = caption(target), animationSpec = tween(400), label = "caption") { text ->
                Text(
                    text = text,
                    fontSize = 15.sp,
                    color = SubColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun caption(target: DistanceTarget): String = when (target) {
    is DistanceTarget.Upcoming -> formatAway(target.minutesAway)
    is DistanceTarget.Arrived -> "You're here"
    DistanceTarget.NothingLeft -> "Nothing else today"
}

private fun formatAway(minutes: Float): String {
    val total = minutes.toInt().coerceAtLeast(0)
    val h = total / 60
    val m = total % 60
    return when {
        total < 1 -> "less than a minute away"
        h == 0 -> "$m min away"
        m == 0 -> "$h h away"
        else -> "$h h $m min away"
    }
}

// ---------------- Previews ----------------

private fun previewDay(now: LocalTime): DayState {
    val today = LocalDate.now()
    return DayState(
        now = today.atTime(now),
        events = listOf(
            com.example.now_where.domain.Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
            com.example.now_where.domain.Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
            com.example.now_where.domain.Event(3, "Gym", today.atTime(17, 0), today.atTime(18, 30)),
        ),
        notion = TimeNotation.H24
    )
}

@Preview(name = "Far (2h 40m)", widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun DistanceFarPreview() {
    Box(Modifier.fillMaxSize().background(NowWhereColors.Background)) {
        DistanceVisualization(previewDay(LocalTime.of(14, 20)))
    }
}

@Preview(name = "Close (12 min)", widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun DistanceClosePreview() {
    Box(Modifier.fillMaxSize().background(NowWhereColors.Background)) {
        DistanceVisualization(previewDay(LocalTime.of(16, 48)))
    }
}

@Preview(name = "Arrived", widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun DistanceArrivedPreview() {
    Box(Modifier.fillMaxSize().background(NowWhereColors.Background)) {
        DistanceVisualization(previewDay(LocalTime.of(17, 10)))
    }
}