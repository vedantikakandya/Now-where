package com.example.now_where.ui.visualization.circular_day

import androidx.compose.animation.Crossfade
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
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbTwilight
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Pal {
    val Bg = Color(0xFF070E15)
    val Surface = Color(0xFF131C27)
    val Border = Color(0xFF1D2935)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF5E6B78)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val Blue = Color(0xFF5BB8F0)
    val Track = Color(0xFF2A333D)
}

private val Mono = FontFamily.Monospace
private const val DAY_MINUTES = 1440f

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

/**
 * Clock angle, in degrees clockwise from the top of the ring.
 * Matches the labels in the design: 12:00 top, 18:00 right, 00:00 bottom, 06:00 left.
 */
private fun cwDeg(minutes: Float): Float = ((minutes / DAY_MINUTES) * 360f + 180f) % 360f

private fun dpPoint(center: Dp, radius: Dp, minutes: Float): Pair<Dp, Dp> {
    val th = Math.toRadians(cwDeg(minutes).toDouble())
    return (center + radius * sin(th).toFloat()) to (center - radius * cos(th).toFloat())
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun CircularDayVisualization(
    day: DayState,
    modifier: Modifier = Modifier,
    // Placeholders: there is no sunset source in the domain yet. Feed real values later.
    twilightAt: LocalTime = LocalTime.of(18, 14),
    sensoryMode: String = "Calm",
    sensoryHint: String = "No sudden alarms"
) {
    val nowMinutes = day.now.minuteOfDay() + day.now.second / 60f
    val next = day.events.filter { it.start > day.now }.minByOrNull { it.start }
    val sorted = remember(day.events) { day.events.sortedBy { it.start } }

    // The sun-orb travels from midnight to now on first show, then glides as time passes.
    val nowAnim by animateFloatAsState(
        targetValue = nowMinutes,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "now"
    )
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1700, easing = FastOutSlowInEasing)) }

    val eventsIntro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(700)
        eventsIntro.animateTo(1f, tween(1000))
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        TitlePill(Modifier.enterAnimation(0))

        Spacer(Modifier.height(12.dp))
        DayRing(
            day = day,
            sortedEvents = sorted,
            next = next,
            shownMinutes = nowAnim * intro.value,
            eventsProgress = eventsIntro.value,
            pulse = pulse,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        Spacer(Modifier.height(16.dp))
        NextEventCard(
            next = next,
            now = day.now,
            notion = day.notion,
            dayFraction = (nowMinutes / DAY_MINUTES) * intro.value,
            modifier = Modifier.enterAnimation(500)
        )

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth().enterAnimation(650),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val untilTwilight = Duration.between(day.now.toLocalTime(), twilightAt).toMinutes().toInt()
            InfoCard(
                icon = { Icon(Icons.Default.WbTwilight, null, tint = Pal.Blue, modifier = Modifier.size(22.dp)) },
                label = "TWILIGHT AT",
                value = formatClock(twilightAt.hour * 60 + twilightAt.minute, day.notion),
                hint = if (untilTwilight > 0) "${formatDuration(untilTwilight)} of daylight" else "Twilight has passed",
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                icon = { Icon(Icons.Default.Spa, null, tint = Pal.OrangeSoft, modifier = Modifier.size(22.dp)) },
                label = "SENSORY MODE",
                value = sensoryMode,
                hint = sensoryHint,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(96.dp)) // room for the common AddButton
    }
}

@Composable
private fun TitlePill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Pal.Surface)
            .border(1.dp, Pal.Border, RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(12.dp).background(Pal.Orange.copy(alpha = 0.35f), CircleShape))
        Text(
            text = "SOLAR CYCLE IN MOTION",
            fontFamily = Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = Pal.OrangeSoft
        )
    }
}

// ---------------------------------------------------------------------------
// The ring
// ---------------------------------------------------------------------------

