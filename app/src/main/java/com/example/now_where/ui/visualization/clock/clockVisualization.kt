package com.example.now_where.ui.visualization.clock


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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Pal {
    val Bg = Color(0xFF070E15)
    val Disc = Color(0xFF0E1822)
    val DiscEdge = Color(0xFF0A121A)
    val RingStroke = Color(0xFF1E2A36)
    val Surface = Color(0xFF111B26)
    val Border = Color(0xFF1D2935)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF5E6B78)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val Blue = Color(0xFF5BB8F0)
    val Track = Color(0xFF232C36)
}

private val Mono = FontFamily.Monospace

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

/** First upcoming event is orange, the second blue, the rest muted. */
private fun rankColor(rank: Int): Color = when (rank) {
    0 -> Pal.Orange
    1 -> Pal.Blue
    else -> Pal.Sub
}

/** Angle of an event on a 12-hour dial, degrees clockwise from 12 o'clock. */
private fun dialAngle(minuteOfDay: Int): Float = (minuteOfDay % 720) / 720f * 360f

private fun dpPoint(center: Dp, radius: Dp, angleDeg: Float): Pair<Dp, Dp> {
    val th = Math.toRadians(angleDeg.toDouble())
    return (center + radius * sin(th).toFloat()) to (center - radius * cos(th).toFloat())
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun ClockVisualization(
    day: DayState,
    modifier: Modifier = Modifier
) {
    val nowTotal = day.now.hour * 60 + day.now.minute + day.now.second / 60f
    val upcoming = remember(day) { day.events.filter { it.start > day.now }.sortedBy { it.start } }
    val next = upcoming.firstOrNull()

    // Hands glide when the time changes. After the first sweep, only the real angle matters.
    val nowAnim by animateFloatAsState(
        targetValue = nowTotal,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "now"
    )
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }

    val eventsIntro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(600)
        eventsIntro.animateTo(1f, tween(900))
    }

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
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        ClockFace(
            day = day,
            upcoming = upcoming,
            next = next,
            nowTotal = nowAnim,
            intro = intro.value,
            eventsProgress = eventsIntro.value,
            pulse = pulse,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(4.dp))
            TimeLine(day = day, next = next, modifier = Modifier.enterAnimation(300))

            Spacer(Modifier.height(24.dp))
            NextLandmarkCard(
                next = next,
                day = day,
                introProgress = intro.value,
                modifier = Modifier.enterAnimation(450)
            )

            Spacer(Modifier.height(26.dp))
            SequenceHeader(count = upcoming.size, modifier = Modifier.enterAnimation(600))
            Spacer(Modifier.height(12.dp))

            if (upcoming.isEmpty()) {
                Text(
                    text = "Nothing left in the sequence today.",
                    fontSize = 14.sp,
                    color = Pal.Sub,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            upcoming.forEachIndexed { rank, e ->
                SequenceRow(
                    event = e,
                    rank = rank,
                    day = day,
                    modifier = Modifier.enterAnimation(700 + rank * 120)
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(96.dp)) // room for the common AddButton
        }
    }
}

// ---------------------------------------------------------------------------
// Clock face
// ---------------------------------------------------------------------------

