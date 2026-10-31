package com.glasgram.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.data.ChatInfo
import com.glasgram.app.data.ChatKind
import com.glasgram.app.data.TgRepository

private val FilterNames = listOf("All", "Unread", "Personal", "Groups", "Channels")

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatsScreen(repo: TgRepository, onOpen: (Long) -> Unit) {
    val s = LocalGlas.current
    val a = accent()
    val haptic = rememberHaptic()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var menuChat by remember { mutableStateOf<ChatInfo?>(null) }
    var showAddFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val customFolders = s.folders()

    val all = repo.sortedChats()
    val unreadChats = all.count { it.unread > 0 || it.markedUnread }
    val shown = all.filter { c ->
        val okFilter = when (filter) {
            1 -> c.unread > 0 || c.markedUnread
            2 -> c.kind == ChatKind.Private || c.kind == ChatKind.Secret
            3 -> c.kind == ChatKind.Group
            4 -> c.kind == ChatKind.Channel
            in 5..(4 + customFolders.size) -> s.folderFor(c.id) == customFolders[filter - 5]
            else -> true
        }
        okFilter && (query.isBlank() || c.title.contains(query.trim(), ignoreCase = true))
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Large title + quick mod toggles
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, end = 16.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("GLASGRAM", color = a, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                    Text("Chats", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                GlassCircleButton(active = s.ghostRead, onClick = { s.ghostRead = !s.ghostRead; haptic() }) {
                    Text("👻", fontSize = 18.sp)
                }
                Spacer(Modifier.width(8.dp))
                GlassCircleButton(active = s.alwaysOffline, onClick = { s.alwaysOffline = !s.alwaysOffline; haptic() }) {
                    Text("🌙", fontSize = 18.sp)
                }
            }

            // Search
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search chats", color = Muted, fontSize = 16.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(a),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close, contentDescription = "Clear", tint = Muted,
                        modifier = Modifier.size(20.dp).clickable { query = "" }
                    )
                }
            }

            // Folder chips
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(FilterNames + customFolders) { i, name ->
                    val selected = i == filter
                    val label = if (i == 1 && unreadChats > 0) "$name · $unreadChats" else name
                    Text(
                        label,
                        color = if (selected) Color.White else Muted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .glass(CircleShape, boost = if (selected) 2.2f else 0.8f, tint = if (selected) a else Color.White)
                            .clickable { filter = i; haptic() }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
                item {
                    Text("+ Folder", color = a, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.glass(CircleShape, tint = a).clickable {
                            newFolderName = ""; showAddFolder = true
                        }.padding(horizontal = 16.dp, vertical = 9.dp))
                }
            }

            if (all.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = a)
                    Spacer(Modifier.height(14.dp))
                    Text("Syncing your chats…", color = Muted)
                }
            } else if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing here", color = Muted, fontSize = 15.sp)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shown, key = { it.id }) { c ->
                        ChatRow(
                            c = c,
                            compact = s.compactList,
                            showIds = s.showIds,
                            h24 = s.use24h,
                            onClick = { onOpen(c.id) },
                            onLongClick = { haptic(); menuChat = c }
                        )
                    }
                }
            }
        }

        val menu = menuChat
        if (menu != null) {
            GlassSheet(title = menu.title, onDismiss = { menuChat = null }) {
                SheetAction("📌", if (menu.pinned) "Unpin" else "Pin to top") { repo.togglePin(menu); menuChat = null }
                SheetAction(if (menu.muted) "🔔" else "🔇", if (menu.muted) "Unmute" else "Mute") { repo.toggleMute(menu); menuChat = null }
                SheetAction("✓", "Mark as read") { repo.markReadNow(menu.id); menuChat = null }
                customFolders.forEach { folder ->
                    SheetAction("📁", "Move to $folder") { s.assignFolder(menu.id, folder); menuChat = null }
                }
                if (s.folderFor(menu.id) != null) {
                    SheetAction("↩", "Remove from custom folder") { s.assignFolder(menu.id, null); menuChat = null }
                }
            }
        }
        if (showAddFolder) {
            GlassSheet(title = "Create chat folder", onDismiss = { showAddFolder = false }) {
                GlassTextField(newFolderName, { newFolderName = it }, "Folder name")
                SheetAction("＋", "Create folder") {
                    if (s.addFolder(newFolderName)) { filter = 5 + s.folders().lastIndex; showAddFolder = false }
                }
                customFolders.forEach { folder ->
                    SheetAction("🗑", "Delete $folder", danger = true) { s.removeFolder(folder); filter = 0 }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatRow(
    c: ChatInfo,
    compact: Boolean,
    showIds: Boolean,
    h24: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val a = accent()
    val unread = c.unread > 0 || c.markedUnread
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(24.dp), boost = if (unread) 1.35f else 1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = if (compact) 9.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(c.title, c.id, if (compact) 44.dp else 54.dp)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.title, color = Color.White, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                if (c.muted) Text(" 🔇", fontSize = 11.sp)
                if (c.pinned) Text(" 📌", fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    formatListTime(c.lastDate, h24),
                    color = if (unread && !c.muted) a else Muted, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = (if (c.lastOutgoing) "You: " else "") + c.lastText.replace('\n', ' ')
                Text(
                    preview.ifBlank { "No messages yet" },
                    color = Muted, fontSize = 14.sp,
                    maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                            .clip(CircleShape)
                            .background(if (c.muted) Color(0xFF5B6479) else a)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (c.unread > 99) "99+" else if (c.unread > 0) c.unread.toString() else "•",
                            color = if (c.muted) Color.White else Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (showIds) {
                Text("ID ${c.id}", color = Muted.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}
