package com.example.now_where.ui.visualization.day_progress

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.TimeNotation
import com.example.now_where.ui.theme.NowWhereColors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt

// Placeholders: swap for NowWhereColors members if you have matching ones.
private val TrackColor = Color(0xFFEDEBE2)
private val DoneColor = Color(0xFF3F7568)
private val NowColor = Color(0xFFC9622F)
private val InkColor = Color(0xFF1E2521)
private val SubColor = Color(0xFF66716B)
private val MutedColor = Color(0xFFB8BDB8)
private val ActiveBg = Color(0x1F3F7568)
private val SurfaceColor = Color.White

private const val MINUTES_PER_DAY = 1440f

private fun LocalDateTime.dayFraction(): Float =
    ((hour * 60 + minute) + second / 60f) / MINUTES_PER_DAY

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

@Composable
fun DayProgressVisualization(
    day: DayState,
    modifier: Modifier = Modifier
) {
    val target = day.now.dayFraction()

    // Intro sweep: the bar fills from empty to the real position once.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        intro.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
    }

    // After the intro, the bar glides whenever the clock changes.
    val live by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val progress = live * intro.value

    // Soft pulse on the leading edge of the bar.
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "pulseValue"
    )

    val percent = (progress * 100f).roundToInt()
    val minutesLeft = ((1f - target) * MINUTES_PER_DAY).roundToInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // Big percentage counts up with the bar
        Text(
            text = "$percent%",
            fontSize = 64.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkColor
        )
        Text(
            text = "of today has passed",
            fontSize = 14.sp,
            color = SubColor,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(36.dp))

        ProgressBar(
            progress = progress,
            pulse = pulse,
            events = day.events,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Text("start of day", fontSize = 11.sp, color = MutedColor)
            Text("end of day", fontSize = 11.sp, color = MutedColor)
        }

        Text(
            text = formatLeft(minutesLeft),
            fontSize = 13.sp,
            color = SubColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
        )

        // Events listed in the order they happen, so the bar and the list agree
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(day.events.sortedBy { it.start }, key = { it.id }) { e ->
                EventRow(e, day.now, day.notion)
            }
        }
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    pulse: Float,
    events: List<Event>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barHeight = 16.dp.toPx()
        val barTop = (size.height - barHeight) / 2f
        val radius = CornerRadius(barHeight / 2f)
        val filledWidth = size.width * progress.coerceIn(0f, 1f)

        // Empty track
        drawRoundRect(
            color = TrackColor,
            topLeft = Offset(0f, barTop),
            size = Size(size.width, barHeight),
            cornerRadius = radius
        )

        // Filled part
        if (filledWidth > 0f) {
            drawRoundRect(
                color = DoneColor,
                topLeft = Offset(0f, barTop),
                size = Size(filledWidth.coerceAtLeast(barHeight), barHeight),
                cornerRadius = radius
            )
        }

        // Event spans sit on the bar as slightly darker segments
        events.forEach { e ->
            val startX = size.width * (e.start.minuteOfDay() / MINUTES_PER_DAY)
            val endX = size.width * (e.end.minuteOfDay() / MINUTES_PER_DAY)
            val done = endX <= filledWidth
            drawRoundRect(
                color = if (done) Color.White.copy(alpha = 0.28f) else InkColor.copy(alpha = 0.16f),
                topLeft = Offset(startX, barTop + 3.dp.toPx()),
                size = Size((endX - startX).coerceAtLeast(3.dp.toPx()), barHeight - 6.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }

        // NOW marker on the leading edge, with pulse ring
        val nowX = filledWidth
        val centerY = size.height / 2f
        drawCircle(
            NowColor.copy(alpha = 0.45f * (1f - pulse)),
            radius = 10.dp.toPx() + 14.dp.toPx() * pulse,
            center = Offset(nowX, centerY)
        )
        drawCircle(SurfaceColor, 12.dp.toPx(), Offset(nowX, centerY))
        drawCircle(NowColor, 8.dp.toPx(), Offset(nowX, centerY))
    }
}

@Composable
private fun EventRow(event: Event, now: LocalDateTime, notion: TimeNotation) {
    val active = event.start <= now && now < event.end
    val past = now >= event.end

    val bg by animateColorAsState(
        targetValue = if (active) ActiveBg else Color.Transparent,
        animationSpec = tween(400),
        label = "rowBg"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    when {
                        active -> NowColor
                        past -> MutedColor
                        else -> InkColor
                    },
                    CircleShape
                )
        )
        Text(
            text = event.title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (past) SubColor else InkColor,
            textDecoration = if (past) TextDecoration.LineThrough else null,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )
        Text(
            text = formatTime(event.start.minuteOfDay(), notion),
            fontSize = 12.sp,
            color = SubColor
        )
    }
}

private fun formatLeft(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        minutes <= 0 -> "The day is over"
        h == 0 -> "$m min left today"
        m == 0 -> "$h h left today"
        else -> "$h h $m min left today"
    }
}

private fun formatTime(minutes: Int, notion: TimeNotation): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (notion == TimeNotation.H24) {
        "%02d:%02d".format(h, m)
    } else {
        val h12 = if (h % 12 == 0) 12 else h % 12
        val suffix = if (h in 12..23) "pm" else "am"
        if (m == 0) "$h12$suffix" else "$h12:${"%02d".format(m)}$suffix"
    }
}

// ---------------- Previews ----------------

private fun previewDay(now: LocalTime): DayState {
    val today = LocalDate.now()
    return DayState(
        now = today.atTime(now),
        events = listOf(
            Event(1, "College", today.atTime(9, 0), today.atTime(13, 0)),
            Event(2, "Lunch", today.atTime(13, 0), today.atTime(14, 0)),
            Event(3, "Gym", today.atTime(17, 0), today.atTime(18, 30)),
            Event(4, "Study", today.atTime(19, 0), today.atTime(20, 30)),
            Event(5, "Dinner", today.atTime(21, 0), today.atTime(22, 0)),
        ),
        notion = TimeNotation.H24
    )
}

@Preview(name = "Afternoon", widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun DayProgressAfternoonPreview() {
    Box(Modifier.fillMaxSize().background(NowWhereColors.Background)) {
        DayProgressVisualization(previewDay(LocalTime.of(16, 42)))
    }
}

@Preview(name = "Early morning", widthDp = 390, heightDp = 800, showBackground = true)
@Composable
private fun DayProgressMorningPreview() {
    Box(Modifier.fillMaxSize().background(NowWhereColors.Background)) {
        DayProgressVisualization(previewDay(LocalTime.of(6, 5)))
    }
}