package com.example.now_where.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ---------------------------------------------------------------------------
// Palette (same dark UI as ProfileScreen)
// ---------------------------------------------------------------------------

private val Card = Color(0xFF19222D)
private val Divider = Color(0xFF2A3541)
private val Ink = Color(0xFFE8EEF4)
private val Sub = Color(0xFFA9B5C1)
private val Muted = Color(0xFF6B7885)
private val Orange = Color(0xFFFF7A2E)
private val OrangeSoft = Color(0xFFF2A878)
private val OnOrange = Color(0xFF2A1305)
private val Mono = FontFamily.Monospace

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

private const val CONTACT_EMAIL = "vedantikakandyadev@gmail.com"

private sealed interface Block {
    data class Heading(val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullets(val items: List<String>) : Block
    data object Contact : Block
}

private val policy: List<Block> = listOf(
    Block.Heading("1. Introduction"),
    Block.Paragraph(
        "Nowhere is designed to help people experience and understand time in ways that work for them. " +
                "We believe that using a time-awareness tool should not require sharing unnecessary personal information."
    ),
    Block.Paragraph(
        "This Privacy Policy explains what information Nowhere stores, how it is used, and what choices you have."
    ),

    Block.Heading("2. Information We Store"),
    Block.Paragraph(
        "Nowhere does not require you to create an account or provide your name, email address, phone number, or password."
    ),
    Block.Paragraph("The app stores information locally on your device, which may include:"),
    Block.Bullets(
        listOf(
            "Your preferred time visualization and time format.",
            "Accessibility and display preferences.",
            "Events and their associated details, such as titles and times.",
            "Timer settings and other preferences needed for the app to function."
        )
    ),
    Block.Paragraph(
        "This information is used to remember your choices and provide the app's core features."
    ),

    Block.Heading("3. Where Your Data Is Stored"),
    Block.Paragraph(
        "Your preferences and events are stored locally on your device. In the initial version, Nowhere does not " +
                "send this information to a server or synchronize it with a cloud account."
    ),
    Block.Paragraph(
        "Your data may be lost if you uninstall the app, clear its storage, or lose access to your device. " +
                "Device-level backups may be governed by your operating system's settings."
    ),

    Block.Heading("4. Analytics and Advertising"),
    Block.Paragraph(
        "The initial version of Nowhere does not use advertising trackers or analytics services to profile users. " +
                "It does not sell personal information."
    ),
    Block.Paragraph(
        "If this changes in a future version, this policy will be updated to explain the relevant data practices " +
                "before they are introduced."
    ),

    Block.Heading("5. Permissions"),
    Block.Paragraph(
        "Nowhere aims to request only the device permissions necessary for features you choose to use. If a future " +
                "feature requires additional permissions, the app will explain their purpose where appropriate."
    ),

    Block.Heading("6. Sharing of Information"),
    Block.Paragraph(
        "Because the initial version stores app information locally and does not operate a backend service, Nowhere " +
                "does not transmit your stored events or preferences to us or share them with third parties through its " +
                "core functionality."
    ),

    Block.Heading("7. Your Choices and Data Deletion"),
    Block.Paragraph(
        "You can change your preferences and delete your events within the app, where those controls are available."
    ),
    Block.Paragraph(
        "You can also remove locally stored app data through your device settings or uninstall the app. These " +
                "actions may permanently erase the information stored by Nowhere on that device."
    ),

    Block.Heading("8. Children's Privacy"),
    Block.Paragraph(
        "Nowhere does not require users to submit personal information to use its core features. If the app is later " +
                "designed or marketed specifically to children, its privacy practices and applicable requirements will be " +
                "reviewed accordingly."
    ),

    Block.Heading("9. Changes to This Policy"),
    Block.Paragraph(
        "This policy may be updated if Nowhere's features or data practices change. The revised policy will include " +
                "an updated date."
    ),

    Block.Heading("10. Contact"),
    Block.Paragraph("If you have questions about this Privacy Policy, contact:"),
    Block.Contact
)

// ---------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------

/**
 * Full privacy policy in a modal. Pops in with a soft fade + scale (skipped when [motion] is false).
 * The body scrolls; the header and the Close button stay put.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit,
    motion: Boolean = true
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val progress = remember { Animatable(if (motion) 0f else 1f) }
        LaunchedEffect(Unit) {
            if (motion) progress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        }
        val shape = RoundedCornerShape(26.dp)

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .fillMaxWidth()
                .heightIn(max = 700.dp)
                .graphicsLayer {
                    alpha = progress.value
                    val s = 0.94f + 0.06f * progress.value
                    scaleX = s
                    scaleY = s
                }
                .clip(shape)
                .background(Card)
                .border(1.dp, Divider, shape)
        ) {
            // Header (fixed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Privacy Policy", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        text = "Nowhere · Last updated: October 21, 2026",
                        fontFamily = Mono,
                        fontSize = 12.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Sub, modifier = Modifier.size(22.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))

            // Body (scrolls)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                policy.forEachIndexed { i, block ->
                    when (block) {
                        is Block.Heading -> {
                            if (i != 0) Spacer(Modifier.height(24.dp))
                            Text(block.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Spacer(Modifier.height(8.dp))
                        }
                        is Block.Paragraph -> Text(
                            text = block.text,
                            fontSize = 14.5.sp,
                            lineHeight = 22.sp,
                            color = Sub,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        is Block.Bullets -> Column(
                            modifier = Modifier.padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            block.items.forEach { BulletRow(it) }
                        }
                        Block.Contact -> {
                            val uri = LocalUriHandler.current
                            Text(
                                text = CONTACT_EMAIL,
                                fontFamily = Mono,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangeSoft,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { runCatching { uri.openUri("mailto:$CONTACT_EMAIL") } }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Footer (fixed)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Orange)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Close",
                    fontFamily = Mono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnOrange,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BulletRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 9.dp, end = 12.dp, start = 4.dp)
                .size(6.dp)
                .background(Orange, CircleShape)
        )
        Text(text = text, fontSize = 14.5.sp, lineHeight = 22.sp, color = Sub, modifier = Modifier.weight(1f))
    }
}

@Preview(widthDp = 390, heightDp = 800, showBackground = true, backgroundColor = 0xFF070E15)
@Composable
private fun PrivacyPolicyDialogPreview() {
    PrivacyPolicyDialog(onDismiss = {})
}