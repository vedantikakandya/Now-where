package com.example.now_where.ui.visualization.water

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Pal {
    val Bg = Color(0xFF050D15)
    val Surface = Color(0xFF0D1822)
    val Border = Color(0xFF1B2A38)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF5E6B78)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val OrangeLight = Color(0xFFFFDCC2)
    val Cyan = Color(0xFF4CC9F0)
    val CyanSoft = Color(0xFF8FD6F0)
    val Deep = Color(0xFF0A3350)
}

private val Mono = FontFamily.Monospace

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * The whole screen is a tank of water. At midnight it is full; by the next midnight it is empty.
 *
 * - On first show the water covers everything, then drains down to the current level.
 * - Past events end up on the dry bank above the surface, the present floats on the crest,
 *   and upcoming events are still submerged below, darker the deeper they are.
 * - Waves flow east, bubbles rise, and every card bobs gently on the current.
 * - When an event passes, the surface glides to its new level instead of jumping.
 */
@Composable
fun WaterFlowVisualization(
    day: DayState,
    modifier: Modifier = Modifier
) {
    val nowMin = day.now.hour * 60 + day.now.minute + day.now.second / 60f
    val remaining = (1f - nowMin / 1440f).coerceIn(0f, 1f)
    val remainingAnim by animateFloatAsState(
        targetValue = remaining,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "remaining"
    )

    val upcoming = remember(day) { day.events.filter { it.start > day.now }.sortedBy { it.start } }
    val past = remember(day) { day.events.filter { it.end <= day.now }.sortedBy { it.start } }
    val active = day.events.firstOrNull { it.start <= day.now && day.now < it.end }
    val next = upcoming.firstOrNull()

    // ---- Water level: starts at the very top (screen full) and drains to the waterline
    val surface = remember { Animatable(0f) }
    var surfaceTarget by remember { mutableStateOf(0f) }
    var firstDrain by remember { mutableStateOf(true) }
    val scroll = rememberScrollState()

    val infinite = rememberInfiniteTransition(label = "water")
    val waveT = infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "waveT"
    )
    val bubbleT = infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Restart),
        label = "bubbleT"
    )
    val bob = infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "bob"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Pal.Bg)) {
        val density = LocalDensity.current
        val viewportPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(surfaceTarget) {
            if (surfaceTarget > 0f) {
                if (firstDrain) {
                    firstDrain = false
                    delay(300)
                    surface.animateTo(surfaceTarget, tween(3000, easing = FastOutSlowInEasing))
                    // After the drain, bring the waterline into view
                    delay(300)
                    scroll.animateScrollTo(
                        (surfaceTarget - viewportPx * 0.38f).toInt().coerceAtLeast(0),
                        tween(900, easing = FastOutSlowInEasing)
                    )
                } else {
                    surface.animateTo(surfaceTarget, tween(1400, easing = FastOutSlowInEasing))
                }
            }
        }

        // 0 = screen full, 1 = settled at the real level
        val drain = if (surfaceTarget > 0f) (surface.value / surfaceTarget).coerceIn(0f, 1f) else 0f
        val shownRemaining = 1f + (remainingAnim - 1f) * drain

        Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(16.dp))

                LevelCard(
                    shownRemaining = shownRemaining,
                    modifier = Modifier.padding(horizontal = 20.dp).enterAnimation(0)
                )

                Spacer(Modifier.height(22.dp))
                SectionHeader(
                    icon = Icons.Default.Air,
                    title = "UPPER HORIZON AIR · SETTLED SHORE",
                    trailing = if (past.isEmpty()) "" else
                        "${formatClock(past.minOf { it.start.minuteOfDay() }, day.notion)} – " +
                                formatClock(past.maxOf { it.end.minuteOfDay() }, day.notion),
                    titleColor = Pal.Sub,
                    trailingColor = Pal.Muted,
                    modifier = Modifier.padding(horizontal = 20.dp).enterAnimation(150)
                )
                Spacer(Modifier.height(14.dp))

                if (past.isEmpty()) {
                    EmptyNote(
                        "The shore is still wet. Nothing has dried yet.",
                        Modifier.padding(horizontal = 20.dp)
                    )
                }
                past.forEachIndexed { i, e ->
                    PastCard(e, i, day.notion, Modifier.padding(horizontal = 20.dp).enterAnimation(250 + i * 100))
                    Spacer(Modifier.height(14.dp))
                }

                Spacer(Modifier.height(18.dp))
                LevelLineRow(
                    nowLabel = formatClock(day.now.minuteOfDay(), day.notion),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                // The waterline band. Its centre is where the surface settles.
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .onGloballyPositioned {
                            val y = it.positionInParent().y + it.size.height * 0.5f
                            if (abs(y - surfaceTarget) > 0.5f) surfaceTarget = y
                        }
                )

                NowCard(
                    day = day,
                    active = active,
                    next = next,
                    bob = bob,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )

                Spacer(Modifier.height(34.dp))
                SectionHeader(
                    icon = Icons.Default.Pool,
                    title = "LOWER WATER BASIN · UPCOMING FLOATING LANDMARKS",
                    trailing = "Flowing East",
                    titleColor = Pal.Cyan,
                    trailingColor = Pal.CyanSoft,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(16.dp))

                if (upcoming.isEmpty()) {
                    EmptyNote(
                        "No landmarks ahead. The basin is calm.",
                        Modifier.padding(horizontal = 20.dp)
                    )
                }
                upcoming.forEachIndexed { rank, e ->
                    UpcomingCard(
                        event = e,
                        rank = rank,
                        day = day,
                        bob = bob,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(4.dp))
                TideFooter(
                    remaining = remaining,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(120.dp)) // room for the common AddButton
            }

            // The water itself: drawn over the content so everything under the surface is submerged
            Canvas(modifier = Modifier.matchParentSize()) {
                drawWater(
                    surfaceY = surface.value,
                    drain = drain,
                    waveT = waveT.value,
                    bubbleT = bubbleT.value
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// The water
// ---------------------------------------------------------------------------

private fun DrawScope.drawWater(surfaceY: Float, drain: Float, waveT: Float, bubbleT: Float) {
    val w = size.width
    val h = size.height
    val twoPi = (2.0 * PI).toFloat()
    val step = 8.dp.toPx()

    // Waves are livelier while the level is still draining
    val ampScale = 1f + 1.2f * (1f - drain)

    fun waveY(x: Float, base: Float, amp: Float, wavelength: Float, cycles: Int, phase: Float): Float =
        base + amp * ampScale * sin((x / wavelength) * twoPi - twoPi * cycles * waveT + phase)

    fun bodyPath(base: Float, amp: Float, wavelength: Float, cycles: Int, phase: Float): Path =
        Path().apply {
            moveTo(0f, h)
            lineTo(0f, waveY(0f, base, amp, wavelength, cycles, phase))
            var x = 0f
            while (x < w) {
                lineTo(x, waveY(x, base, amp, wavelength, cycles, phase))
                x += step
            }
            lineTo(w, waveY(w, base, amp, wavelength, cycles, phase))
            lineTo(w, h)
            close()
        }

    fun crestPath(base: Float, amp: Float, wavelength: Float, cycles: Int, phase: Float): Path =
        Path().apply {
            moveTo(0f, waveY(0f, base, amp, wavelength, cycles, phase))
            var x = step
            while (x < w) {
                lineTo(x, waveY(x, base, amp, wavelength, cycles, phase))
                x += step
            }
            lineTo(w, waveY(w, base, amp, wavelength, cycles, phase))
        }

    // Back layers
    drawPath(
        bodyPath(surfaceY - 4.dp.toPx(), 8.dp.toPx(), 260.dp.toPx(), 1, 1.2f),
        Pal.Cyan.copy(alpha = 0.10f)
    )
    drawPath(
        bodyPath(surfaceY, 6.dp.toPx(), 180.dp.toPx(), 2, 0f),
        Pal.Cyan.copy(alpha = 0.14f)
    )

    // Main body: lighter at the surface, darker with depth
    val frontBase = surfaceY + 3.dp.toPx()
    drawPath(
        path = bodyPath(frontBase, 5.dp.toPx(), 120.dp.toPx(), 3, 2.1f),
        brush = Brush.verticalGradient(
            colors = listOf(Pal.Cyan.copy(alpha = 0.18f), Pal.Deep.copy(alpha = 0.40f)),
            startY = surfaceY,
            endY = h
        )
    )

    // Crest lines
    val crest = crestPath(frontBase, 5.dp.toPx(), 120.dp.toPx(), 3, 2.1f)
    drawPath(crest, Pal.Cyan.copy(alpha = 0.14f), style = Stroke(9.dp.toPx()))
    drawPath(crest, Pal.Cyan.copy(alpha = 0.95f), style = Stroke(2.dp.toPx()))
    drawPath(
        crestPath(surfaceY, 6.dp.toPx(), 180.dp.toPx(), 2, 0f),
        Pal.CyanSoft.copy(alpha = 0.45f), style = Stroke(1.5.dp.toPx())
    )

    // Slow light patches just under the surface, drifting east
    for (i in 0 until 6) {
        val x = (((i * 0.17f) + waveT) % 1f) * (w + 200.dp.toPx()) - 100.dp.toPx()
        val y = surfaceY + 36.dp.toPx() + i * 52.dp.toPx()
        if (y < h) {
            drawOval(
                Pal.CyanSoft.copy(alpha = 0.06f * (1f - i / 7f)),
                topLeft = Offset(x - 75.dp.toPx(), y),
                size = Size(150.dp.toPx(), 18.dp.toPx())
            )
        }
    }

    // Bubbles rising to the surface
    val depth = h - surfaceY
    if (depth > 0f) {
        for (i in 0 until 22) {
            val speed = 1 + (i % 3)
            val prog = (bubbleT * speed + i * 0.137f) % 1f
            val y = h - prog * depth
            if (y > surfaceY + 8.dp.toPx()) {
                val x = w * ((i * 0.6180339f) % 1f) + sin(prog * 14f + i) * 9.dp.toPx()
                val r = (1.5f + (i % 4) * 0.9f).dp.toPx()
                val a = sin(prog * PI.toFloat()) * 0.30f
                drawCircle(Pal.CyanSoft.copy(alpha = a), r, Offset(x, y), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

/** Gentle bob and tilt, as if floating on the current. Reads the animation inside the layer, so no recomposition. */
private fun Modifier.floating(bob: State<Float>, amp: Dp, phase: Float, tiltDeg: Float = 0.35f): Modifier =
    graphicsLayer {
        val a = 2f * PI.toFloat() * (bob.value + phase)
        translationY = sin(a) * amp.toPx()
        rotationZ = sin(a + 1.2f) * tiltDeg
    }

// ---------------------------------------------------------------------------
// Top card: how much water is left
// ---------------------------------------------------------------------------

@Composable
private fun LevelCard(shownRemaining: Float, modifier: Modifier = Modifier) {
    val pct = ceil(shownRemaining * 100f).toInt().coerceIn(0, 100)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Surface.copy(alpha = 0.85f))
            .border(1.dp, Pal.Border, shape)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Waves, null, tint = Pal.Cyan, modifier = Modifier.size(20.dp))
            Text(
                text = "WATER HORIZON RECEDED: $pct% REMAINING",
                fontFamily = Mono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Pal.CyanSoft,
                modifier = Modifier.weight(1f).padding(start = 10.dp, end = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).background(Pal.Orange, CircleShape))
                Text(
                    text = "Ebb Stage: -${100 - pct}%",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Pal.OrangeSoft
                )
            }
        }

        // The bar is the tank: full at midnight, shrinking all day
        Box(
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(shownRemaining.coerceIn(0f, 1f))
                    .background(
                        Brush.horizontalGradient(listOf(Pal.Cyan.copy(alpha = 0.8f), Pal.Orange)),
                        RoundedCornerShape(50)
                    )
            )
        }

        Text(
            text = "Water moves left to right toward sunset horizon · Level recedes into night basin",
            fontSize = 13.sp,
            lineHeight = 17.sp,
            color = Pal.Sub,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Section headers and the level line
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(
    icon: ImageVector,
    title: String,
    trailing: String,
    titleColor: Color,
    trailingColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = titleColor.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Text(
                text = title,
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.8.sp,
                lineHeight = 20.sp,
                color = titleColor,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        if (trailing.isNotEmpty()) {
            Text(
                text = trailing,
                fontFamily = Mono,
                fontSize = 11.sp,
                color = trailingColor,
                modifier = Modifier.padding(start = 12.dp).width(78.dp)
            )
        }
    }
}

@Composable
private fun LevelLineRow(nowLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Waves, null, tint = Pal.Cyan, modifier = Modifier.size(18.dp))
            Text(
                text = "Water Level Line · $nowLabel",
                fontFamily = Mono,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.Cyan
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "CURRENT FLOWING EAST",
                fontFamily = Mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                color = Pal.OrangeSoft
            )
            Icon(Icons.Default.ArrowForward, null, tint = Pal.OrangeSoft, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Pal.Muted,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// Dry bank: past events
// ---------------------------------------------------------------------------

private val pastPhrases = listOf("Left on high dry bank", "Settled into shore sand", "Dried on warm stone", "Carried off by the wind")

@Composable
private fun PastCard(event: Event, index: Int, notion: TimeNotation, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dashedBorder(Pal.Border, 18.dp)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(Pal.Surface.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconFor(event.title), null, tint = Pal.Muted, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                text = event.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Pal.Sub.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatClock(event.start.minuteOfDay(), notion)} · ${pastPhrases[index % pastPhrases.size]}",
                fontFamily = Mono,
                fontSize = 12.sp,
                color = Pal.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (index % 2 == 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Pal.Border, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("Evaporated", fontFamily = Mono, fontSize = 11.sp, color = Pal.Muted, softWrap = false, maxLines = 1)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Dry wake", fontFamily = Mono, fontSize = 11.sp, color = Pal.Cyan.copy(alpha = 0.6f), softWrap = false, maxLines = 1)
                Box(Modifier.size(6.dp).background(Pal.Cyan.copy(alpha = 0.6f), CircleShape))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// The present, floating on the crest
// ---------------------------------------------------------------------------

@Composable
private fun NowCard(
    day: DayState,
    active: Event?,
    next: Event?,
    bob: State<Float>,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val nowMinute = day.now.minuteOfDay()
    val pace = if (next != null) minutesUntil(day.now, next) else (1440 - nowMinute)
    val basin = when {
        nowMinute < 12 * 60 -> "midday"
        nowMinute < 18 * 60 -> "evening"
        else -> "night"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .floating(bob, amp = 4.dp, phase = 0f, tiltDeg = 0.4f)
            .clip(shape)
            .background(Pal.Surface.copy(alpha = 0.78f))
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(Pal.Orange, Color(0xFF7C8A98), Pal.Cyan.copy(alpha = 0.55f))),
                shape
            )
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(14.dp).background(Pal.Muted, CircleShape))
                    Text(
                        text = "NOW ON WATERLINE CREST",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Pal.OrangeSoft
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Pal.Orange.copy(alpha = 0.16f))
                        .border(1.dp, Pal.Orange.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Floating", fontFamily = Mono, fontSize = 11.sp, color = Pal.OrangeLight, softWrap = false)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("NOW", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Pal.Ink)
                        Text("·", fontSize = 30.sp, color = Pal.Muted)
                        Text(
                            text = formatClock(nowMinute, day.notion),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Pal.Ink
                        )
                    }
                    Text(
                        text = active?.title ?: "Drifting between landmarks",
                        fontSize = 18.sp,
                        color = Pal.CyanSoft,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Compass wobbles as the water moves under it
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(18.dp, CircleShape, ambientColor = Pal.Orange, spotColor = Pal.Orange)
                        .background(
                            Brush.radialGradient(listOf(Color(0xFFFF9A5E), Pal.Orange)),
                            CircleShape
                        )
                        .border(3.dp, Pal.OrangeSoft.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = Color(0xFF3A1A08),
                        modifier = Modifier
                            .size(30.dp)
                            .graphicsLayer {
                                rotationZ = sin(2f * PI.toFloat() * bob.value * 2f) * 14f
                            }
                    )
                }
            }

            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(1.dp).background(Pal.Border))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gentle horizontal drift", fontFamily = Mono, fontSize = 13.sp, color = Pal.Sub)
                Text(
                    text = "${formatShort(pace)} unhurried pace",
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Cyan
                )
            }
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.ArrowForward, null, tint = Pal.Cyan, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Text(
                    text = "Waves carry present moment eastward into $basin basin",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Pal.Ink.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Submerged: upcoming events
// ---------------------------------------------------------------------------

@Composable
private fun UpcomingCard(
    event: Event,
    rank: Int,
    day: DayState,
    bob: State<Float>,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val isNext = rank == 0
    val until = minutesUntil(day.now, event)

    // Deeper events are dimmer, as if further under water
    val depthAlpha = when (rank) {
        0 -> 1f
        1 -> 0.9f
        2 -> 0.72f
        else -> 0.58f
    }
    val depthLabel = when (rank) {
        0 -> "~${formatShort(until)} away"
        1 -> "Mid pool"
        2 -> "Night basin"
        else -> "Deep basin"
    }
    val subtitle = if (isNext) "Bobbing toward shore in current" else subtitleFor(event.title)

    // The bar along the bottom fills as the next landmark gets closer
    val progress = if (isNext) progressToNext(event, day.events, day.now) else 0f
    val fill by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1200, delayMillis = 3400, easing = FastOutSlowInEasing),
        label = "arrival"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .floating(bob, amp = if (isNext) 3.dp else 2.dp, phase = 0.33f * (rank + 1))
            .then(
                if (isNext) Modifier.shadow(14.dp, shape, ambientColor = Pal.Cyan, spotColor = Pal.Cyan) else Modifier
            )
            .clip(shape)
            .background(
                if (isNext) Pal.Deep.copy(alpha = 0.75f) else Pal.Surface.copy(alpha = 0.55f * depthAlpha)
            )
            .border(
                1.dp,
                if (isNext) Pal.Cyan.copy(alpha = 0.6f) else Pal.Border.copy(alpha = depthAlpha),
                shape
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = if (isNext) 22.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pal.Surface.copy(alpha = 0.7f))
                    .border(
                        1.dp,
                        if (isNext) Pal.Cyan.copy(alpha = 0.45f) else Pal.Border,
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconFor(event.title),
                    contentDescription = null,
                    tint = if (isNext) Pal.Cyan else Pal.OrangeSoft.copy(alpha = depthAlpha),
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = event.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Pal.Ink.copy(alpha = depthAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isNext) Box(Modifier.size(14.dp).background(Pal.Cyan.copy(alpha = 0.8f), CircleShape))
                }
                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isNext) {
                        Icon(Icons.Default.Pool, null, tint = Pal.CyanSoft, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    }
                    Text(
                        text = subtitle,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        color = (if (isNext) Pal.CyanSoft else Pal.Sub).copy(alpha = depthAlpha)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatClock(event.start.minuteOfDay(), day.notion),
                    fontFamily = Mono,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Ink.copy(alpha = depthAlpha)
                )
                Text(
                    text = depthLabel,
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                    color = if (isNext) Pal.Cyan else Pal.Muted.copy(alpha = depthAlpha),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }

        if (isNext) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(fill.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(Brush.horizontalGradient(listOf(Pal.Cyan, Pal.Orange)))
            )
        }
    }
}

private fun subtitleFor(title: String): String {
    val t = title.lowercase()
    return when {
        listOf("study", "focus", "code", "coding", "work", "project", "read").any { it in t } ->
            "Submerged beacon · Autonomous rhythm"
        listOf("lunch", "dinner", "breakfast", "meal", "eat").any { it in t } ->
            "Warm nourishment waiting in deep basin"
        listOf("gym", "workout", "run", "walk", "exercise", "sport").any { it in t } ->
            "Rippling ahead · Steady momentum"
        else -> "Drifting steadily in the current"
    }
}

private fun iconFor(title: String): ImageVector {
    val t = title.lowercase()
    return when {
        listOf("gym", "workout", "run", "exercise", "sport", "stretch").any { it in t } -> Icons.Default.FitnessCenter
        listOf("seminar", "college", "class", "lecture").any { it in t } -> Icons.Default.School
        listOf("study", "read", "focus", "code").any { it in t } -> Icons.Default.MenuBook
        listOf("lunch", "coffee", "break", "stroll").any { it in t } -> Icons.Default.LocalCafe
        listOf("dinner", "meal", "eat", "breakfast").any { it in t } -> Icons.Default.RamenDining
        else -> Icons.Default.Event
    }
}

// ---------------------------------------------------------------------------
// Footer
// ---------------------------------------------------------------------------

@Composable
private fun TideFooter(remaining: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val tide = when {
        remaining > 0.6f -> "HIGH\nTIDE"
        remaining > 0.15f -> "EBBING\nSOFT"
        else -> "LOW\nTIDE"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Deep.copy(alpha = 0.35f))
            .border(1.dp, Pal.Cyan.copy(alpha = 0.25f), shape)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .shadow(8.dp, CircleShape, ambientColor = Pal.Cyan, spotColor = Pal.Cyan)
                .background(Pal.Cyan, CircleShape)
        )
        Text(
            text = "The current will carry you to the next event without hurry.",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = Pal.Ink.copy(alpha = 0.92f),
            modifier = Modifier.weight(1f).padding(horizontal = 14.dp)
        )
        Text(
            text = tide,
            fontFamily = Mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Pal.Cyan
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun progressToNext(next: Event, events: List<Event>, now: LocalDateTime): Float {
    val gapStart = events.filter { it.end <= next.start }.maxOfOrNull { it.end.minuteOfDay() } ?: 0
    val span = (next.start.minuteOfDay() - gapStart).coerceAtLeast(1)
    return ((now.minuteOfDay() - gapStart).toFloat() / span).coerceIn(0f, 1f)
}

private fun minutesUntil(now: LocalDateTime, event: Event): Int =
    Duration.between(now, event.start).toMinutes().toInt().coerceAtLeast(0)

/** "18m", "2h 18m". */
private fun formatShort(minutes: Int): String {
    val m = minutes.coerceAtLeast(0)
    val h = m / 60
    val r = m % 60
    return when {
        h == 0 -> "${r}m"
        r == 0 -> "${h}h"
        else -> "${h}h ${r}m"
    }
}

private fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        )
    }

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

// ---------------------------------------------------------------------------
// Preview (matches the reference screenshot's moment: 16:42)
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 900, showBackground = true, backgroundColor = 0xFF050D15)
@Composable
private fun WaterFlowPreview() {
    val today = LocalDate.now()
    val day = DayState(
        now = today.atTime(LocalTime.of(16, 42)),
        events = listOf(
            Event(1, "Seminar & Reading", today.atTime(9, 30), today.atTime(11, 30)),
            Event(2, "Mindful Lunch & Stroll", today.atTime(13, 15), today.atTime(14, 0)),
            Event(3, "Deep Focus: Concept Synthesis", today.atTime(16, 0), today.atTime(17, 0)),
            Event(4, "Gym & Hydro-Stretch", today.atTime(17, 0), today.atTime(18, 0)),
            Event(5, "Quiet Studio Study", today.atTime(19, 0), today.atTime(20, 30)),
            Event(6, "Dinner in Evening Pool", today.atTime(21, 0), today.atTime(22, 0)),
        ),
        notion = TimeNotation.H24
    )
    WaterFlowVisualization(day = day)
}