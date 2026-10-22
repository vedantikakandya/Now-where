package com.example.now_where.ui.visualization.route

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.EventPhase
import com.example.now_where.domain.TimeFormat
import com.example.now_where.domain.TimeNotation
import com.example.now_where.domain.phaseAt
import com.example.now_where.ui.theme.NowWhereColors
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---- Layout constants (tweak these to change how the route "feels") ----------------------

/** Vertical length of one hour. THIS is what makes "far away" look far and "soon" look close. */
private val HourHeight = 72.dp
private val RowHeight = 44.dp

/** Width of the middle column that holds the path line and the dots. */
private val PathColumnWidth = 44.dp

/** NOW sits this far down the screen (0 = top, 1 = bottom): some past above, more future below. */
private const val NowAnchorFraction = 0.38f

/** Events further away than this fade to their dimmest. */
private const val ProximityHorizonMinutes = 240f

/**
 * Route: the day as a vertical path. Past is behind (above, faded), NOW is the glowing
 * marker, upcoming events are ahead (below) and brighten as they get closer.
 *
 * Position on screen = real time. Because the whole day is laid out to scale and NOW is
 * just a point on it, "NOW moving" and "events getting closer" are the same thing.
 */
@Composable
fun RouteVisualization(
    day: DayState,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val anchor = maxHeight * NowAnchorFraction
        // Extra space above midnight / below the end of the day so NOW can sit at the anchor
        // even at 00:05 or 23:55.
        val topPad = anchor
        val bottomPad = maxHeight - anchor
        val totalHeight = topPad + HourHeight * 24 + bottomPad

        fun yOf(time: LocalDateTime): Dp {
            val seconds = Duration.between(day.startOfDay, time).seconds.coerceIn(0, 24 * 3600L)
            return topPad + HourHeight * (seconds / 3600f)
        }

        val nowY = yOf(day.now)

        // Open the screen already scrolled so NOW is at the anchor (no visible jump).
        val scrollState = rememberScrollState(
            initial = with(density) { (nowY - anchor).roundToPx() }
        )

        Box(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Box(Modifier.fillMaxWidth().height(totalHeight)) {

                val items = day.sortedEvents.map { event ->
                    val phase = event.phaseAt(day.now)
                    val proximity = proximityOf(day, event, phase)
                    RouteItem(event, phase, proximity, yOf(event.start), yOf(event.end))
                }

                // 1) The path itself, hour ticks, and duration bars drawn on it.
                PathCanvas(nowY = nowY, topPad = topPad, items = items)

                // 2) Hourly timestamps along the route.
                for (h in 0..24) {
                    val hourTime = day.startOfDay.plusHours(h.toLong())
                    val hourY = yOf(hourTime)
                    val past = hourTime.isBefore(day.now)
                    HourlyRow(
                        y = hourY,
                        label = hourLabel(h % 24, day.notation),
                        past = past,
                    )
                }

                // 3) Events.
                items.forEach { item ->
                    EventRow(
                        item = item,
                        isNext = item.event == day.nextEvent,
                        day = day,
                    )
                }

                // 4) NOW on top of everything.
                NowMarker(
                    y = nowY,
                    timeText = TimeFormat.clock(day.now, day.notation),
                    reduceMotion = reduceMotion,
                )
            }
        }
    }
}

// ---- Data prepared once per tick -----------------------------------------------------------

private data class RouteItem(
    val event: Event,
    val phase: EventPhase,
    /** 0 = far away / past, 1 = happening now. Drives brightness. */
    val proximity: Float,
    val startY: Dp,
    val endY: Dp,
)

private fun proximityOf(day: DayState, event: Event, phase: EventPhase): Float = when (phase) {
    EventPhase.PAST -> 0f
    EventPhase.ONGOING -> 1f
    EventPhase.UPCOMING ->
        1f - (day.minutesUntil(event) / ProximityHorizonMinutes).coerceIn(0f, 1f)
}

private fun RouteItem.tint(): Color = when (phase) {
    EventPhase.PAST -> NowWhereColors.Ink.copy(alpha = 0.18f)
    EventPhase.ONGOING -> NowWhereColors.Orange
    EventPhase.UPCOMING -> NowWhereColors.Peach.copy(alpha = lerpFloat(0.25f, 1f, proximity))
}

private fun RouteItem.textColor(): Color = when (phase) {
    EventPhase.PAST -> NowWhereColors.InkMuted.copy(alpha = 0.45f)
    EventPhase.ONGOING -> NowWhereColors.Ink
    EventPhase.UPCOMING -> lerp(NowWhereColors.InkMuted.copy(alpha = 0.55f), NowWhereColors.Ink, proximity)
}

private fun lerpFloat(a: Float, b: Float, t: Float) = a + (b - a) * t

private fun hourLabel(hour24: Int, notation: TimeNotation): String =
    if (notation == TimeNotation.H24) {
        "%02d:00".format(hour24)
    } else {
        val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val suffix = if (hour24 in 12..23) "pm" else "am"
        "$h12 $suffix"
    }

// ---- Pieces ---------------------------------------------------------------------------------