@Composable
private fun ClockFace(
    day: DayState,
    upcoming: List<Event>,
    next: Event?,
    nowTotal: Float,
    intro: Float,
    eventsProgress: Float,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    // Angles come from the unwrapped minute count so hands never spin backwards.
    // Multiplying the wrapped angle by `intro` makes both hands sweep out from 12 once.
    val minuteAngle = ((nowTotal * 6f) % 360f) * intro
    val hourAngle = ((nowTotal * 0.5f) % 360f) * intro
    val n = upcoming.size

    fun appear(i: Int): Float = (eventsProgress * (n + 1) - i).coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val c = w / 2
        val rOut = w * 0.43f
        val nowMinute = day.now.minuteOfDay()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)
            val outer = size.width * 0.43f
            val rDots = outer * 0.76f

            fun pt(angle: Float, radius: Float): Offset {
                val th = Math.toRadians(angle.toDouble())
                return Offset(cx + radius * sin(th).toFloat(), cy - radius * cos(th).toFloat())
            }

            // Soft glow behind the face
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Pal.Orange.copy(alpha = 0.05f), Color.Transparent),
                    center = center, radius = outer * 1.25f
                ),
                radius = outer * 1.25f, center = center
            )

            // Disc and rings
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Pal.Disc, Pal.DiscEdge), center = center, radius = outer
                ),
                radius = outer, center = center
            )
            drawCircle(Pal.RingStroke, outer, center, style = Stroke(2.dp.toPx()))
            drawCircle(Pal.RingStroke.copy(alpha = 0.5f), outer - 14.dp.toPx(), center, style = Stroke(1.dp.toPx()))

            // Dotted ring
            for (i in 0 until 120) {
                drawCircle(Pal.Muted.copy(alpha = 0.35f), 0.8.dp.toPx(), pt(i * 3f, rDots))
            }

            // Hour markers: bars at 12/3/6/9, dots elsewhere
            for (h in 0 until 12) {
                val a = h * 30f
                when (h) {
                    0 -> drawLine(
                        Color.White.copy(alpha = 0.85f),
                        pt(a, rDots - 9.dp.toPx()), pt(a, rDots + 9.dp.toPx()),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
                    )
                    3, 6, 9 -> drawLine(
                        Pal.Sub.copy(alpha = 0.7f),
                        pt(a, rDots - 8.dp.toPx()), pt(a, rDots + 8.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round
                    )
                    else -> drawCircle(Pal.Orange.copy(alpha = 0.55f), 2.dp.toPx(), pt(a, rDots))
                }
            }

            // Events on the dotted ring
            day.events.forEach { e ->
                val a = dialAngle(e.start.minuteOfDay())
                val rank = upcoming.indexOfFirst { it.id == e.id }
                val p = pt(a, rDots)
                if (rank < 0) {
                    // Already started: a quiet pale dot
                    drawCircle(Pal.Ink.copy(alpha = 0.65f), 2.5.dp.toPx(), p)
                } else {
                    val s = appear(rank)
                    val color = rankColor(rank)
                    drawCircle(color.copy(alpha = 0.22f * s), 9.dp.toPx() * s, p)
                    drawCircle(color.copy(alpha = s), 4.dp.toPx() * s, p)
                    if (rank == 0) {
                        drawCircle(
                            color.copy(alpha = 0.5f * (1f - pulse)),
                            radius = 5.dp.toPx() + 12.dp.toPx() * pulse,
                            center = p, style = Stroke(1.5.dp.toPx())
                        )
                    }
                }
            }

            // Arc along the ring from the hour hand to the next event
            if (next != null && intro >= 1f) {
                val target = dialAngle(next.start.minuteOfDay())
                val sweep = ((target - hourAngle) + 360f) % 360f
                val tl = Offset(cx - rDots, cy - rDots)
                val sz = Size(2f * rDots, 2f * rDots)
                drawArc(
                    Pal.Orange.copy(alpha = 0.18f), hourAngle - 90f, sweep, false, tl, sz,
                    style = Stroke(8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    Pal.Orange, hourAngle - 90f, sweep, false, tl, sz,
                    style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Minute hand (long, thin) with a small diamond tip
            val minTip = pt(minuteAngle, outer * 0.72f)
            drawLine(
                Pal.Orange.copy(alpha = 0.18f), pt(minuteAngle + 180f, outer * 0.08f), minTip,
                strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round
            )
            drawLine(
                Pal.Orange, pt(minuteAngle + 180f, outer * 0.08f), minTip,
                strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
            )
            val d = 4.dp.toPx()
            val diamond = Path().apply {
                moveTo(minTip.x, minTip.y - d)
                lineTo(minTip.x + d, minTip.y)
                lineTo(minTip.x, minTip.y + d)
                lineTo(minTip.x - d, minTip.y)
                close()
            }
            // Rotate the diamond with the hand
            rotate(minuteAngle, pivot = minTip) {
                drawPath(diamond, Pal.Orange, style = Fill)
            }

            // Hour hand (short, thick)
            val hourTip = pt(hourAngle, outer * 0.30f)
            drawLine(
                Pal.Orange.copy(alpha = 0.18f), center, hourTip,
                strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round
            )
            drawLine(
                Pal.Orange, center, hourTip,
                strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
            )

            // Hub
            drawCircle(Pal.Disc, 10.dp.toPx(), center)
            drawCircle(Pal.Orange.copy(alpha = 0.5f), 10.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
            drawCircle(Pal.Orange, 3.5.dp.toPx(), center)
        }

        // Floating event pills at the edge of the dial, at each event's hour position
        val pillW = 112.dp
        upcoming.take(3).forEachIndexed { rank, e ->
            val (px, py) = dpPoint(c, rOut, dialAngle(e.start.minuteOfDay()))
            val color = rankColor(rank)
            val s = appear(rank)
            val x = (px - pillW / 2).coerceIn(4.dp, w - pillW - 4.dp)

            Row(
                modifier = Modifier
                    .offset(x = x, y = py - 21.dp)
                    .width(pillW)
                    .graphicsLayer {
                        alpha = s
                        scaleX = 0.85f + 0.15f * s
                        scaleY = 0.85f + 0.15f * s
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pal.Surface.copy(alpha = 0.95f))
                    .border(
                        1.dp,
                        if (rank == 0) color.copy(alpha = 0.4f) else Pal.Border,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.size(7.dp).background(color, CircleShape))
                Column {
                    Text(
                        text = e.title.uppercase(),
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = if (rank == 0) Pal.OrangeSoft else Pal.Sub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatClock(e.start.minuteOfDay(), day.notion),
                        fontFamily = Mono,
                        fontSize = 11.sp,
                        color = color
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Time line under the clock
// ---------------------------------------------------------------------------

@Composable
private fun TimeLine(day: DayState, next: Event?, modifier: Modifier = Modifier) {
    val weekday = day.now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = formatClock(day.now.minuteOfDay(), day.notion),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.Ink
            )
            Box(Modifier.size(4.dp).background(Pal.Muted, CircleShape))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Pal.Orange.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "NOW",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.OrangeSoft
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = weekday, fontSize = 15.sp, color = Pal.Sub)
            Box(Modifier.size(3.dp).background(Pal.Muted, CircleShape))
            Text(
                text = if (next != null) {
                    "${next.title} in ${formatDuration(minutesUntil(day.now, next))}"
                } else {
                    "Day complete"
                },
                fontFamily = Mono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.OrangeSoft
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Next landmark card
// ---------------------------------------------------------------------------

@Composable
private fun NextLandmarkCard(
    next: Event?,
    day: DayState,
    introProgress: Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)

    // Progress through the quiet gap that leads up to the next event
    val progress = if (next == null) 0f else {
        val startOfGap = day.events
            .filter { it.end <= next.start }
            .maxOfOrNull { it.end.minuteOfDay() } ?: 0
        val span = (next.start.minuteOfDay() - startOfGap).coerceAtLeast(1)
        ((day.now.minuteOfDay() - startOfGap).toFloat() / span).coerceIn(0f, 1f)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress * introProgress,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "landmarkProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Border, shape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 18.dp, top = 16.dp, bottom = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Pal.Orange.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(next?.title.orEmpty()),
                    contentDescription = null,
                    tint = Pal.OrangeSoft,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "NEXT LANDMARK",
                        fontFamily = Mono,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pal.OrangeSoft,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Spatial Anchor",
                        fontFamily = Mono,
                        fontSize = 10.5.sp,
                        color = Pal.Muted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Text(
                    text = next?.title ?: "Nothing else today",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Pal.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (next != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "in ${formatDuration(minutesUntil(day.now, next))}",
                        fontFamily = Mono,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pal.OrangeSoft
                    )
                    Text(
                        text = formatClock(next.start.minuteOfDay(), day.notion),
                        fontSize = 14.sp,
                        color = Pal.Sub,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Thin bar along the bottom edge
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(animatedProgress)
                .height(3.dp)
                .background(Pal.Orange)
        )
    }
}

private fun iconFor(title: String): ImageVector {
    val t = title.lowercase()
    return when {
        listOf("gym", "workout", "run", "exercise", "sport").any { it in t } -> Icons.Default.FitnessCenter
        listOf("study", "read", "learn", "class", "college", "code").any { it in t } -> Icons.Default.MenuBook
        listOf("lunch", "dinner", "breakfast", "meal", "eat").any { it in t } -> Icons.Default.Restaurant
        else -> Icons.Default.Event
    }
}

// ---------------------------------------------------------------------------
// Temporal sequence
// ---------------------------------------------------------------------------

@Composable
private fun SequenceHeader(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TEMPORAL SEQUENCE",
            fontFamily = Mono,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = Pal.Muted
        )
        Text(
            text = "$count ${if (count == 1) "Event" else "Events"} Remaining",
            fontFamily = Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Pal.Blue
        )
    }
}

@Composable
private fun SequenceRow(event: Event, rank: Int, day: DayState, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val color = if (rank >= 2) Pal.Track else rankColor(rank)
    val textAlpha = when (rank) {
        0 -> 1f
        1 -> 0.92f
        else -> 0.6f
    }
    val length = Duration.between(event.start, event.end).toMinutes().toInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (rank == 0) Pal.Surface else Pal.Surface.copy(alpha = 0.45f))
            .border(1.dp, if (rank == 0) Pal.Border else Pal.Border.copy(alpha = 0.6f), shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 6.dp, height = 36.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = event.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Pal.Ink.copy(alpha = textAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDuration(length),
                fontSize = 13.sp,
                color = Pal.Sub.copy(alpha = textAlpha),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatClock(event.start.minuteOfDay(), day.notion),
                fontFamily = Mono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = (if (rank >= 2) Pal.Sub else rankColor(rank)).copy(alpha = if (rank >= 2) 0.8f else 1f),
                textAlign = TextAlign.End
            )
            Text(
                text = "in ${formatAway(minutesUntil(day.now, event))}",
                fontSize = 13.sp,
                color = Pal.Sub.copy(alpha = textAlpha),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun minutesUntil(now: LocalDateTime, event: Event): Int =
    Duration.between(now, event.start).toMinutes().toInt().coerceAtLeast(0)

/** Fade + slide-up played once when the item first appears. */
private fun Modifier.enterAnimation(delayMs: Int): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }
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

private fun formatDuration(minutes: Int): String {
    val m = minutes.coerceAtLeast(0)
    val h = m / 60
    val r = m % 60
    return when {
        h == 0 -> "${r}m"
        r == 0 -> "${h}h"
        else -> "${h}h ${r}m"
    }
}

/** "18 min" under an hour, "2h 18m" above. */
private fun formatAway(minutes: Int): String =
    if (minutes < 60) "$minutes min" else formatDuration(minutes)

// ---------------------------------------------------------------------------
// Preview (matches the reference screenshot's moment: 16:42)
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 1200, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun ClockPreview() {
    val today = LocalDate.now()
    val day = DayState(
        now = today.atTime(LocalTime.of(16, 42)),
        events = listOf(
            Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
            Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
            Event(3, "Gym", today.atTime(17, 0), today.atTime(17, 55)),
            Event(4, "Study", today.atTime(19, 0), today.atTime(20, 30)),
            Event(5, "Dinner", today.atTime(21, 0), today.atTime(22, 0)),
        ),
        notion = TimeNotation.H24
    )
    ClockVisualization(day = day)
}