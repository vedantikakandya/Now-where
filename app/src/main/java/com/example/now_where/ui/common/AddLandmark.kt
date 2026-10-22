package com.example.now_where.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.now_where.domain.TimeNotation
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Ppal {
    val Bg = Color(0xFF0C131B)
    val Card = Color(0xFF18212C)
    val Inset = Color(0xFF111922)
    val Divider = Color(0xFF263240)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF6B7885)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val OnOrange = Color(0xFF4A1C06)
    val CloseBg = Color(0xFF232C37)
    val Error = Color(0xFFF2A98C)
}

private val Mono = FontFamily.Monospace

// ---------------------------------------------------------------------------
// Output
// ---------------------------------------------------------------------------

/**
 * What the sheet hands back. Times are minutes since midnight (0..1439).
 * Map this to your own Event type in the caller.
 */
data class NewEventDraft(
    val title: String,
    val description: String,
    val startMinutes: Int,
    val endMinutes: Int
)

// ---------------------------------------------------------------------------
// Dialog wrapper: full screen, opened by the shared "+" button
// ---------------------------------------------------------------------------

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAdd: (NewEventDraft) -> Unit,
    notation: TimeNotation = TimeNotation.H12,
    initialStartMinutes: Int = 17 * 60,
    motion: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        AddEventSheet(
            onDismiss = onDismiss,
            onAdd = onAdd,
            notation = notation,
            initialStartMinutes = initialStartMinutes,
            motion = motion
        )
    }
}

// ---------------------------------------------------------------------------
// Sheet content
// ---------------------------------------------------------------------------

@Composable
fun AddEventSheet(
    onDismiss: () -> Unit,
    onAdd: (NewEventDraft) -> Unit,
    modifier: Modifier = Modifier,
    notation: TimeNotation = TimeNotation.H12,
    initialStartMinutes: Int = 17 * 60,
    initialTitle: String = "",
    motion: Boolean = true
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf("") }
    var descriptionOpen by remember { mutableStateOf(false) }
    var start by remember { mutableIntStateOf(initialStartMinutes.coerceIn(0, 1439)) }
    var end by remember { mutableIntStateOf((initialStartMinutes + 90).coerceIn(0, 1439)) }
    var picking by remember { mutableStateOf<Which?>(null) }

    val duration = end - start
    val timesValid = duration > 0
    val canAdd = title.isNotBlank() && timesValid

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Ppal.Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, start = 4.dp)
                .enterAnimation(0, motion),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("What’s coming?", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ppal.Ink)
                Text(
                    "Place a landmark on your horizon.",
                    fontSize = 14.sp,
                    color = Ppal.Sub,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Ppal.CloseBg)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Ppal.Ink, modifier = Modifier.size(24.dp))
            }
        }

        // Landmark identity (+ optional description)
        Spacer(Modifier.height(36.dp))
        IdentityCard(
            title = title,
            onTitleChange = { title = it },
            description = description,
            onDescriptionChange = { description = it },
            descriptionOpen = descriptionOpen,
            onDescriptionOpenChange = { descriptionOpen = it },
            motion = motion,
            modifier = Modifier.enterAnimation(90, motion)
        )

        // Time geometry
        Spacer(Modifier.height(24.dp))
        TimeGeometryCard(
            start = start,
            end = end,
            notation = notation,
            motion = motion,
            onPickStart = { picking = Which.Entry },
            onPickExit = { picking = Which.Exit },
            modifier = Modifier.enterAnimation(170, motion)
        )

        Spacer(Modifier.height(44.dp))

        AddButton(
            enabled = canAdd,
            motion = motion,
            modifier = Modifier.enterAnimation(250, motion),
            onClick = {
                onAdd(
                    NewEventDraft(
                        title = title.trim(),
                        description = description.trim(),
                        startMinutes = start,
                        endMinutes = end
                    )
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onDismiss)
                .padding(vertical = 12.dp)
                .enterAnimation(320, motion),
            contentAlignment = Alignment.Center
        ) {
            Text("Cancel", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Ppal.Ink)
        }
        Spacer(Modifier.height(32.dp))
    }

    picking?.let { which ->
        TimePickDialog(
            heading = if (which == Which.Entry) "ENTRY" else "EXIT",
            initialMinutes = if (which == Which.Entry) start else end,
            is24h = notation == TimeNotation.H24,
            onDismiss = { picking = null },
            onConfirm = { m ->
                if (which == Which.Entry) {
                    // Keep the same length when the start moves, as long as it still fits in the day.
                    val keep = (end - start).coerceAtLeast(30)
                    start = m
                    end = (m + keep).coerceAtMost(1439)
                } else {
                    end = m
                }
                picking = null
            }
        )
    }
}

