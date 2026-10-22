package com.example.now_where.ui.visualization.ladder

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.TimeNotation
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design)
// ---------------------------------------------------------------------------

private object Pal {
    val Bg = Color(0xFF070E15)
    val Rail = Color(0xFF26313C)
    val Rung = Color(0xFF1E2832)
    val Knob = Color(0xFF2E3843)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF5E6B78)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val OrangeLight = Color(0xFFFFDCC2)
    val Blue = Color(0xFF5BB8F0)
    val BlueLight = Color(0xFFCFE8FF)
}

private val Mono = FontFamily.Monospace

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

// ---------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------

private enum class RungKind { FUTURE, NEXT, NOW, PAST, HOURLY }

private data class Rung(
    val kind: RungKind,
    val event: Event?,       // null for NOW or HOURLY
    val timeMinutes: Int,    // minute of day
    val title: String,
    val subtitle: String,
    val distance: Int        // rungs away from NOW
) {
    val height: Dp
        get() = when (kind) {
            RungKind.NOW -> 128.dp
            RungKind.NEXT -> 100.dp
            RungKind.FUTURE, RungKind.PAST -> 80.dp
            RungKind.HOURLY -> 44.dp
        }
}

private fun subtitleFor(title: String): String {
    val t = title.lowercase()
    return when {
        listOf("study", "focus", "code", "coding", "work", "project", "read").any { it in t } -> "Deep Flow"
        listOf("lunch", "dinner", "breakfast", "meal", "eat").any { it in t } -> "Unwind"
        listOf("gym", "workout", "run", "walk", "exercise", "sport").any { it in t } -> "Momentum"
        else -> "Ahead"
    }
}

