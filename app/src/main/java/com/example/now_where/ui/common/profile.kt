package com.example.now_where.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.example.now_where.ui.theme.VibrationHelper
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.now_where.domain.TimeNotation
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Palette (dark UI from the design). Swap for NowWhereColors if you have matches.
// ---------------------------------------------------------------------------

private object Pal {
    val Bg = Color(0xFF070E15)
    val Card = Color(0xFF19222D)
    val Divider = Color(0xFF232E3A)
    val Ink = Color(0xFFE8EEF4)
    val Sub = Color(0xFF9BA8B5)
    val Muted = Color(0xFF6B7885)
    val Orange = Color(0xFFFF7A2E)
    val OrangeSoft = Color(0xFFF2A878)
    val Peach = Color(0xFFFFC4A3)
    val OnOrange = Color(0xFF2A1305)
    val Salmon = Color(0xFFF2A98C)
    val SalmonSub = Color(0xFFB98672)
    val OffTrack = Color(0xFF343C47)
    val SegmentBg = Color(0xFF0B1118)
}

private val Mono = FontFamily.Monospace

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

data class ProfileUiState(
    val defaultVisualizationName: String = "Route",
    val notation: TimeNotation = TimeNotation.H12,
    val haptics: Boolean = true
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

/**
 * Stateless: the caller owns [state] and reacts to the callbacks.
 * Sections: TIME, ACCESSIBILITY, ABOUT (Your privacy opens the policy modal), ACCOUNT.
 * [motion] = false skips the entrance, switch and modal animations.
 */
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onDefaultVisualizationClick: () -> Unit,
    onNotationChange: (TimeNotation) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onLogOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    motion: Boolean = true,
    appVersion: String = "0.9.4"
) {
    val context = LocalContext.current
    fun tick() {
        VibrationHelper.performTouchHaptic(context, state.haptics)
    }

    var confirmDelete by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Pal.Bg)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.padding(top = 28.dp, start = 4.dp).enterAnimation(0, motion),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Pal.Ink,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = Pal.Ink)
        }

        // ---- TIME
        Spacer(Modifier.height(36.dp))
        SectionLabel("TIME", "ANCHOR", Modifier.enterAnimation(100, motion))
        Spacer(Modifier.height(10.dp))
        SettingsCard(Modifier.enterAnimation(140, motion)) {
            SettingRow(
                title = "Default visualization",
                subtitle = null,
                onClick = onDefaultVisualizationClick
            ) {
                Text(
                    text = state.defaultVisualizationName,
                    fontFamily = Mono,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.OrangeSoft
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Pal.Peach,
                    modifier = Modifier.size(22.dp)
                )
            }
            RowDivider()
            SettingRow(
                title = "Time notation",
                subtitle = "Current rhythm: ${if (state.notation == TimeNotation.H12) "12H" else "24H"}",
                subtitleMono = true
            ) {
                NotationToggle(
                    selected = state.notation,
                    motion = motion,
                    onSelect = {
                        if (it != state.notation) {
                            tick()
                            onNotationChange(it)
                        }
                    }
                )
            }
        }

        // ---- ACCESSIBILITY
        Spacer(Modifier.height(34.dp))
        SectionLabel("ACCESSIBILITY", null, Modifier.enterAnimation(200, motion))
        Spacer(Modifier.height(10.dp))
        SettingsCard(Modifier.enterAnimation(240, motion)) {
            SettingRow(title = "Haptic feedback", subtitle = "Soft sensory anchors") {
                NowSwitch(
                    checked = state.haptics,
                    motion = motion,
                    onCheckedChange = {
                        if (it) tick()
                        onHapticsChange(it)
                    }
                )
            }
        }

        // ---- ABOUT
        Spacer(Modifier.height(34.dp))
        SectionLabel("ABOUT", null, Modifier.enterAnimation(300, motion))
        Spacer(Modifier.height(10.dp))
        SettingsCard(Modifier.enterAnimation(340, motion)) {
            SettingRow(
                title = "Your privacy",
                subtitle = "How Nowhere handles your data",
                onClick = {
                    tick()
                    showPrivacy = true
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Pal.Peach,
                    modifier = Modifier.size(22.dp)
                )
            }
        }



        // Footer
        Spacer(Modifier.height(44.dp))
        Column(
            modifier = Modifier.fillMaxWidth().enterAnimation(520, motion),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NOWHERE OS • $appVersion",
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.5.sp,
                color = Pal.Muted
            )
            Text(
                text = "Grounding the present landscape",
                fontSize = 14.sp,
                color = Pal.Muted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showPrivacy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacy = false }, motion = motion)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Pal.Card,
            title = { Text("Delete account?", color = Pal.Ink) },
            text = {
                Text(
                    "This erases all local anchors and data. It can't be undone.",
                    color = Pal.Sub
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDeleteAccount()
                }) { Text("Delete account", color = Pal.Salmon) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Pal.Sub) }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Sections and rows
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(left: String, right: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = left,
            fontFamily = Mono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Pal.Muted
        )
        if (right != null) {
            Text(
                text = right,
                fontFamily = Mono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Pal.OrangeSoft
            )
        }
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Pal.Card)
    ) { content() }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Pal.Divider))
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    subtitleMono: Boolean = false,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Pal.Ink,
    subtitleColor: Color = Pal.Sub,
    titleSize: TextUnit = 17.sp,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = titleSize, fontWeight = FontWeight.Normal, color = titleColor)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontFamily = if (subtitleMono) Mono else FontFamily.Default,
                    fontSize = if (subtitleMono) 13.sp else 14.sp,
                    color = subtitleColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) { trailing() }
    }
}

