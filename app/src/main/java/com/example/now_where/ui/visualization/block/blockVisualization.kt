package com.example.now_where.ui.visualization.block

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.now_where.domain.DayState
import com.example.now_where.domain.Event
import com.example.now_where.domain.TimeNotation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Pal {
    val Bg = Color(0xFF0A121B)
    val Card = Color(0xFF111B26)
    val Surface = Color(0xFF131E2A)
    val Border = Color(0xFF1D2935)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF5E6B78)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val OnOrange = Color(0xFF2A1305)
    val Green = Color(0xFF3FB68B)
    val Blue = Color(0xFF5BB8F0)
    val Violet = Color(0xFFA78BFA)
}

private val Mono = FontFamily.Monospace

// ---------------------------------------------------------------------------
// Model
// ---------------------------------------------------------------------------

private enum class Kind(val tag: String, val long: String, val color: Color, val icon: ImageVector?) {
    NOURISH("NOURISH", "Nourish", Pal.Green, Icons.Default.Restaurant),
    KINETIC("KINETIC", "Kinetic", Pal.Blue, Icons.Default.OpenInFull),
    FOCUS("FOCUS", "Deep Focus", Pal.OrangeSoft, Icons.Default.Psychology),
    OTHER("ANCHORED", "Anchored", Pal.Violet, null)
}

private enum class BlockState { PAST, NOW, ANCHORED, VOID }

private data class BlockUi(
    val index: Int,
    val startMinute: Int,
    val endMinute: Int,
    val event: Event?,
    val state: BlockState,
    val kind: Kind?,
    val part: Int,          // 1-based position inside a multi-block event
    val partCount: Int,     // how many blocks the event covers
    val title: String
)

// Derived from the event title, because Event has no category field.
// Replace with a real category on Event when you add one.
private fun kindOf(title: String): Kind {
    val t = title.lowercase()
    return when {
        listOf("lunch", "dinner", "breakfast", "meal", "eat").any { it in t } -> Kind.NOURISH
        listOf("gym", "workout", "run", "walk", "exercise", "sport").any { it in t } -> Kind.KINETIC
        listOf("study", "focus", "code", "coding", "work", "project", "synthesis", "read").any { it in t } -> Kind.FOCUS
        else -> Kind.OTHER
    }
}

private fun LocalDateTime.minuteOfDay(): Int = hour * 60 + minute

