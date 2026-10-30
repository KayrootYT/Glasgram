package com.glasgram.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.BuildConfig
import com.glasgram.app.data.TgRepository

// ───────────────────────── shared building blocks ─────────────────────────

@Composable
private fun SettingsGroup(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        if (title != null) {
            Text(
                title.uppercase(), color = Muted, fontSize = 12.sp, letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)
            )
        }
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(26.dp)), content = content)
    }
}

@Composable
private fun IconBadge(emoji: String, tint: Color) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        Modifier
            .size(36.dp)
            .clip(shape)
            .background(tint.copy(alpha = 0.28f))
            .border(1.dp, tint.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = 17.sp) }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 64.dp).height(1.dp).background(Color.White.copy(alpha = 0.07f)))
}

@Composable
private fun ToggleRow(
    emoji: String,
    tint: Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val haptic = rememberHaptic()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked); haptic() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(emoji, tint)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = Muted, fontSize = 12.5.sp, lineHeight = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(checked) { onChange(it); haptic() }
    }
}

@Composable
private fun SliderRow(
    emoji: String,
    tint: Color,
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(emoji, tint)
            Spacer(Modifier.width(14.dp))
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(valueLabel, color = Muted, fontSize = 14.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accent(),
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            )
        )
    }
}

@Composable
private fun PageHeader(title: String, onBack: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = 12.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            GlassCircleButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Column {
            Text("GLASGRAM", color = accent(), fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ───────────────────────── Settings tab ─────────────────────────

@Composable
fun SettingsScreen(repo: TgRepository, onOpenGlasgram: () -> Unit) {
    val s = LocalGlas.current
    val a = accent()
    var confirmLogout by remember { mutableStateOf(false) }
    val me = repo.me
    val name = listOfNotNull(me?.firstName, me?.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Your account" }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Settings", null)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 130.dp)
            ) {
                // profile
                Row(
                    Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(28.dp), boost = 1.2f)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(name, me?.id ?: 0L, 64.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        val phone = me?.phoneNumber.orEmpty()
                        if (phone.isNotBlank()) Text("+$phone", color = Muted, fontSize = 14.sp)
                    }
                }

                // Glasgram Settings entry
                Row(
                    Modifier
                        .padding(top = 18.dp)
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(28.dp), boost = 2.6f, tint = a)
                        .clickable(onClick = onOpenGlasgram)
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✦", color = Color.White, fontSize = 30.sp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Glasgram Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Ghost mode, deleted-message guard, edit history, themes and more",
                            color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp
                        )
                    }
                    Text("›", color = Color.White, fontSize = 28.sp)
                }

                SettingsGroup("Quick toggles") {
                    ToggleRow("👻", Accents[1], "Ghost Mode", "Don't send read receipts", s.ghostRead) { s.ghostRead = it }
                    RowDivider()
                    ToggleRow("🌙", Accents[0], "Always Offline", "Never appear online", s.alwaysOffline) {
                        s.alwaysOffline = it
                        repo.applyOnline()
                    }
                }

                SettingsGroup("About") {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Version", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(BuildConfig.VERSION_NAME, color = Muted, fontSize = 15.sp)
                    }
                    RowDivider()
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Engine", color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text("TDLib (official)", color = Muted, fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(22.dp))
                GlassButton("Log out", primary = false, danger = true) { confirmLogout = true }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Glasgram is an unofficial Telegram client and is not affiliated with Telegram.",
                    color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }

        if (confirmLogout) {
            GlassSheet(title = "Log out of Glasgram?", onDismiss = { confirmLogout = false }) {
                SheetAction("🚪", "Log out", danger = true) { confirmLogout = false; repo.logout() }
                SheetAction("✕", "Cancel") { confirmLogout = false }
            }
        }
    }
}

// ───────────────────────── Glasgram Settings (mod features) ─────────────────────────

@Composable
fun GlasgramSettingsScreen(repo: TgRepository, onBack: () -> Unit) {
    val s = LocalGlas.current
    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            PageHeader("Glasgram Settings", onBack)
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 40.dp)
            ) {
                SettingsGroup("Ghost & privacy") {
                    ToggleRow(
                        "👻", Accents[1], "Ghost Mode",
                        "Read chats without telling the sender. Messages stay unread until you tap “Mark read”.",
                        s.ghostRead
                    ) { s.ghostRead = it }
                    RowDivider()
                    ToggleRow(
                        "🌙", Accents[0], "Always Offline",
                        "Glasgram never sets your status to online.",
                        s.alwaysOffline
                    ) { s.alwaysOffline = it; repo.applyOnline() }
                }

                SettingsGroup("Message guard") {
                    ToggleRow(
                        "🗑", Accents[3], "Keep deleted messages",
                        "Messages deleted by others stay visible, marked as deleted. Kept while the app is running.",
                        s.keepDeleted
                    ) { s.keepDeleted = it }
                    RowDivider()
                    ToggleRow(
                        "✏️", Accents[4], "Keep edit history",
                        "Show the original text under edited messages.",
                        s.keepEdits
                    ) { s.keepEdits = it }
                }

                SettingsGroup("Appearance") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge("🎨", Accents[2])
                            Spacer(Modifier.width(14.dp))
                            Text("Accent colour", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text(AccentNames[s.accentIndex.coerceIn(0, AccentNames.lastIndex)], color = Muted, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Accents.forEachIndexed { i, c ->
                                val selected = i == s.accentIndex
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(c, c.copy(alpha = 0.55f))))
                                        .border(if (selected) 3.dp else 1.dp, Color.White.copy(alpha = if (selected) 0.95f else 0.3f), CircleShape)
                                        .clickable { s.accentIndex = i }
                                )
                            }
                        }
                    }
                    RowDivider()
                    SliderRow("🪟", Accents[0], "Glass intensity", "${(s.glassIntensity * 100).toInt()}%", s.glassIntensity, 0f..1f) { s.glassIntensity = it }
                    RowDivider()
                    SliderRow("💬", Accents[1], "Bubble roundness", "${s.bubbleRadius.toInt()} dp", s.bubbleRadius, 8f..30f) { s.bubbleRadius = it }
                    RowDivider()
                    SliderRow("🔤", Accents[4], "Message text size", "${(s.textScale * 100).toInt()}%", s.textScale, 0.85f..1.3f) { s.textScale = it }
                    RowDivider()
                    ToggleRow("✨", Accents[1], "Animated background", "Slowly drifting light orbs", s.animatedBackground) { s.animatedBackground = it }
                }

                SettingsGroup("Interface") {
                    ToggleRow("📋", Accents[0], "Compact chat list", "Smaller rows, one-line previews", s.compactList) { s.compactList = it }
                    RowDivider()
                    ToggleRow("🕐", Accents[2], "24-hour clock", null, s.use24h) { s.use24h = it }
                    RowDivider()
                    ToggleRow("#️⃣", Accents[4], "Show chat & message IDs", "Handy for bots and debugging", s.showIds) { s.showIds = it }
                    RowDivider()
                    ToggleRow("👆", Accents[3], "Double-tap to reply", "Double-tap any message bubble", s.doubleTapReply) { s.doubleTapReply = it }
                    RowDivider()
                    ToggleRow("📳", Accents[1], "Haptic feedback", null, s.haptics) { s.haptics = it }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Ghost Mode, Always Offline and the message guard are client-side features. " +
                        "Telegram itself does not offer them, and they only affect what this app sends and shows.",
                    color = Muted, fontSize = 11.5.sp, modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}
