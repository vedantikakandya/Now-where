package com.example.now_where.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Paal {
    val Bg = Color(0xFF070E15)
    val Card = Color(0xFF131B25)
    val CardSelected = Color(0xFF1A2230)
    val Tile = Color(0xFF0B1118)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFFD9B8A6)
    val Peach = Color(0xFFF4C9B3)
    val Gray = Color(0xFF4A525C)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val OnOrange = Color(0xFF4A1C06)
    val RadioOff = Color(0xFF2C343E)
    val Teal = Color(0xFF3F8FB5)
}

private val Mono = FontFamily.Monospace

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

/** The ten ways to perceive time. Map these to your own visualization switch. */
enum class TimePerception(val title: String, val subtitle: String) {
    Route("Route", "I am moving through my day."),
    Distance("Distance", "How close is something?"),
    DayProgress("Day Progress", "How much of my day has passed?"),
    Blocks("Blocks", "Time is made of chunks."),
    Clock("Clock", "What exact time is it?"),
    CircularDay("Circular Day", "Where am I in the 24-hour cycle?"),
    FlowOfWater("Flow of Water", "Time continuously flows."),
    TimeLadder("Time Ladder", "I am moving through time.")
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Stateless about the saved choice: [current] is the preference in use (pre-selected).
 * Tapping a row only changes the local highlight; "Use this" calls [onConfirm].
 */
@Composable
fun SelectTimePreferenceScreen(
    current: TimePerception?,
    onConfirm: (TimePerception) -> Unit,
    modifier: Modifier = Modifier,
    motion: Boolean = true,
    haptics: Boolean = true
) {
    var selected by remember { mutableStateOf(current) }
    val haptic = LocalHapticFeedback.current

    Box(modifier = modifier.fillMaxSize().background(Paal.Bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(motion) }
            itemsIndexed(TimePerception.entries) { i, option ->
                OptionRow(
                    option = option,
                    selected = option == selected,
                    motion = motion,
                    onClick = {
                        if (haptics && option != selected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        selected = option
                    },
                    modifier = Modifier.enterAnimation(80 + i * 45, motion)
                )
            }
        }

        // Pinned action with a fade so rows slide under it softly
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Paal.Bg, Paal.Bg)))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp)
        ) {
            UseThisButton(
                enabled = selected != null,
                motion = motion,
                onClick = { selected?.let(onConfirm) }
            )
        }
    }
}

@Composable
private fun Header(motion: Boolean) {
    val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotPulse"
    )
    Column(modifier = Modifier.padding(start = 2.dp, bottom = 12.dp).enterAnimation(0, motion)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .graphicsLayer { alpha = if (motion) pulse else 1f }
                    .background(Paal.Orange, CircleShape)
            )
            Text(
                "COGNITIVE CALIBRATION",
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Paal.OrangeSoft,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Text(
            "How do you want to see time?",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = Paal.Ink,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            "Choose the way that makes time easiest to understand.",
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = Paal.Sub,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Row
// ---------------------------------------------------------------------------

@Composable
private fun OptionRow(
    option: TimePerception,
    selected: Boolean,
    motion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = if (motion) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "rowScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Paal.Orange else Color.Transparent,
        animationSpec = if (motion) tween(220) else snap(),
        label = "rowBorder"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) Paal.CardSelected else Paal.Card,
        animationSpec = if (motion) tween(220) else snap(),
        label = "rowBg"
    )
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(bg)
            .border(1.5.dp, borderColor, shape)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Paal.Tile),
            contentAlignment = Alignment.Center
        ) {
            PerceptionIcon(option, selected, motion)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 18.dp, end = 8.dp)) {
            Text(option.title, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Paal.Ink, maxLines = 1)
            Text(
                option.subtitle,
                fontSize = 15.sp,
                color = Paal.Sub,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        RadioDot(selected, motion)
    }
}

@Composable
private fun RadioDot(selected: Boolean, motion: Boolean) {
    val fill by animateColorAsState(
        targetValue = if (selected) Paal.Orange else Paal.RadioOff,
        animationSpec = if (motion) tween(200) else snap(),
        label = "radioFill"
    )
    val tickScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (motion) spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "tick"
    )
    Box(
        modifier = Modifier.size(26.dp).background(fill, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Paal.OnOrange,
            modifier = Modifier
                .size(16.dp)
                .graphicsLayer { scaleX = tickScale; scaleY = tickScale; alpha = tickScale.coerceIn(0f, 1f) }
        )
    }
}

// ---------------------------------------------------------------------------
// Button
// ---------------------------------------------------------------------------

@Composable
private fun UseThisButton(enabled: Boolean, motion: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = if (motion) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "btnScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = if (motion) tween(200) else snap(),
        label = "btnAlpha"
    )
    // Arrow slides forward while pressed
    val arrowX by animateFloatAsState(
        targetValue = if (pressed && enabled) 6f else 0f,
        animationSpec = if (motion) spring(dampingRatio = 0.5f) else snap(),
        label = "btnArrow"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .clip(RoundedCornerShape(18.dp))
            .background(Paal.Orange)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(vertical = 17.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Use this", fontSize = 21.sp, fontWeight = FontWeight.Medium, color = Paal.OnOrange)
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Paal.OnOrange,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(22.dp)
                .graphicsLayer { translationX = arrowX.dp.toPx() }
        )
    }
}

// ---------------------------------------------------------------------------
// Icons (drawn, no assets). Brighter when the row is selected.
// ---------------------------------------------------------------------------