// ---------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------

/** 12-Hour / 24-Hour. The orange pill slides to the chosen side. */
@Composable
private fun NotationToggle(
    selected: TimeNotation,
    motion: Boolean,
    onSelect: (TimeNotation) -> Unit
) {
    val segW = 84.dp
    val segH = 36.dp
    val pillX by animateDpAsState(
        targetValue = if (selected == TimeNotation.H12) 0.dp else segW,
        animationSpec = if (motion) spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "pill"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Pal.SegmentBg)
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .offset(x = pillX)
                .size(segW, segH)
                .clip(RoundedCornerShape(10.dp))
                .background(Pal.Orange)
        )
        Row {
            Segment("12-Hour", selected == TimeNotation.H12, segW, segH, motion) { onSelect(TimeNotation.H12) }
            Segment("24-Hour", selected == TimeNotation.H24, segW, segH, motion) { onSelect(TimeNotation.H24) }
        }
    }
}

@Composable
private fun Segment(
    label: String,
    selected: Boolean,
    w: Dp,
    h: Dp,
    motion: Boolean,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (selected) Pal.OnOrange else Pal.Peach,
        animationSpec = if (motion) tween(200) else snap(),
        label = "segmentText"
    )
    Box(
        modifier = Modifier
            .size(w, h)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Mono,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** Orange track with a dark knob when on, dark track with a peach knob when off. */
@Composable
private fun NowSwitch(
    checked: Boolean,
    motion: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Pal.Orange else Pal.OffTrack,
        animationSpec = if (motion) tween(220) else snap(),
        label = "track"
    )
    val knobColor by animateColorAsState(
        targetValue = if (checked) Color(0xFF6B2606) else Color(0xFFE6C9BA),
        animationSpec = if (motion) tween(220) else snap(),
        label = "knob"
    )
    val knobX by animateDpAsState(
        targetValue = if (checked) 32.dp else 4.dp,
        animationSpec = if (motion) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "knobX"
    )

    Box(
        modifier = Modifier
            .size(width = 60.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .align(Alignment.CenterStart)
                .size(24.dp)
                .background(knobColor, CircleShape)
        )
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

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview(widthDp = 390, heightDp = 900, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun ProfilePreview() {
    var state by remember { mutableStateOf(ProfileUiState()) }
    ProfileScreen(
        state = state,
        onBack = {},
        onDefaultVisualizationClick = {},
        onNotationChange = { state = state.copy(notation = it) },
        onHapticsChange = { state = state.copy(haptics = it) },
        onLogOut = {},
        onDeleteAccount = {}
    )
}