private enum class Which { Entry, Exit }

// ---------------------------------------------------------------------------
// Identity card
// ---------------------------------------------------------------------------

@Composable
private fun IdentityCard(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    descriptionOpen: Boolean,
    onDescriptionOpenChange: (Boolean) -> Unit,
    motion: Boolean,
    modifier: Modifier = Modifier
) {
    val pulse by rememberInfiniteTransition(label = "dot").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "dotPulse"
    )
    val descFocus = remember { FocusRequester() }
    LaunchedEffect(descriptionOpen) {
        if (descriptionOpen && description.isEmpty()) {
            delay(180)
            runCatching { descFocus.requestFocus() }
        }
    }
    // An existing description keeps the field open
    val showField = descriptionOpen || description.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Ppal.Card)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "LANDMARK IDENTITY",
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Ppal.Orange
                )
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Ppal.Ink),
                    cursorBrush = SolidColor(Ppal.Orange),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = if (showField) ImeAction.Next else ImeAction.Done
                    ),
                    decorationBox = { inner ->
                        Box(Modifier.padding(top = 4.dp)) {
                            if (title.isEmpty()) {
                                Text(
                                    "Name it, e.g. Gym workout",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ppal.Muted,
                                    maxLines = 1
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Box(
                Modifier
                    .padding(start = 12.dp, end = 4.dp)
                    .size(14.dp)
                    .graphicsLayer { alpha = if (motion) pulse else 1f }
                    .background(Ppal.Orange, CircleShape)
            )
        }

        // Description: a quiet "add" link that opens a multi-line field
        AnimatedVisibility(
            visible = !showField,
            enter = fadeIn(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDescriptionOpenChange(true) }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("+", fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ppal.OrangeSoft)
                Text(
                    "Add a description",
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ppal.OrangeSoft
                )
            }
        }

        AnimatedVisibility(
            visible = showField,
            enter = fadeIn(tween(220)) + expandVertically(tween(260, easing = FastOutSlowInEasing)),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Ppal.Divider))
                Text(
                    "DESCRIPTION",
                    fontFamily = Mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Ppal.Muted,
                    modifier = Modifier.padding(top = 14.dp)
                )
                BasicTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    minLines = 2,
                    maxLines = 5,
                    textStyle = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, color = Ppal.Ink),
                    cursorBrush = SolidColor(Ppal.Orange),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    decorationBox = { inner ->
                        Box(Modifier.padding(top = 6.dp)) {
                            if (description.isEmpty()) {
                                Text(
                                    "Anything that helps future you: where, what to bring, why it matters.",
                                    fontSize = 16.sp,
                                    lineHeight = 23.sp,
                                    color = Ppal.Muted
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(descFocus)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Time geometry
// ---------------------------------------------------------------------------

@Composable
private fun TimeGeometryCard(
    start: Int,
    end: Int,
    notation: TimeNotation,
    motion: Boolean,
    onPickStart: () -> Unit,
    onPickExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = end - start
    val valid = duration > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Ppal.Card)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DonutLarge,
                contentDescription = null,
                tint = Ppal.OrangeSoft,
                modifier = Modifier.size(24.dp)
            )
            Text(
                "Time Geometry",
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ppal.Ink,
                modifier = Modifier.padding(start = 8.dp).weight(1f)
            )
            Crossfade(targetState = if (valid) formatDuration(duration) else "End after start", label = "duration") {
                Text(
                    text = it,
                    fontFamily = Mono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (valid) Ppal.Ink else Ppal.Error,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Ppal.Inset)
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeBox("ENTRY", start, notation, error = false, motion = motion, onClick = onPickStart, modifier = Modifier.weight(1f))
            TimeBox("EXIT", end, notation, error = !valid, motion = motion, onClick = onPickExit, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun TimeBox(
    label: String,
    minutes: Int,
    notation: TimeNotation,
    error: Boolean,
    motion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = if (motion) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "boxScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (error) Ppal.Error else Color.Transparent,
        animationSpec = if (motion) tween(200) else snap(),
        label = "boxBorder"
    )
    val shape = RoundedCornerShape(16.dp)
    val (clock, suffix) = formatClock(minutes, notation)

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Ppal.Inset)
            .border(1.dp, borderColor, shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            fontFamily = Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Ppal.Ink
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Crossfade(targetState = clock, label = "clock$label") {
                Text(it, fontFamily = Mono, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ppal.Ink, maxLines = 1)
            }
            if (suffix != null) {
                Text(
                    suffix,
                    fontFamily = Mono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ppal.OrangeSoft,
                    modifier = Modifier.padding(start = 5.dp, bottom = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Primary button
// ---------------------------------------------------------------------------

@Composable
private fun AddButton(
    enabled: Boolean,
    motion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = if (motion) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "addScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = if (motion) tween(200) else snap(),
        label = "addAlpha"
    )
    val shape = RoundedCornerShape(18.dp)
    val glow = Ppal.Orange.copy(alpha = if (enabled) 0.55f else 0f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .shadow(24.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(Ppal.Orange)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 19.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AddCircleOutline,
            contentDescription = null,
            tint = Ppal.OnOrange,
            modifier = Modifier.size(26.dp)
        )
        Text(
            "Add to my day",
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            color = Ppal.OnOrange,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Time picker dialog
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(
    heading: String,
    initialMinutes: Int,
    is24h: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = is24h
    )
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Ppal.Card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                heading,
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = Ppal.Orange,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    containerColor = Ppal.Card,
                    clockDialColor = Ppal.Inset,
                    clockDialSelectedContentColor = Ppal.OnOrange,
                    clockDialUnselectedContentColor = Ppal.Ink,
                    selectorColor = Ppal.Orange,
                    periodSelectorBorderColor = Ppal.Divider,
                    periodSelectorSelectedContainerColor = Ppal.Orange.copy(alpha = 0.25f),
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = Ppal.OrangeSoft,
                    periodSelectorUnselectedContentColor = Ppal.Sub,
                    timeSelectorSelectedContainerColor = Ppal.Orange.copy(alpha = 0.25f),
                    timeSelectorUnselectedContainerColor = Ppal.Inset,
                    timeSelectorSelectedContentColor = Ppal.OrangeSoft,
                    timeSelectorUnselectedContentColor = Ppal.Ink
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Cancel",
                    fontSize = 16.sp,
                    color = Ppal.Sub,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
                Text(
                    "Set",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ppal.Orange,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onConfirm(state.hour * 60 + state.minute) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------

/** "05:00" with "PM" in 12-hour mode, "17:00" with no suffix in 24-hour mode. */
private fun formatClock(minutes: Int, notation: TimeNotation): Pair<String, String?> {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return if (notation == TimeNotation.H24) {
        "%02d:%02d".format(h, m) to null
    } else {
        val h12 = if (h % 12 == 0) 12 else h % 12
        "%02d:%02d".format(h12, m) to if (h < 12) "AM" else "PM"
    }
}

/** 90 -> "1 hr 30 mins", 60 -> "1 hr", 45 -> "45 mins". */
private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "$h hr $m ${if (m == 1) "min" else "mins"}"
        h > 0 -> "$h hr"
        else -> "$m ${if (m == 1) "min" else "mins"}"
    }
}

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

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 860, showBackground = true, backgroundColor = 0xFF0C131B)
@Composable
private fun AddEventSheetPreview() {
    AddEventSheet(
        onDismiss = {},
        onAdd = {},
        initialStartMinutes = 17 * 60,
        initialTitle = "Gym workout"
    )
}