/** Top to bottom: furthest future first, NOW in the middle, most recent past last. Includes hourly timestamps. */
private fun buildLadder(day: DayState): List<Rung> {
    val now = day.now
    val nowMinute = now.minuteOfDay()
    val upcomingEvents = day.events.filter { it.start > now }.sortedBy { it.start }
    val active = day.events.firstOrNull { it.start <= now && now < it.end }
    val pastEvents = day.events.filter { it.end <= now }.sortedByDescending { it.end }
    val nextId = upcomingEvents.firstOrNull()?.id

    // Collect event start minutes to avoid duplicate hourly rungs at exact event start hours
    val eventStartMinutes = day.events.map { it.start.minuteOfDay() }.toSet()

    // 1. Future items (timeMinutes > nowMinute), sorted descending (furthest future first)
    val futureRungs = mutableListOf<Rung>()
    val futureHours = (0..23).filter { h -> h * 60 > nowMinute && (h * 60) !in eventStartMinutes }

    upcomingEvents.forEach { e ->
        val isNext = e.id == nextId
        futureRungs.add(
            Rung(
                kind = if (isNext) RungKind.NEXT else RungKind.FUTURE,
                event = e,
                timeMinutes = e.start.minuteOfDay(),
                title = e.title,
                subtitle = if (isNext) "Next Step" else subtitleFor(e.title),
                distance = 0
            )
        )
    }

    futureHours.forEach { h ->
        val min = h * 60
        futureRungs.add(
            Rung(
                kind = RungKind.HOURLY,
                event = null,
                timeMinutes = min,
                title = "",
                subtitle = "",
                distance = 0
            )
        )
    }

    val sortedFutureTemp = futureRungs.sortedByDescending { it.timeMinutes }
    val sortedFuture = sortedFutureTemp.mapIndexed { index, rung ->
        rung.copy(distance = sortedFutureTemp.size - index)
    }

    // 2. NOW item
    val nowRung = Rung(
        kind = RungKind.NOW,
        event = active,
        timeMinutes = nowMinute,
        title = "NOW",
        subtitle = active?.let { "In ${it.title}" } ?: "Breath & Prepare",
        distance = 0
    )

    // 3. Past items (timeMinutes <= nowMinute), sorted descending (most recent past closest to NOW)
    val pastRungs = mutableListOf<Rung>()
    val pastHours = (0..23).filter { h -> h * 60 <= nowMinute && (h * 60) !in eventStartMinutes }

    pastEvents.forEach { e ->
        pastRungs.add(
            Rung(
                kind = RungKind.PAST,
                event = e,
                timeMinutes = e.start.minuteOfDay(),
                title = e.title,
                subtitle = "Ascended",
                distance = 0
            )
        )
    }

    pastHours.forEach { h ->
        val min = h * 60
        pastRungs.add(
            Rung(
                kind = RungKind.HOURLY,
                event = null,
                timeMinutes = min,
                title = "",
                subtitle = "",
                distance = 0
            )
        )
    }

    val sortedPast = pastRungs.sortedByDescending { it.timeMinutes }
        .mapIndexed { index, rung ->
            rung.copy(distance = index + 1)
        }

    return sortedFuture + nowRung + sortedPast
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun LadderVisualization(
    day: DayState,
    modifier: Modifier = Modifier
) {
    val ladder = remember(day) { buildLadder(day) }
    val remainingTasks = ladder.count { (it.kind == RungKind.FUTURE || it.kind == RungKind.NEXT) && it.event != null }
    val maxDistance = ladder.maxOf { it.distance }
    val nowMinute = day.now.minuteOfDay()

    // Rungs appear one after another, spreading outward from NOW
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseValue"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Pal.Bg)
    ) {
        Header(remaining = remainingTasks, modifier = Modifier.enterAnimation(0))

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val w = maxWidth
            val viewportH = maxHeight
            val density = LocalDensity.current
            val scroll = rememberScrollState()

            val topPad = 40.dp
            val above = ladder.takeWhile { it.kind != RungKind.NOW }.fold(0.dp) { acc, r -> acc + r.height }
            val nowRung = ladder.first { it.kind == RungKind.NOW }
            val nowCenter = topPad + above + nowRung.height / 2

            // Glide so NOW sits in the middle of the viewport
            LaunchedEffect(Unit) {
                delay(500)
                val target = with(density) { (nowCenter - viewportH / 2).roundToPx() }
                scroll.animateScrollTo(target.coerceAtLeast(0), tween(900, easing = FastOutSlowInEasing))
            }

            Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
                // Rails and the warm glow behind NOW
                Box(modifier = Modifier.matchParentSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val leftX = size.width * 0.257f
                        val rightX = size.width * 0.743f
                        val rail = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.08f to Pal.Rail,
                            0.92f to Pal.Rail,
                            1f to Color.Transparent
                        )
                        drawLine(rail, Offset(leftX, 0f), Offset(leftX, size.height), 1.5.dp.toPx())
                        drawLine(rail, Offset(rightX, 0f), Offset(rightX, size.height), 1.5.dp.toPx())

                        val glowCenter = Offset(size.width / 2f, nowCenter.toPx())
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Pal.Orange.copy(alpha = 0.09f), Color.Transparent),
                                center = glowCenter, radius = 240.dp.toPx()
                            ),
                            radius = 240.dp.toPx(), center = glowCenter
                        )
                    }
                }

                Column {
                    Spacer(Modifier.height(topPad))
                    ladder.forEach { rung ->
                        val appear = (intro.value * (maxDistance + 2) - rung.distance).coerceIn(0f, 1f)
                        RungRow(
                            rung = rung,
                            w = w,
                            appear = appear,
                            pulse = pulse,
                            day = day,
                            nowMinute = nowMinute,
                            allEvents = day.events
                        )
                    }
                    Spacer(Modifier.height(96.dp)) // room for the common AddButton
                }
            }

            // Soft fades at the top and bottom edges of the climb
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Pal.Bg, Color.Transparent)))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Pal.Bg)))
            )
        }
    }
}