@Composable
private fun PerceptionIcon(option: TimePerception, selected: Boolean, motion: Boolean) {
    val glow by animateFloatAsState(
        targetValue = if (selected) 1f else 0.78f,
        animationSpec = if (motion) tween(250) else snap(),
        label = "iconGlow"
    )
    // Only the water wave moves, and only when selected
    val wave by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing)),
        label = "wavePhase"
    )
    val phase = if (motion && selected) wave else 0f

    Canvas(Modifier.size(30.dp).graphicsLayer { alpha = glow }) {
        when (option) {
            TimePerception.Route -> drawRoute()
            TimePerception.Distance -> drawDistance()
            TimePerception.DayProgress -> drawDayProgress()
            TimePerception.Blocks -> drawBlocks()
            TimePerception.Clock -> drawClock()
            TimePerception.CircularDay -> drawCircular()
            TimePerception.FlowOfWater -> drawWave(phase)
            TimePerception.TimeLadder -> drawLadder()
        }
    }
}

private fun DrawScope.drawRoute() {
    val cy = size.height / 2
    drawLine(Paal.Gray, Offset(2.dp.toPx(), cy), Offset(size.width - 2.dp.toPx(), cy), 1.5.dp.toPx())
    drawCircle(Paal.Peach, 3.dp.toPx(), Offset(4.dp.toPx(), cy))
    drawCircle(Paal.Gray, 2.5.dp.toPx(), Offset(size.width - 4.dp.toPx(), cy))
    drawCircle(Paal.Orange, 5.dp.toPx(), Offset(size.width / 2, cy))
}

private fun DrawScope.drawDistance() {
    val cy = size.height / 2
    val c = Offset(size.width * 0.68f, cy)
    drawLine(Paal.Gray, Offset(0f, cy), Offset(c.x - 8.dp.toPx(), cy), 1.5.dp.toPx())
    drawCircle(Paal.Peach, 7.dp.toPx(), c, style = Stroke(1.6.dp.toPx()))
    drawCircle(Paal.Peach, 3.dp.toPx(), c)
}

private fun DrawScope.drawDayProgress() {
    val h = 7.dp.toPx()
    val top = (size.height - h) / 2
    val r = CornerRadius(h / 2)
    drawRoundRect(Paal.Gray, Offset(0f, top), Size(size.width, h), r)
    drawRoundRect(Paal.Peach, Offset(0f, top), Size(size.width * 0.55f, h), r)
}

private fun DrawScope.drawBlocks() {
    val w = 7.dp.toPx()
    val h = 20.dp.toPx()
    val gap = 4.dp.toPx()
    val total = w * 3 + gap * 2
    val x0 = (size.width - total) / 2
    val top = (size.height - h) / 2
    val colors = listOf(Paal.Peach, Paal.Peach, Paal.Gray)
    colors.forEachIndexed { i, c ->
        drawRoundRect(c, Offset(x0 + i * (w + gap), top), Size(w, h), CornerRadius(2.dp.toPx()))
    }
}

private fun DrawScope.drawClock() {
    val c = Offset(size.width / 2, size.height / 2)
    val r = size.minDimension / 2 - 1.5.dp.toPx()
    val sw = 1.8.dp.toPx()
    drawCircle(Paal.Peach, r, c, style = Stroke(sw))
    drawLine(Paal.Peach, c, Offset(c.x, c.y - r * 0.55f), sw, StrokeCap.Round)
    drawLine(Paal.Peach, c, Offset(c.x + r * 0.4f, c.y + r * 0.2f), sw, StrokeCap.Round)
}

private fun DrawScope.drawCircular() {
    val r = size.minDimension / 2 - 2.dp.toPx()
    val topLeft = Offset(size.width / 2 - r, size.height / 2 - r)
    val dia = Size(r * 2, r * 2)
    val sw = 1.8.dp.toPx()
    drawArc(
        Paal.Peach.copy(alpha = 0.55f), 0f, 360f, false, topLeft, dia,
        style = Stroke(sw, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())))
    )
    drawArc(Paal.Peach, -90f, 110f, false, topLeft, dia, style = Stroke(sw, cap = StrokeCap.Round))
}

private fun DrawScope.drawWave(phase: Float) {
    val path = Path()
    val cy = size.height / 2
    val amp = 4.dp.toPx()
    val steps = 40
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val x = t * size.width
        val y = cy + amp * sin(t * 2 * PI.toFloat() * 1.1f - phase)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, Paal.Peach, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawLadder() {
    val sw = 1.8.dp.toPx()
    val l = size.width * 0.3f
    val r = size.width * 0.7f
    val top = 2.dp.toPx()
    val bottom = size.height - 2.dp.toPx()
    drawLine(Paal.Peach, Offset(l, top), Offset(l, bottom), sw, StrokeCap.Round)
    drawLine(Paal.Peach, Offset(r, top), Offset(r, bottom), sw, StrokeCap.Round)
    listOf(0.25f, 0.5f, 0.75f).forEach {
        val y = top + (bottom - top) * it
        drawLine(Paal.Peach, Offset(l, y), Offset(r, y), sw)
    }
}



// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/** Fade + slide-up played once when the item first appears. Skipped when motion is off. */
private fun Modifier.enterAnimation(delayMs: Int, enabled: Boolean): Modifier = composed {
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (enabled) {
            delay(delayMs.toLong())
            progress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

@Preview(widthDp = 390, heightDp = 860, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun SelectTimePreferencePreview() {
    SelectTimePreferenceScreen(current = TimePerception.Route, onConfirm = {})
}