private fun buildBlocks(
    day: DayState,
    anchoredIds: Set<Long>?,
    startHour: Int,
    endHourExclusive: Int,
    blockMinutes: Int
): List<BlockUi> {
    val nowMin = day.now.minuteOfDay()
    val total = (endHourExclusive - startHour) * 60 / blockMinutes

    // Pick the event with the biggest overlap for each block
    val eventForBlock = (0 until total).map { i ->
        val s = startHour * 60 + i * blockMinutes
        val e = s + blockMinutes
        day.events
            .map { ev ->
                val overlap = minOf(e, ev.end.minuteOfDay()) - maxOf(s, ev.start.minuteOfDay())
                ev to overlap
            }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    val blocksOfEvent = eventForBlock.withIndex()
        .filter { it.value != null }
        .groupBy({ it.value!!.id }, { it.index })

    return (0 until total).map { i ->
        val s = startHour * 60 + i * blockMinutes
        val e = s + blockMinutes
        val ev = eventForBlock[i]
        val anchored = ev != null && (anchoredIds == null || ev.id in anchoredIds)
        val isNow = nowMin in s until e
        val isPast = e <= nowMin

        val state = when {
            isNow -> BlockState.NOW
            ev != null && (anchored || !isPast) -> BlockState.ANCHORED
            isPast -> BlockState.PAST
            else -> BlockState.VOID
        }
        val siblings = ev?.let { blocksOfEvent[it.id] }.orEmpty()
        val voidTitle = if (s / 60 >= 22 || s / 60 < 6) "Rest Void" else "Unoccupied"

        BlockUi(
            index = i,
            startMinute = s,
            endMinute = e,
            event = ev,
            state = state,
            kind = ev?.let { kindOf(it.title) },
            part = siblings.indexOf(i) + 1,
            partCount = siblings.size,
            title = ev?.title ?: voidTitle
        )
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun BlocksVisualization(
    day: DayState,
    modifier: Modifier = Modifier,
    anchoredIds: Set<Long>? = null,          // null = every event counts as anchored
    blockMinutes: Int = 60,
    dayStartHour: Int = 7,
    dayEndHourExclusive: Int = 23,
    onBlockAction: (BlockAction) -> Unit = {}
) {
    val blocks = remember(day, anchoredIds, blockMinutes, dayStartHour, dayEndHourExclusive) {
        buildBlocks(day, anchoredIds, dayStartHour, dayEndHourExclusive, blockMinutes)
    }
    val nowMin = day.now.minuteOfDay()
    val nowIndex = blocks.indexOfFirst { it.state == BlockState.NOW }

    var selected by remember { mutableStateOf(if (nowIndex >= 0) nowIndex else 0) }
    val selectedBlock = blocks.getOrNull(selected) ?: blocks.first()

    // One shared glow value for the NOW card and its detail
    val glowTransition = rememberInfiniteTransition(label = "glow")
    val glow by glowTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowValue"
    )

    val scroll = rememberScrollState()
    var gridTop by remember { mutableStateOf(-1) }
    val density = LocalDensity.current
    val rowStep = with(density) { (CARD_HEIGHT + CARD_GAP).roundToPx() }

    // Once the grid is measured, glide down so the NOW row is in view
    LaunchedEffect(gridTop, nowIndex) {
        if (gridTop >= 0 && nowIndex >= 0) {
            delay(600)
            val target = gridTop + (nowIndex / COLUMNS) * rowStep - with(density) { 160.dp.roundToPx() }
            scroll.animateScrollTo(target.coerceAtLeast(0), tween(900, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Pal.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().enterAnimation(0),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Temporal Grid",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Pal.Ink
                )
                ChunkBadge(count = blocks.size, blockMinutes = blockMinutes)
            }


            Spacer(Modifier.height(20.dp))
            Legend(
                nowLabel = formatClock(nowMin, day.notion),
                glow = glow,
                modifier = Modifier.enterAnimation(160)
            )

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.onGloballyPositioned { gridTop = it.positionInParent().y.toInt() },
                verticalArrangement = Arrangement.spacedBy(CARD_GAP)
            ) {
                blocks.chunked(COLUMNS).forEach { rowBlocks ->
                    Row(horizontalArrangement = Arrangement.spacedBy(CARD_GAP)) {
                        rowBlocks.forEach { b ->
                            BlockCard(
                                block = b,
                                selected = b.index == selected,
                                nowMinute = nowMin,
                                notion = day.notion,
                                glow = glow,
                                onClick = { selected = b.index },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(COLUMNS - rowBlocks.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Crossfade(
                targetState = selectedBlock,
                modifier = Modifier.animateContentSize(tween(250)).enterAnimation(240),
                animationSpec = tween(250),
                label = "detail"
            ) { b ->
                DetailCard(
                    detail = detailOf(b, nowMin, day.now, day.notion),
                    glow = glow,
                    onAction = { onBlockAction(BlockAction(b.event, b.startMinute, b.state == BlockState.VOID)) }
                )
            }

            Spacer(Modifier.height(14.dp))
            GroundingCard(blocks = blocks, day = day, modifier = Modifier.enterAnimation(320))

            // Room for the common AddButton that the host screen floats above this view
            Spacer(Modifier.height(96.dp))
        }
    }
}

/** What the user asked for from the detail card. */
data class BlockAction(val event: Event?, val startMinute: Int, val isOpenSpace: Boolean)

private const val COLUMNS = 4
private val CARD_HEIGHT = 96.dp
private val CARD_GAP = 10.dp

// ---------------------------------------------------------------------------
// Title chip, legend
// ---------------------------------------------------------------------------

@Composable
private fun ChunkBadge(count: Int, blockMinutes: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Pal.Card)
            .border(1.dp, Pal.Border, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.GridView, null, tint = Pal.OrangeSoft, modifier = Modifier.size(12.dp))
        Text(
            text = "$count CHUNKS · ${blockMinutes}M",
            fontFamily = Mono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = Pal.Ink,
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
private fun Legend(nowLabel: String, glow: Float, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Pal.Card.copy(alpha = 0.6f))
            .border(1.dp, Pal.Border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem("Past", Pal.Muted, outlineOnly = true)
        LegendItem("Now ($nowLabel)", Pal.Orange, alpha = glow)
        LegendItem("Anchored", Pal.Blue)
        LegendItem("Open Space", Pal.Muted, dashed = true)
    }
}

@Composable
private fun LegendItem(
    text: String,
    color: Color,
    outlineOnly: Boolean = false,
    dashed: Boolean = false,
    alpha: Float = 1f
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = if (dashed) CircleShape else RoundedCornerShape(3.dp)
        val dot = Modifier.size(11.dp)
        when {
            dashed -> Box(dot.dashedBorder(color, 6.dp))
            outlineOnly -> Box(dot.border(1.dp, color, shape))
            else -> Box(dot.background(color.copy(alpha = alpha), shape))
        }
        Text(
            text = text,
            fontFamily = Mono,
            fontSize = 11.sp,
            color = if (color == Pal.Blue) Pal.Blue else if (color == Pal.Orange) Pal.OrangeSoft else Pal.Sub,
            maxLines = 1,
            softWrap = false
        )
    }
}

// ---------------------------------------------------------------------------
// Block card
// ---------------------------------------------------------------------------

@Composable
private fun BlockCard(
    block: BlockUi,
    selected: Boolean,
    nowMinute: Int,
    notion: TimeNotation,
    glow: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Staggered entrance
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(block.index * 35L)
        shown = true
    }
    val appear by animateFloatAsState(if (shown) 1f else 0f, tween(350), label = "appear")
    val lift by animateFloatAsState(if (selected) 1.04f else 1f, tween(180), label = "lift")

    // Press feedback: the card sinks slightly under the finger
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )

    val kindColor = block.kind?.color ?: Pal.Muted
    val isNow = block.state == BlockState.NOW
    val isVoid = block.state == BlockState.VOID
    val isPast = block.state == BlockState.PAST

    // NOW card: a tint sweeps in to show how much of this block has already elapsed
    val elapsed = ((nowMinute - block.startMinute).toFloat() / (block.endMinute - block.startMinute))
        .coerceIn(0f, 1f)
    val sweep by animateFloatAsState(
        targetValue = if (shown && isNow) elapsed else 0f,
        animationSpec = tween(1200, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "sweep"
    )

    val targetBorder = when {
        isNow -> Pal.Orange.copy(alpha = glow)
        selected -> Pal.Ink.copy(alpha = 0.85f)
        block.state == BlockState.ANCHORED -> kindColor.copy(alpha = 0.5f)
        isVoid -> Color.Transparent
        else -> Pal.Border
    }
    val borderColor by animateColorAsState(targetBorder, tween(200), label = "border")

    val bg = when (block.state) {
        BlockState.NOW -> Pal.Card
        BlockState.ANCHORED -> kindColor.copy(alpha = 0.08f)
        BlockState.PAST -> Pal.Card.copy(alpha = 0.55f)
        BlockState.VOID -> Color.Transparent
    }
    val timeColor = when (block.state) {
        BlockState.NOW -> Pal.OrangeSoft
        BlockState.ANCHORED -> kindColor
        BlockState.PAST -> Pal.Muted
        BlockState.VOID -> Pal.Sub
    }
    val titleColor = when (block.state) {
        BlockState.NOW -> Pal.OrangeSoft
        BlockState.ANCHORED -> Pal.Ink
        BlockState.PAST -> Pal.Muted
        BlockState.VOID -> Pal.Sub
    }
    val tag = when (block.state) {
        BlockState.NOW -> "NOW"
        BlockState.PAST -> "PASSED"
        BlockState.VOID -> "OPEN VOID"
        BlockState.ANCHORED -> {
            val base = block.kind?.tag ?: "ANCHORED"
            if (block.partCount > 1) "$base ${block.part}/${block.partCount}" else base
        }
    }
    val tagColor = when (block.state) {
        BlockState.NOW -> Pal.Orange
        BlockState.ANCHORED -> kindColor
        else -> Pal.Muted
    }

    val shape = RoundedCornerShape(14.dp)
    var card = modifier
        .height(CARD_HEIGHT)
        .graphicsLayer {
            alpha = appear
            val s = (0.9f + 0.1f * appear) * lift * pressScale
            scaleX = s
            scaleY = s
        }
    if (isNow) card = card.shadow(14.dp, shape, ambientColor = Pal.Orange, spotColor = Pal.Orange)
    card = card.background(bg, shape)
    card = if (isVoid && !selected) {
        card.dashedBorder(Pal.Muted.copy(alpha = 0.6f), 14.dp)
    } else {
        card.border(if (isNow || selected) 2.dp else 1.dp, borderColor, shape)
    }

    Column(
        modifier = card
            .clip(shape)
            .drawBehind {
                if (sweep > 0f) {
                    drawRect(Pal.Orange.copy(alpha = 0.14f), size = Size(size.width * sweep, size.height))
                    drawRect(
                        Pal.Orange,
                        topLeft = Offset(0f, size.height - 3.dp.toPx()),
                        size = Size(size.width * sweep, 3.dp.toPx())
                    )
                }
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatClock(block.startMinute, notion),
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = timeColor
            )
            CardIcon(block, kindColor, nowMinute)
        }

        Text(
            text = block.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (isPast) TextDecoration.LineThrough else null
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                fontFamily = Mono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp,
                color = tagColor,
                maxLines = 1,
                softWrap = false
            )
            if (isNow) {
                Text(
                    text = formatDuration(block.endMinute - nowMinute),
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Ink
                )
            }
        }
    }
}

@Composable
private fun CardIcon(block: BlockUi, kindColor: Color, nowMinute: Int) {
    val size = Modifier.size(14.dp)
    when (block.state) {
        BlockState.PAST -> Icon(Icons.Default.Check, null, tint = Pal.Muted, modifier = size)
        BlockState.NOW -> Box(size.border(2.dp, Pal.Orange.copy(alpha = 0.5f), CircleShape))
        BlockState.VOID ->
            if (block.title == "Rest Void") {
                Icon(Icons.Default.DarkMode, null, tint = Pal.Sub, modifier = size)
            } else {
                Box(Modifier.size(6.dp).background(Pal.Muted, CircleShape))
            }
        BlockState.ANCHORED -> {
            val isPastAnchored = block.endMinute <= nowMinute
            val icon = when {
                isPastAnchored -> null
                block.kind == Kind.FOCUS && block.part > 1 -> Icons.Default.Layers
                else -> block.kind?.icon
            }
            if (icon != null) {
                Icon(icon, null, tint = kindColor, modifier = size)
            } else {
                Box(Modifier.size(6.dp).background(kindColor, CircleShape))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Detail card
// ---------------------------------------------------------------------------

private data class Detail(
    val badge: String,
    val badgeColor: Color,
    val badgeFilled: Boolean,
    val range: String,
    val rangeColor: Color,
    val bigValue: String,
    val bigLabel: String,
    val title: String,
    val body: String,
    val footer: String,
    val action: String?
)

private fun detailOf(b: BlockUi, nowMin: Int, now: LocalDateTime, notion: TimeNotation): Detail {
    val start = formatClock(b.startMinute, notion)
    val end = formatClock(b.endMinute, notion)
    val kindLong = b.kind?.long ?: "Open Space"

    return when (b.state) {
        BlockState.NOW -> {
            val left = b.endMinute - nowMin
            Detail(
                badge = "NOW\nOCCUPYING", badgeColor = Pal.Orange, badgeFilled = true,
                range = "$start — $end (${formatClock(nowMin, notion)} NOW)", rangeColor = Pal.OrangeSoft,
                bigValue = formatDuration(left), bigLabel = "TO BOUNDARY",
                title = b.title,
                body = "Occupying this container exclusively. ${plural(left, "minute")} remaining until arrival threshold.",
                footer = "$kindLong · Active",
                action = "FOCUS SPACE"
            )
        }
        BlockState.ANCHORED -> {
            val past = b.endMinute <= nowMin
            val until = b.startMinute - nowMin
            Detail(
                badge = if (past) "DONE" else "UPCOMING",
                badgeColor = b.kind?.color ?: Pal.Blue, badgeFilled = false,
                range = "$start — $end", rangeColor = b.kind?.color ?: Pal.Blue,
                bigValue = if (past) formatDuration(b.endMinute - b.startMinute) else formatDuration(until),
                bigLabel = if (past) "COMPLETED" else "UNTIL START",
                title = b.title,
                body = if (past) "This anchored block has already passed."
                else "Anchored for $start. It arrives in ${formatDuration(until)}.",
                footer = "$kindLong · ${if (past) "Completed" else "Scheduled"}",
                action = null
            )
        }
        BlockState.PAST -> Detail(
            badge = "PASSED", badgeColor = Pal.Muted, badgeFilled = false,
            range = "$start — $end", rangeColor = Pal.Sub,
            bigValue = formatDuration(b.endMinute - b.startMinute), bigLabel = "COMPLETED",
            title = b.title,
            body = "This chunk has passed.",
            footer = "Past · Closed", action = null
        )
        BlockState.VOID -> {
            val until = b.startMinute - nowMin
            Detail(
                badge = "OPEN\nVOID", badgeColor = Pal.Sub, badgeFilled = false,
                range = "$start — $end", rangeColor = Pal.Sub,
                bigValue = formatDuration(until.coerceAtLeast(0)), bigLabel = "UNTIL START",
                title = b.title,
                body = "Nothing is anchored here. Add an event to place something in this space.",
                footer = "Open Space · Free", action = "ADD EVENT"
            )
        }
    }
}

@Composable
private fun DetailCard(detail: Detail, glow: Float, onAction: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Border, shape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (detail.badgeFilled) detail.badgeColor else Color.Transparent)
                    .border(1.dp, detail.badgeColor, RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Text(
                    text = detail.badge,
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 13.sp,
                    color = if (detail.badgeFilled) Pal.OnOrange else detail.badgeColor
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = detail.range,
                fontFamily = Mono,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = detail.rangeColor,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = detail.bigValue,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.Ink
                )
                Text(
                    text = detail.bigLabel,
                    fontFamily = Mono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = Pal.Sub
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = detail.title,
            fontSize = 25.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = Pal.Ink
        )
        Text(
            text = detail.body,
            fontSize = 14.sp,
            lineHeight = 24.sp,
            color = Pal.Sub,
            modifier = Modifier.padding(top = 12.dp)
        )

    }
}

// ---------------------------------------------------------------------------
// Grounding card
// ---------------------------------------------------------------------------

@Composable
private fun GroundingCard(blocks: List<BlockUi>, day: DayState, modifier: Modifier = Modifier) {
    val nowMin = day.now.minuteOfDay()
    val active = blocks.count { it.state == BlockState.NOW }
    val anchoredAhead = blocks
        .filter { it.state == BlockState.ANCHORED && it.startMinute > nowMin }
        .mapNotNull { it.event?.id }
        .distinct()
        .size
    val voidsAhead = blocks.count { it.state == BlockState.VOID && it.startMinute >= nowMin }

    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Surface.copy(alpha = 0.7f))
            .border(1.dp, Pal.Border, shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(52.dp).background(Pal.Blue.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SelfImprovement, null, tint = Pal.Blue, modifier = Modifier.size(26.dp))
        }
        Column {
            Text("Temporal Grounding", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
            Text(
                text = buildString {
                    append(if (active > 0) "One chunk active. " else "No chunk active. ")
                    append("$anchoredAhead anchored ${if (anchoredAhead == 1) "event" else "events"} ahead. ")
                    append("$voidsAhead quiet void ${if (voidsAhead == 1) "buffer remains" else "buffers remain"} open today.")
                },
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = Pal.Sub,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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

private fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.dp): Modifier =
    this.drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        )
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

private fun plural(n: Int, word: String): String = if (n == 1) "1 $word" else "$n ${word}s"

// ---------------------------------------------------------------------------
// Preview (matches the reference screenshot)
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 1300, showBackground = true)
@Composable
private fun BlocksVisualizationPreview() {
    val today = LocalDate.now()
    fun ev(id: Long, title: String, sh: Int, eh: Int) =
        Event(id, title, today.atTime(sh, 0), today.atTime(eh, 0))

    val day = DayState(
        now = today.atTime(LocalTime.of(16, 42)),
        events = listOf(
            ev(1, "Wake", 7, 8),
            ev(2, "Ritual", 8, 9),
            ev(3, "Focus Lab", 9, 10),
            ev(4, "Reading", 10, 11),
            ev(5, "Notes", 11, 12),
            ev(6, "Buffer", 12, 13),
            ev(7, "Lunch", 13, 14),
            ev(8, "Research", 14, 15),
            ev(9, "Comms", 15, 16),
            ev(10, "Deep Synthesis & Reset", 16, 17),
            ev(11, "Gym Lab", 17, 18),
            ev(12, "Study Lab", 19, 21),
            ev(13, "Dinner", 21, 22)
        ),
        notion = TimeNotation.H24
    )
    BlocksVisualization(
        day = day,
        anchoredIds = setOf(7L, 10L, 11L, 12L, 13L)
    )
}