@Composable
private fun Header(remaining: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Text(
                text = "THE CLIMB",
                fontFamily = Mono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = Pal.OrangeSoft
            )
            Text(
                text = "One rung at a time.",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = Pal.Ink,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "REMAINING",
                fontFamily = Mono,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = Pal.Sub
            )
            Text(
                text = "$remaining ${if (remaining == 1) "Rung" else "Rungs"} Today",
                fontFamily = Mono,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.OrangeLight,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// One rung: time on the left, the bar in the middle, the name on the right
// ---------------------------------------------------------------------------

@Composable
private fun RungRow(
    rung: Rung,
    w: Dp,
    appear: Float,
    pulse: Float,
    day: DayState,
    nowMinute: Int,
    allEvents: List<Event>
) {
    // Rungs further from NOW fade away, so attention stays on where you are
    val distanceFade = when (rung.kind) {
        RungKind.NOW, RungKind.NEXT -> 1f
        RungKind.FUTURE -> (1f - 0.10f * rung.distance).coerceAtLeast(0.35f)
        RungKind.PAST -> (0.85f - 0.15f * rung.distance).coerceAtLeast(0.30f)
        RungKind.HOURLY -> (0.65f - 0.05f * rung.distance).coerceAtLeast(0.22f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rung.height)
            .graphicsLayer { alpha = appear * distanceFade }
    ) {
        // Left: time
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(w * 0.31f - 4.dp)
        ) {
            TimeLabel(rung, day, nowMinute, Modifier.align(Alignment.CenterEnd))
        }

        // Middle: the rung itself
        Box(modifier = Modifier.align(Alignment.Center)) {
            when (rung.kind) {
                RungKind.NOW -> NowBar(w = w * 0.32f * appear, pulse = pulse)
                RungKind.NEXT -> NextBar(
                    w = w * 0.34f * appear,
                    progress = progressToNext(rung.event, allEvents, day.now)
                )
                RungKind.HOURLY -> HourlyBar(w = w * 0.22f * appear)
                else -> SimpleBar(
                    w = w * 0.29f * appear,
                    muted = rung.kind == RungKind.PAST
                )
            }
        }

        // Right: name and mood
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = w * 0.69f)
                .width(w * 0.31f - 12.dp)
        ) {
            RungText(rung)
        }
    }
}

@Composable
private fun TimeLabel(rung: Rung, day: DayState, nowMinute: Int, modifier: Modifier = Modifier) {
    when (rung.kind) {
        RungKind.NOW -> Column(modifier = modifier, horizontalAlignment = Alignment.End) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Pal.Orange.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(8.dp).background(Pal.Orange, CircleShape))
                Text(
                    text = "ANCHOR",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.OrangeLight
                )
            }
            Text(
                text = formatClock(nowMinute, day.notion),
                fontFamily = Mono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.Orange,
                modifier = Modifier.padding(top = 6.dp, end = 4.dp)
            )
        }
        RungKind.NEXT -> Column(modifier = modifier, horizontalAlignment = Alignment.End) {
            val e = rung.event!!
            Text(
                text = formatClock(e.start.minuteOfDay(), day.notion),
                fontFamily = Mono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.Blue
            )
            Text(
                text = "in ${formatMins(minutesUntil(day.now, e))}",
                fontFamily = Mono,
                fontSize = 12.sp,
                color = Pal.Blue.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        RungKind.HOURLY -> Text(
            text = formatClock(rung.timeMinutes, day.notion),
            fontFamily = Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Pal.Muted,
            textAlign = TextAlign.End,
            modifier = modifier
        )
        else -> Text(
            text = formatClock(rung.timeMinutes, day.notion),
            fontFamily = Mono,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Pal.Sub,
            textAlign = TextAlign.End,
            modifier = modifier
        )
    }
}

