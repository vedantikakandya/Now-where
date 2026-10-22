package com.example.now_where.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.now_where.R
import com.example.now_where.ui.theme.VibrationHelper

private object Pall {
    val Bg = Color(0xFF070E15)
    val Card = Color(0xFF19222D)
    val OrangeSoft = Color(0xFFF2A878)
}

/**
 * The single top bar for the main screens.
 *
 * - Left: the app logo. Tap opens Profile.
 * - Right: an arrow. Tap opens the "How do you want to see time?" page.
 */
@Composable
fun NowWhereTopBar(
    onLogoClick: () -> Unit,
    onPreferenceClick: () -> Unit,
    modifier: Modifier = Modifier,
    haptics: Boolean = true,
    motion: Boolean = true,
    logo: @Composable () -> Unit = {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher),
            contentDescription = "Logo",
            modifier = Modifier.size(36.dp)
        )
    }
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Pall.Bg)
            .statusBarsPadding()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PressableCircle(
            size = 42,
            onClick = {
                VibrationHelper.performTouchHaptic(context, haptics)
                onLogoClick()
            },
            motion = motion,
            description = "Open profile",
            background = Color.Transparent
        ) { logo() }

        Box(Modifier.weight(1f))

        PressableCircle(
            size = 42,
            onClick = {
                VibrationHelper.performTouchHaptic(context, haptics)
                onPreferenceClick()
            },
            motion = motion,
            description = "Choose how to see time",
            background = Pall.Card
        ) {
            // The arrow nudges to the right now and then, hinting that it leads somewhere
            val nudge by rememberInfiniteTransition(label = "nudge").animateFloat(
                initialValue = 0f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "nudgeX"
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Pall.OrangeSoft,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { translationX = if (motion) nudge.dp.toPx() else 0f }
            )
        }
    }
}

@Composable
private fun PressableCircle(
    size: Int,
    onClick: () -> Unit,
    motion: Boolean,
    description: String,
    background: Color,
    content: @Composable () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = if (motion) spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "press"
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Preview(widthDp = 390, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun NowWhereTopBarPreview() {
    NowWhereTopBar(onLogoClick = {}, onPreferenceClick = {})
}