@Composable
private fun PathCanvas(nowY: Dp, topPad: Dp, items: List<RouteItem>) {
    Canvas(Modifier.fillMaxSize()) {
        val x = size.width / 2f
        val nowPx = nowY.toPx()
        val line = 2.dp.toPx()

        // Behind you: a dim, settled line.
        drawLine(
            color = NowWhereColors.Ink.copy(alpha = 0.10f),
            start = Offset(x, 0f),
            end = Offset(x, nowPx),
            strokeWidth = line,
        )

        // Ahead of you: starts warm at NOW and fades into the distance.
        drawLine(
            brush = Brush.verticalGradient(
                0f to NowWhereColors.Orange.copy(alpha = 0.7f),
                1f to NowWhereColors.Ink.copy(alpha = 0.06f),
                startY = nowPx,
                endY = nowPx + HourHeight.toPx() * 4,
            ),
            start = Offset(x, nowPx),
            end = Offset(x, size.height),
            strokeWidth = line,
        )

        // Hour ticks along the path line
        for (h in 0..24) {
            val yPx = (topPad + HourHeight * h).toPx()
            val tickWidth = 5.dp.toPx()
            drawLine(
                color = NowWhereColors.Ink.copy(alpha = 0.15f),
                start = Offset(x - tickWidth, yPx),
                end = Offset(x + tickWidth, yPx),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Each event's LENGTH on the path = how long it lasts.
        items.forEach { item ->
            drawLine(
                color = item.tint().copy(alpha = if (item.phase == EventPhase.PAST) 0.12f else 0.55f * item.tint().alpha),
                start = Offset(x, item.startY.toPx()),
                end = Offset(x, item.endY.toPx()),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * One horizontal row on the route: [time] — [dot on the path] — [label].
 * Left and right halves are equal width, so the dot always lands exactly on the line.
 */
@Composable
private fun RouteRow(
    y: Dp,
    description: String,
    startContent: @Composable BoxScope.() -> Unit,
    marker: @Composable BoxScope.() -> Unit,
    endContent: @Composable BoxScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .offset(y = y - RowHeight / 2)
            .fillMaxWidth()
            .height(RowHeight)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(end = 6.dp), contentAlignment = Alignment.CenterEnd, content = startContent)
        Box(Modifier.width(PathColumnWidth), contentAlignment = Alignment.Center, content = marker)
        Box(Modifier.weight(1f).padding(start = 6.dp), contentAlignment = Alignment.CenterStart, content = endContent)
    }
}

@Composable
private fun HourlyRow(y: Dp, label: String, past: Boolean) {
    RouteRow(
        y = y,
        description = label,
        startContent = {
            Text(
                text = label,
                color = if (past) NowWhereColors.InkMuted.copy(alpha = 0.25f) else NowWhereColors.InkMuted.copy(alpha = 0.45f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )
        },
        marker = {
            Box(
                Modifier
                    .size(4.dp)
                    .background(
                        if (past) NowWhereColors.InkMuted.copy(alpha = 0.20f) else NowWhereColors.InkMuted.copy(alpha = 0.40f),
                        CircleShape
                    )
            )
        },
        endContent = {}
    )
}

@Composable
private fun EventRow(item: RouteItem, isNext: Boolean, day: DayState) {
    val timeText = TimeFormat.clock(item.event.start, day.notation)
    val distanceText = when {
        isNext -> "in ${TimeFormat.duration(day.minutesUntil(item.event))}"
        item.phase == EventPhase.ONGOING -> "now"
        else -> null
    }
    val description = buildString {
        append(item.event.title).append(", ").append(timeText)
        if (distanceText != null) append(", ").append(distanceText)
    }

    RouteRow(
        y = item.startY,
        description = description,
        startContent = {
            Text(
                text = timeText,
                color = item.textColor(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
            )
        },
        marker = {
            Box(
                Modifier
                    .size(if (isNext) 12.dp else 9.dp)
                    .background(item.tint(), CircleShape)
            )
        },
        endContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.event.title,
                    color = item.textColor(),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (distanceText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = distanceText,
                        color = NowWhereColors.Peach.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
            }
        },
    )
}

@Composable
private fun NowMarker(y: Dp, timeText: String, reduceMotion: Boolean) {
    // A slow breathing glow. With "Reduce motion" on, it holds still.
    val glowAlpha = if (reduceMotion) {
        0.35f
    } else {
        rememberInfiniteTransition(label = "nowGlow").animateFloat(
            initialValue = 0.22f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "nowGlowAlpha",
        ).value
    }

    val glowSize = 180.dp
    Canvas(
        Modifier
            .offset(y = y - glowSize / 2)
            .fillMaxWidth()
            .height(glowSize)
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NowWhereColors.Orange.copy(alpha = glowAlpha), Color.Transparent),
                center = center,
                radius = size.height / 2f,
            ),
            radius = size.height / 2f,
            center = center,
        )
    }

    RouteRow(
        y = y,
        description = "Now, $timeText",
        startContent = {
            Text(
                text = timeText,
                color = NowWhereColors.Peach,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        marker = {
            Box(
                Modifier
                    .size(22.dp)
                    .background(NowWhereColors.Orange.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(12.dp).background(NowWhereColors.Orange, CircleShape))
            }
        },
        endContent = {
            Text(
                text = "NOW",
                color = NowWhereColors.Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        },
    )
}

// ---- Preview --------------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun RouteVisualizationPreview() {
    val today = LocalDate.now()
    RouteVisualization(
        day = DayState(
            now = today.atTime(LocalTime.of(15, 0)),
            events = listOf(
                Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
                Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
                Event(3, "Gym", today.atTime(17, 0), today.atTime(18, 30)),
                Event(4, "Study", today.atTime(19, 0), today.atTime(20, 30)),
                Event(5, "Dinner", today.atTime(21, 0), today.atTime(22, 0)),
            ),
            notion = TimeNotation.H24,
        )
    )
}