@Composable
private fun RungText(rung: Rung) {
    when (rung.kind) {
        RungKind.NOW -> Column {
            Text(
                text = "NOW",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Pal.OrangeLight
            )
            Text(
                text = rung.subtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Pal.OrangeSoft,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        RungKind.NEXT -> Column {
            Text(
                text = rung.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Pal.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rung.subtitle,
                fontSize = 14.sp,
                color = Pal.Blue,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        RungKind.HOURLY -> Text(
            text = formatClock(rung.timeMinutes, rung.timeMinutes.let { TimeNotation.H24 /* default */ }),
            fontFamily = Mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Pal.Muted.copy(alpha = 0.7f),
            maxLines = 1
        )
        else -> Column {
            Text(
                text = rung.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Pal.Sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = rung.subtitle,
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Pal.Muted,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Rung bars
// ---------------------------------------------------------------------------

@Composable
private fun SimpleBar(w: Dp, muted: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(w)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Pal.Rung.copy(alpha = if (muted) 0.6f else 1f))
        )
        Box(Modifier.size(10.dp).background(Pal.Knob, CircleShape))
    }
}

@Composable
private fun HourlyBar(w: Dp) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(w)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Pal.Rung.copy(alpha = 0.5f))
        )
        Box(Modifier.size(4.dp).background(Pal.Knob.copy(alpha = 0.6f), CircleShape))
    }
}

@Composable
private fun NextBar(w: Dp, progress: Float) {
    // The blue fill grows as the next event gets closer
    val fill by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "nextFill"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(w)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Pal.Blue.copy(alpha = 0.28f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(listOf(Pal.Blue.copy(alpha = 0.75f), Color(0xFF8CCBF0)))
                    )
            )
        }
        Box(
            Modifier
                .size(20.dp)
                .shadow(12.dp, CircleShape, ambientColor = Pal.Blue, spotColor = Pal.Blue)
                .background(Pal.BlueLight, CircleShape)
        )
    }
}

@Composable
private fun NowBar(w: Dp, pulse: Float) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(w)
                .height(20.dp)
                .shadow(16.dp, RoundedCornerShape(10.dp), ambientColor = Pal.Orange, spotColor = Pal.Orange)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.horizontalGradient(listOf(Color(0xFFFF8A44), Pal.Orange)))
        ) {
            // Lighter inner line, like the design
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.82f)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(1.dp))
            )
        }
        // Pulse ring around the knob
        Box(
            Modifier
                .size(28.dp)
                .graphicsLayer {
                    val s = 1f + 0.9f * pulse
                    scaleX = s
                    scaleY = s
                    alpha = 0.55f * (1f - pulse)
                }
                .border(2.dp, Pal.Orange, CircleShape)
        )
        Box(
            Modifier
                .size(28.dp)
                .background(Pal.Orange, CircleShape)
                .border(3.dp, Color(0xFFFF9A5E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(10.dp).background(Color(0xFF3A1A08), CircleShape))
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** How far through the quiet gap before [next] we are, 0..1. */
private fun progressToNext(next: Event?, events: List<Event>, now: LocalDateTime): Float {
    if (next == null) return 0f
    val gapStart = events
        .filter { it.end <= next.start }
        .maxOfOrNull { it.end.minuteOfDay() } ?: 0
    val span = (next.start.minuteOfDay() - gapStart).coerceAtLeast(1)
    return ((now.minuteOfDay() - gapStart).toFloat() / span).coerceIn(0f, 1f)
}

private fun minutesUntil(now: LocalDateTime, event: Event): Int =
    Duration.between(now, event.start).toMinutes().toInt().coerceAtLeast(0)

/** "18 mins", "1 min", "2h 18m". */
private fun formatMins(minutes: Int): String = when {
    minutes < 60 -> if (minutes == 1) "1 min" else "$minutes mins"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

/** Fade + slide-up played once when the item first appears. */
private fun Modifier.enterAnimation(delayMs: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
    // Return the graphicsLayer modifier chained onto the base modifier
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

private fun formatClock(minutes: Int, notion: TimeNotation): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return if (notion == TimeNotation.H24) {
        "%02d:%02d".format(h, m)
    } else {
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (h >= 12) "pm" else "am"
        if (m == 0) "$h12 $suffix" else "$h12:${"%02d".format(m)} $suffix"
    }
}

// ---------------------------------------------------------------------------
// Preview (matches the reference screenshot's moment: 16:42)
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 800, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun LadderPreview() {
    val today = LocalDate.now()
    val day = DayState(
        now = today.atTime(LocalTime.of(16, 42)),
        events = listOf(
            Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
            Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
            Event(3, "Gym", today.atTime(17, 0), today.atTime(18, 0)),
            Event(4, "Study Session", today.atTime(19, 0), today.atTime(20, 30)),
            Event(5, "Dinner", today.atTime(21, 0), today.atTime(22, 0)),
            Event(6, "Sleep & Rest", today.atTime(23, 0), today.atTime(23, 59)),
        ),
        notion = TimeNotation.H24
    )
    LadderVisualization(day = day)
}
