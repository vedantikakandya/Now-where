package com.example.now_where.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val AddButtonColor = Color(0xFFFF7A2E)
private val AddButtonIconColor = Color(0xFF2A1305)

/**
 * The one "+" button used on every time-perception screen.
 * Host it once (Scaffold's floatingActionButton, or a Box overlay) so the
 * button stays put while the visualization underneath changes.
 *
 * Animations:
 *  - pops in with a springy overshoot the first time it appears
 *  - sinks and turns the "+" a quarter turn while pressed
 */
@Composable
fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enterDelayMs: Long = 400L,
    contentDescription: String = "Add event"
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Entrance pop
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(enterDelayMs)
        appear.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    // Press feedback
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    val iconTurn by animateFloatAsState(
        targetValue = if (pressed) 90f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "iconTurn"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = appear.value * pressScale
                scaleX = s
                scaleY = s
                alpha = appear.value.coerceIn(0f, 1f)
            }
            .shadow(14.dp, CircleShape, ambientColor = AddButtonColor, spotColor = AddButtonColor)
            .size(56.dp)
            .clip(CircleShape)
            .background(AddButtonColor)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = contentDescription,
            tint = AddButtonIconColor,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { rotationZ = iconTurn }
        )
    }
}

@Preview(widthDp = 390, heightDp = 300, showBackground = true, backgroundColor = 0xFF0A121B)
@Composable
private fun AddButtonPreview() {
    Box(Modifier.fillMaxSize()) {
        AddButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        )
    }
}