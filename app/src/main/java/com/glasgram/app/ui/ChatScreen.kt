package com.glasgram.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.data.ChatKind
import com.glasgram.app.data.Msg
import com.glasgram.app.data.TgRepository
import kotlin.math.abs

@Composable
fun ChatScreen(repo: TgRepository, chatId: Long, onBack: () -> Unit) {
    val s = LocalGlas.current
    val a = accent()
    val haptic = rememberHaptic()
    val clipboard = LocalClipboardManager.current
    val chat = repo.chats[chatId]
    val msgs = repo.messages[chatId] ?: emptyList()
    val byId = remember(msgs) { msgs.associateBy { it.id } }
    val readUpTo = repo.readOutbox[chatId] ?: 0L
    val isGroup = chat?.kind == ChatKind.Group || chat?.kind == ChatKind.Channel

    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<Msg?>(null) }
    var editing by remember { mutableStateOf<Msg?>(null) }
    var menuMsg by remember { mutableStateOf<Msg?>(null) }
    val listState = rememberLazyListState()

    DisposableEffect(chatId) {
        repo.openChat(chatId)
        onDispose { repo.closeChat(chatId) }
    }

    val newestId = msgs.firstOrNull()?.id
    LaunchedEffect(newestId) {
        if (listState.firstVisibleItemIndex <= 2) listState.animateScrollToItem(0)
    }
    LaunchedEffect(listState, chatId) {
        snapshotFlow {
            (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) to listState.layoutInfo.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 6) repo.loadOlder(chatId)
        }
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            // ── top bar ──
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(28.dp), boost = 1.3f)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Avatar(chat?.title ?: "…", chatId, 40.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        chat?.title ?: "Chat", color = Color.White, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    val kind = when (chat?.kind) {
                        ChatKind.Group -> "Group"
                        ChatKind.Channel -> "Channel"
                        ChatKind.Secret -> "Secret chat"
                        else -> "Private chat"
                    }
                    Text(
                        if (s.showIds) "$kind · ID $chatId" else kind,
                        color = Muted, fontSize = 12.sp, maxLines = 1
                    )
                }
                if (s.ghostRead) {
                    val unread = (chat?.unread ?: 0) > 0
                    Text(
                        if (unread) "👻 Mark read" else "👻",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .glass(CircleShape, boost = 2.2f, tint = a)
                            .clickable(enabled = unread) { repo.markReadNow(chatId); haptic() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // ── messages ──
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    itemsIndexed(msgs, key = { _, m -> m.id }) { i, m ->
                        val older = msgs.getOrNull(i + 1)
                        Column(Modifier.fillMaxWidth()) {
                            if (older == null || dayKey(older.date) != dayKey(m.date)) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        dayLabel(m.date), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .glass(CircleShape)
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            if (isGroup && !m.outgoing && !m.senderIsChat) {
                                LaunchedEffect(m.senderId) { repo.requestUser(m.senderId) }
                            }
                            Bubble(
                                m = m,
                                reply = byId[m.replyToId],
                                showName = isGroup && !m.outgoing,
                                name = repo.senderName(m),
                                read = m.outgoing && m.id <= readUpTo,
                                onReply = { replyTo = it; editing = null },
                                onMenu = { menuMsg = it }
                            )
                        }
                    }
                }
                if (msgs.isEmpty()) {
                    Text("No messages yet", color = Muted, modifier = Modifier.align(Alignment.Center))
                }
            }

            // ── composer ──
            Column(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                val banner = editing ?: replyTo
                if (banner != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .glass(RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(3.dp).height(30.dp).background(a, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (editing != null) "Editing message" else "Reply to ${repo.senderName(banner)}",
                                color = a, fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                            Text(banner.text, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(
                            Icons.Rounded.Close, contentDescription = "Cancel", tint = Muted,
                            modifier = Modifier.size(22.dp).clickable {
                                if (editing != null) input = ""
                                editing = null; replyTo = null
                            }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(
                        Modifier
                            .weight(1f)
                            .glass(RoundedCornerShape(26.dp), boost = 1.2f)
                            .padding(horizontal = 18.dp, vertical = 13.dp)
                    ) {
                        if (input.isEmpty()) Text("Message", color = Muted, fontSize = 16.sp)
                        BasicTextField(
                            value = input,
                            onValueChange = { input = it },
                            maxLines = 6,
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(a),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    val canSend = input.isNotBlank()
                    Box(
                        Modifier
                            .size(48.dp)
                            .glass(CircleShape, boost = if (canSend) 3f else 1f, tint = if (canSend) a else Color.White)
                            .clickable(enabled = canSend) {
                                val text = input.trim()
                                val e = editing
                                if (e != null) repo.editMessage(chatId, e.id, text)
                                else repo.sendText(chatId, text, replyTo?.id ?: 0L)
                                input = ""; editing = null; replyTo = null
                                haptic()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send, contentDescription = "Send",
                            tint = if (canSend) Color.White else Muted, modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // ── message actions ──
        val menu = menuMsg
        if (menu != null) {
            GlassSheet(title = menu.text.replace('\n', ' ').take(60), onDismiss = { menuMsg = null }) {
                SheetAction("↩︎", "Reply") { replyTo = menu; editing = null; menuMsg = null }
                SheetAction("📋", "Copy text") { clipboard.setText(AnnotatedString(menu.text)); menuMsg = null }
                if (menu.outgoing && !menu.deleted) {
                    SheetAction("✏️", "Edit") { editing = menu; replyTo = null; input = menu.text; menuMsg = null }
                }
                if (!menu.deleted) {
                    SheetAction("🗑", "Delete for me", danger = true) { repo.deleteMessage(chatId, menu.id, false); menuMsg = null }
                    SheetAction("🗑", "Delete for everyone", danger = true) { repo.deleteMessage(chatId, menu.id, true); menuMsg = null }
                }
                if (s.showIds) {
                    Text("Message ID ${menu.id}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Bubble(
    m: Msg,
    reply: Msg?,
    showName: Boolean,
    name: String,
    read: Boolean,
    onReply: (Msg) -> Unit,
    onMenu: (Msg) -> Unit
) {
    val s = LocalGlas.current
    val a = accent()
    val haptic = rememberHaptic()
    val r = s.bubbleRadius.dp
    val shape = RoundedCornerShape(
        topStart = r, topEnd = r,
        bottomStart = if (m.outgoing) r else 6.dp,
        bottomEnd = if (m.outgoing) 6.dp else r
    )
    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .align(if (m.outgoing) Alignment.CenterEnd else Alignment.CenterStart)
                .widthIn(max = 320.dp)
                .glass(shape, boost = if (m.outgoing) 2.4f else 1.1f, tint = if (m.outgoing) a else Color.White)
                .pointerInput(m.id, s.doubleTapReply) {
                    detectTapGestures(
                        onDoubleTap = { if (s.doubleTapReply) { haptic(); onReply(m) } },
                        onLongPress = { haptic(); onMenu(m) }
                    )
                }
                .padding(horizontal = 13.dp, vertical = 8.dp)
        ) {
            if (showName) {
                Text(
                    name, color = Accents[(abs(m.senderId) % Accents.size).toInt()],
                    fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
            }
            if (m.replyToId != 0L) {
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(3.dp).height(26.dp).background(Color.White.copy(alpha = 0.6f), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        reply?.text?.replace('\n', ' ') ?: "↩︎ Reply",
                        color = Color.White.copy(alpha = 0.7f), fontSize = 12.5.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            }
            Text(
                m.text,
                color = if (m.deleted) Color.White.copy(alpha = 0.75f) else Color.White,
                fontSize = (16f * s.textScale).sp,
                textDecoration = if (m.deleted) TextDecoration.LineThrough else null
            )
            if (m.deleted) {
                Text("🗑 Deleted — kept by Glasgram", color = Danger, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            }
            val original = m.originalText
            if (original != null) {
                Text(
                    "Original: " + original.replace('\n', ' ').take(140),
                    color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontStyle = FontStyle.Italic
                )
            }
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                if (m.edited) Text("edited  ", color = Color.White.copy(alpha = 0.55f), fontSize = 10.5.sp)
                Text(formatClock(m.date, s.use24h), color = Color.White.copy(alpha = 0.55f), fontSize = 10.5.sp)
                if (m.outgoing) {
                    Text(
                        when {
                            m.sending -> "  🕓"
                            read -> "  ✓✓"
                            else -> "  ✓"
                        },
                        color = if (read) Color.White else Color.White.copy(alpha = 0.55f),
                        fontSize = 10.5.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            if (s.showIds) Text("#${m.id}", color = Color.White.copy(alpha = 0.4f), fontSize = 9.sp)
        }
    }
}
