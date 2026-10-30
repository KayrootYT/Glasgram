package com.glasgram.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.data.GlasSettings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ───────────────────────── theme ─────────────────────────

val LocalGlas = staticCompositionLocalOf<GlasSettings> { error("GlasSettings not provided") }

val Accents = listOf(
    Color(0xFF7DB7FF), Color(0xFFB69CFF), Color(0xFF5FE3C0), Color(0xFFFF8FB3), Color(0xFFFFC56E)
)
val AccentNames = listOf("Sky", "Violet", "Mint", "Rose", "Amber")
val Ink = Color(0xFF05070E)
val Muted = Color(0xFFA3ADC2)
val Danger = Color(0xFFFF6B7A)

@Composable
fun accent(): Color {
    val i = LocalGlas.current.accentIndex
    return Accents[i.coerceIn(0, Accents.lastIndex)]
}

@Composable
fun rememberHaptic(): () -> Unit {
    val h = LocalHapticFeedback.current
    val s = LocalGlas.current
    return { if (s.haptics) h.performHapticFeedback(HapticFeedbackType.LongPress) }
}

// ───────────────────────── background ─────────────────────────

private fun DrawScope.orb(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center
    )
}

@Composable
private fun Orbs(accent: Color, animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "orbs")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(tween(45000, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(Modifier.fillMaxSize()) {
        val p = if (animate) phase.value else 0f
        val w = size.width
        val h = size.height
        orb(Offset(w * (0.15f + 0.12f * sin(p)), h * (0.12f + 0.06f * cos(p))), w * 0.80f, accent.copy(alpha = 0.40f))
        orb(Offset(w * (0.95f - 0.12f * cos(p)), h * (0.45f + 0.08f * sin(p))), w * 0.75f, Color(0xFF8A5CFF).copy(alpha = 0.27f))
        orb(Offset(w * (0.25f + 0.10f * sin(p + 2f)), h * (0.92f - 0.05f * cos(p))), w * 0.85f, Color(0xFF2FD4C0).copy(alpha = 0.20f))
    }
}

@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val a = accent()
    val animate = LocalGlas.current.animatedBackground
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1226), Ink, Color(0xFF070912))))
    ) {
        Orbs(a, animate)
        content()
    }
}

// ───────────────────────── glass surface ─────────────────────────

/**
 * Liquid-glass surface: translucent gradient fill, soft specular highlight in the top-left
 * corner and a bright-to-dim rim, like the edge of a piece of glass catching light.
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    boost: Float = 1f,
    tint: Color = Color.White
): Modifier {
    val k = LocalGlas.current.glassIntensity.coerceIn(0f, 1f)
    val top = ((0.07f + 0.13f * k) * boost).coerceIn(0f, 0.9f)
    val bottom = ((0.025f + 0.06f * k) * boost).coerceIn(0f, 0.9f)
    val rim = (0.25f + 0.45f * k).coerceIn(0f, 1f)
    val shine = (0.10f * (0.4f + k)).coerceIn(0f, 1f)
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = top), tint.copy(alpha = bottom))), shape)
        .drawBehind {
            if (size.width > 0f) {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.White.copy(alpha = shine), Color.Transparent),
                        center = Offset(size.width * 0.12f, 0f),
                        radius = size.width * 0.9f
                    )
                )
            }
        }
        .border(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = rim),
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = rim * 0.5f)
                )
            ),
            shape
        )
}

// ───────────────────────── controls ─────────────────────────

@Composable
fun GlassButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val a = accent()
    val shape = RoundedCornerShape(20.dp)
    val fill = if (primary) {
        Modifier.background(Brush.verticalGradient(listOf(a, a.copy(alpha = 0.72f))), shape)
    } else {
        Modifier.glass(shape, boost = 1.2f)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(shape)
            .then(fill)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = when {
                danger -> Danger
                primary -> Ink
                else -> Color.White
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun GlassCircleButton(
    size: Dp = 42.dp,
    active: Boolean = false,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val a = accent()
    Box(
        Modifier
            .size(size)
            .glass(CircleShape, boost = if (active) 2.4f else 1f, tint = if (active) a else Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false
) {
    Box(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        if (value.isEmpty()) Text(placeholder, color = Muted, fontSize = 16.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(accent()),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val a = accent()
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, label = "thumb")
    val track by animateColorAsState(if (checked) a else Color.White.copy(alpha = 0.16f), label = "track")
    Box(
        Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
            .clickable { onChange(!checked) }
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun Avatar(title: String, id: Long, size: Dp) {
    val palettes = listOf(
        listOf(Color(0xFF6AA8FF), Color(0xFF3B5BDB)),
        listOf(Color(0xFFB69CFF), Color(0xFF6C3FD1)),
        listOf(Color(0xFF5FE3C0), Color(0xFF14917A)),
        listOf(Color(0xFFFF8FB3), Color(0xFFC2306A)),
        listOf(Color(0xFFFFC56E), Color(0xFFD9822B)),
        listOf(Color(0xFF7BE0FF), Color(0xFF1B7FB5))
    )
    val p = palettes[(abs(id) % palettes.size).toInt()]
    val initials = title.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(p))
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
    }
}

data class TabItem(val icon: ImageVector, val label: String)

@Composable
fun GlassTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val a = accent()
    Row(
        modifier
            .glass(RoundedCornerShape(34.dp), boost = 1.5f)
            .background(Color(0x66080C18))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, item ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) Color.White.copy(alpha = 0.16f) else Color.Transparent, label = "tab")
            Row(
                Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(item.icon, contentDescription = item.label, tint = if (sel) a else Muted, modifier = Modifier.size(22.dp))
                if (sel) Text(item.label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun GlassSheet(title: String?, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val shape = RoundedCornerShape(32.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(visible = shown, enter = slideInVertically { it / 2 } + fadeIn()) {
            Column(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(12.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(0xF2101728))
                    .glass(shape, boost = 1.2f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .padding(vertical = 10.dp)
            ) {
                if (title != null) {
                    Text(
                        title, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun SheetAction(emoji: String, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (danger) Danger else Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

// ───────────────────────── time helpers ─────────────────────────

private fun zoned(date: Int) = Instant.ofEpochSecond(date.toLong()).atZone(ZoneId.systemDefault())

fun formatClock(date: Int, h24: Boolean): String =
    DateTimeFormatter.ofPattern(if (h24) "HH:mm" else "h:mm a").format(zoned(date))

fun formatListTime(date: Int, h24: Boolean): String {
    if (date <= 0) return ""
    val d = zoned(date)
    val days = ChronoUnit.DAYS.between(d.toLocalDate(), LocalDate.now())
    return when {
        days <= 0L -> formatClock(date, h24)
        days < 7L -> d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
        else -> DateTimeFormatter.ofPattern("dd/MM/yy").format(d)
    }
}

fun dayKey(date: Int): Long = zoned(date).toLocalDate().toEpochDay()

fun dayLabel(date: Int): String {
    val d = zoned(date)
    val days = ChronoUnit.DAYS.between(d.toLocalDate(), LocalDate.now())
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        else -> DateTimeFormatter.ofPattern("d MMM yyyy").format(d)
    }
}
