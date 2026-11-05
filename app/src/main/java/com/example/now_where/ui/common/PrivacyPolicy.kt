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
import com.example.now_where.ui.theme.touchHaptics

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
    Block.Heading("1. Who we are"),
    Block.Paragraph(
        "now-where (“the app”) is developed by Vedantika Kandya (“we”, “us”), an independent developer. The app helps people see and understand time in different ways, for example as a route, a clock or a flow of water."
    ),
    Block.Paragraph(
        "This policy explains exactly what the app stores, what it does not collect, and what choices you have."
    ),
    Block.Heading("2. The short version"),
    Block.Bullets(
        listOf(
            "The app has no account and no sign-in.",
            "The app has no internet permission and cannot send any data over the network.",
            "Everything you enter stays on your device.",
            "There are no ads, no analytics and no third-party tracking."
        )
    ),
    Block.Heading("3. Information the app stores on your device"),
    Block.Paragraph(
        "The app stores only the following, in its private storage on your device:"
    ),
    Block.Bullets(
        listOf(
            "Events you create: the title, start time and end time.",
            "App settings: whether haptic feedback is on or off."
        )
    ),
    Block.Paragraph(
        "The app does not collect your name, email address, phone number, location, contacts, photos, camera or microphone data, advertising ID or any device identifier."
    ),
    Block.Heading("4. How this information is used"),
    Block.Paragraph(
        "Events are used to show your day in the view you choose, and to vibrate when an event starts. The haptic setting is used to remember your choice. Nothing is used for any other purpose."
    ),
    Block.Heading("5. Where your data is stored"),
    Block.Paragraph(
        "Your data is stored only on your device. We do not run a server, and we never receive your data."
    ),
    Block.Paragraph(
        "The app turns off Android’s automatic cloud backup, so your events are not copied to your Google account backup. Your data will be lost if you uninstall the app, clear its storage or lose your device."
    ),
    Block.Heading("6. Permissions"),
    Block.Bullets(
        listOf(
            "Vibrate: used to give haptic feedback when you touch the app (if you turn it on) and to vibrate when an event starts. This permission does not give the app access to any of your data."
        )
    ),
    Block.Paragraph(
        "The app requests no other permissions."
    ),
    Block.Heading("7. Advertising, analytics and third parties"),
    Block.Paragraph(
        "The app contains no advertising, no analytics or crash-reporting service, and no third-party SDK that collects data. We do not sell or share personal information, because we do not collect any."
    ),
    Block.Heading("8. Your choices and deleting your data"),
    Block.Paragraph(
        "You can edit or delete any event inside the app. You can erase everything the app has stored by clearing the app’s storage in your device settings, or by uninstalling the app. Because we hold no copy of your data, there is nothing for us to delete on our side."
    ),
    Block.Heading("9. Security"),
    Block.Paragraph(
        "Your data sits in the app’s private storage, which Android protects from other apps. The app does not add its own encryption on top of that."
    ),
    Block.Heading("10. Children"),
    Block.Paragraph(
        "The app does not collect personal information from anyone, including children, and is not directed at children specifically."
    ),
    Block.Heading("11. Changes to this policy"),
    Block.Paragraph(
        "If a future version of the app collects or shares any information, we will update this policy and its date before that version is released."
    ),
    Block.Heading("12. Contact"),
    Block.Paragraph(
        "Questions about this policy? Contact:"
    ),
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
                .touchHaptics()
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
                        text = "now-where · Last updated: November 5, 2026",
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