@Composable
private fun DayRing(
    day: DayState,
    sortedEvents: List<Event>,
    next: Event?,
    shownMinutes: Float,
    eventsProgress: Float,
    pulse: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val c = w / 2
        val ringR = w * 0.32f
        val nowMinute = day.now.minuteOfDay()
        val n = sortedEvents.size

        // Per-event pop-in, staggered along one shared 0..1 value
        fun appear(i: Int): Float = (eventsProgress * (n + 1) - i).coerceIn(0f, 1f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = ringR.toPx()
            val topLeft = Offset(cx - r, cy - r)
            val arcSize = Size(2f * r, 2f * r)
            val center = Offset(cx, cy)

            fun point(minutes: Float, radius: Float): Offset {
                val th = Math.toRadians(cwDeg(minutes).toDouble())
                return Offset(cx + radius * sin(th).toFloat(), cy - radius * cos(th).toFloat())
            }

            // Faint outer halo
            drawCircle(Pal.Orange.copy(alpha = 0.05f), radius = r + 34.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))

            // Base ring: warm daylight half, cool night half
            drawArc(
                Pal.Orange.copy(alpha = 0.30f), startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(2.5.dp.toPx())
            )
            drawArc(
                Pal.Blue.copy(alpha = 0.36f), startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(2.5.dp.toPx())
            )

            // Quarter-hour ticks, hours a little longer
            for (i in 0 until 96) {
                val minute = i * 15f
                val isHour = i % 4 == 0
                val len = if (isHour) 7.dp.toPx() else 3.5.dp.toPx()
                val daylight = minute in 360f..1080f
                drawLine(
                    color = (if (daylight) Pal.Orange else Pal.Blue).copy(alpha = if (isHour) 0.35f else 0.2f),
                    start = point(minute, r - 5.dp.toPx()),
                    end = point(minute, r - 5.dp.toPx() - len),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Elapsed part of the day: starts faint at midnight, burns brighter toward now
            val total = max(shownMinutes, 1f)
            val segMinutes = DAY_MINUTES / 120f
            var k = 0
            while (k * segMinutes < shownMinutes) {
                val s = k * segMinutes
                val e = minOf((k + 1) * segMinutes, shownMinutes)
                val t = s / total
                val sweep = (e - s) / DAY_MINUTES * 360f + 0.6f
                val start = cwDeg(s) - 90f
                drawArc(
                    Pal.Orange.copy(alpha = 0.10f * t), start, sweep, false, topLeft, arcSize,
                    style = Stroke((10.dp.toPx() * (0.4f + t)))
                )
                drawArc(
                    Pal.Orange.copy(alpha = 0.10f + 0.90f * t * t), start, sweep, false, topLeft, arcSize,
                    style = Stroke((2.5f + 3.5f * t).dp.toPx())
                )
                k++
            }

            // Event spans and start dots
            sortedEvents.forEachIndexed { i, e ->
                val a = appear(i)
                val s = e.start.minuteOfDay().toFloat()
                val en = e.end.minuteOfDay().toFloat()
                val isPast = en <= nowMinute
                val isNext = e.id == next?.id
                val color = when {
                    isPast -> Pal.Muted
                    isNext -> Pal.Orange
                    else -> Pal.Blue
                }
                drawArc(
                    color.copy(alpha = (if (isPast) 0.35f else 0.6f) * a),
                    startAngle = cwDeg(s) - 90f,
                    sweepAngle = (en - s) / DAY_MINUTES * 360f * a,
                    useCenter = false, topLeft = topLeft, size = arcSize,
                    style = Stroke(6.dp.toPx(), cap = StrokeCap.Round)
                )
                val p = point(s, r)
                drawCircle(color.copy(alpha = 0.25f * a), radius = 10.dp.toPx() * a, center = p)
                drawCircle(Pal.Bg, radius = 6.dp.toPx() * a, center = p)
                drawCircle(color.copy(alpha = a), radius = 4.5.dp.toPx() * a, center = p)
            }

            // The sun-orb at "now"
            val nowPoint = point(shownMinutes, r)
            val glowR = 34.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Pal.Orange.copy(alpha = 0.55f), Color.Transparent),
                    center = nowPoint, radius = glowR
                ),
                radius = glowR, center = nowPoint
            )
            drawCircle(
                Pal.Orange.copy(alpha = 0.5f * (1f - pulse)),
                radius = 10.dp.toPx() + 16.dp.toPx() * pulse,
                center = nowPoint, style = Stroke(1.5.dp.toPx())
            )
            drawCircle(Pal.Orange, radius = 9.dp.toPx(), center = nowPoint)
            drawCircle(Color.White.copy(alpha = 0.9f), radius = 3.dp.toPx(), center = nowPoint)
        }

        // Cardinal labels
        CardinalLabel("MIDDAY ${formatClock(720, day.notion)}", c - 60.dp, c - ringR - 32.dp, 120.dp, TextAlign.Center)
        CardinalLabel("MIDNIGHT ${formatClock(0, day.notion)}", c - 60.dp, c + ringR + 14.dp, 120.dp, TextAlign.Center)
        CardinalLabel("${formatClock(360, day.notion)}\nDAWN", c - ringR + 8.dp, c - 14.dp, 46.dp, TextAlign.Start)
        CardinalLabel("${formatClock(1080, day.notion)}\nDUSK", c + ringR - 54.dp, c - 14.dp, 46.dp, TextAlign.End)

        // Event labels sit just outside each dot
        sortedEvents.forEachIndexed { i, e ->
            val s = e.start.minuteOfDay().toFloat()
            val isPast = e.end.minuteOfDay() <= nowMinute
            val isNext = e.id == next?.id
            val (x, y) = dpPoint(c, ringR, s)
            val onRight = x >= c
            val labelW = 78.dp
            val text = if (isPast) e.title else "${e.title} ${formatClock(e.start.minuteOfDay(), day.notion)}"

            Text(
                text = text,
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isPast -> Pal.Muted
                    isNext -> Pal.OrangeSoft
                    else -> Pal.Blue
                },
                textAlign = if (onRight) TextAlign.Start else TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(x = if (onRight) x + 12.dp else x - 12.dp - labelW, y = y - 8.dp)
                    .width(labelW)
                    .graphicsLayer { alpha = appear(i) }
            )
        }

        // Centre: present anchor
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PRESENT ANCHOR",
                fontFamily = Mono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Pal.OrangeSoft
            )
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatClock(nowMinute, day.notion),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Ink
                )
                Text(
                    text = "NOW",
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Orange,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            Text(
                text = phaseName(nowMinute),
                fontSize = 12.sp,
                color = Pal.Sub,
                modifier = Modifier.padding(top = 2.dp)
            )
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 26.dp, height = 2.dp)
                    .background(Pal.Orange.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun CardinalLabel(text: String, x: Dp, y: Dp, width: Dp, align: TextAlign) {
    Text(
        text = text,
        fontFamily = Mono,
        fontSize = 9.5.sp,
        letterSpacing = 0.8.sp,
        lineHeight = 12.sp,
        color = Pal.Muted,
        textAlign = align,
        modifier = Modifier.offset(x = x, y = y).width(width)
    )
}

private fun phaseName(minute: Int): String = when {
    minute < 5 * 60 -> "Deep Night Stillness"
    minute < 7 * 60 -> "First Light"
    minute < 11 * 60 -> "Morning Ascent"
    minute < 14 * 60 -> "Midday Zenith"
    minute < 16 * 60 -> "Afternoon Drift"
    minute < 18 * 60 -> "Late Afternoon Zenith"
    minute < 20 * 60 -> "Golden Dusk"
    else -> "Evening Descent"
}

// ---------------------------------------------------------------------------
// Status card: next-event description + day progress
// ---------------------------------------------------------------------------

@Composable
private fun NextEventCard(
    next: Event?,
    now: LocalDateTime,
    notion: TimeNotation,
    dayFraction: Float,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Border, shape)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Crossfade(targetState = next, animationSpec = tween(350), label = "next") { e ->
            if (e != null) {
                val until = Duration.between(now, e.start).toMinutes().toInt().coerceAtLeast(0)
                val length = Duration.between(e.start, e.end).toMinutes().toInt()
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NEXT EVENT",
                            fontFamily = Mono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            color = Pal.OrangeSoft
                        )
                        Text(
                            text = "in ${formatDuration(until)}",
                            fontFamily = Mono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Pal.Ink
                        )
                    }
                    Text(
                        text = e.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Pal.Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "${formatClock(e.start.minuteOfDay(), notion)} – ${formatClock(e.end.minuteOfDay(), notion)}" +
                                " · ${formatDuration(length)} long · ${formatDuration(until)} away",
                        fontSize = 13.sp,
                        color = Pal.Sub,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            } else {
                Column {
                    Text(
                        text = "NEXT EVENT",
                        fontFamily = Mono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        color = Pal.OrangeSoft
                    )
                    Text(
                        text = "Nothing else today",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Pal.Ink,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "The rest of the day is open.",
                        fontSize = 13.sp,
                        color = Pal.Sub,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Day progress bar (fills on first show, then follows the clock)
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Pal.Track)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(dayFraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Pal.Orange.copy(alpha = 0.7f), Pal.Orange)),
                        RoundedCornerShape(50)
                    )
            )
        }
    }
}

@Composable
private fun InfoCard(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Border, shape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon()
            Text(
                text = label,
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Pal.Sub
            )
        }
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.Ink,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = hint,
            fontSize = 13.sp,
            color = Pal.Sub,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Preview (matches the reference screenshot's moment: 16:42)
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 940, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun CircularDayPreview() {
    val today = LocalDate.now()
    val day = DayState(
        now = today.atTime(LocalTime.of(16, 42)),
        events = listOf(
            Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
            Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
            Event(3, "Gym", today.atTime(17, 0), today.atTime(18, 0)),
            Event(4, "Study", today.atTime(19, 0), today.atTime(20, 30)),
            Event(5, "Dinner", today.atTime(21, 0), today.atTime(22, 0)),
        ),
        notion = TimeNotation.H24
    )
    CircularDayVisualization(day = day)
}