package com.glasgram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF080B14)
private val Glass = Color(0xFF171D2C)
private val Blue = Color(0xFF8BB8FF)
private val Muted = Color(0xFF929DB4)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GlasgramApp() }
    }
}

private data class ChatPreview(
    val name: String,
    val message: String,
    val time: String,
    val initials: String,
    val unread: Int = 0,
    val color: Color
)

private val sampleChats = listOf(
    ChatPreview("Design Circle", "Mira: the new glass pass looks great ✨", "18:42", "DC", 3, Color(0xFF596BC2)),
    ChatPreview("Alex Morgan", "Sent a photo", "18:27", "AM", 0, Color(0xFFBD7899)),
    ChatPreview("Android Builders", "Ravi: CI build is green", "17:56", "AB", 12, Color(0xFF3C9E91)),
    ChatPreview("Saved Messages", "Your notes and saved links", "16:31", "✦", 0, Color(0xFF477CC8)),
    ChatPreview("Product Updates", "New features are on the way", "15:08", "PU", 1, Color(0xFFC38D51)),
    ChatPreview("Neha", "Voice message · 0:18", "Yesterday", "N", 0, Color(0xFF9B75C8))
)

@Composable
private fun GlasgramApp() {
    var selectedTab by remember { mutableStateOf("Chats") }
    var searchOpen by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF121B31), Night, Color(0xFF080A11))))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("GLASGRAM", color = Blue, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            when (selectedTab) {
                                "Calls" -> "Your calls"
                                "Contacts" -> "People"
                                "Settings" -> "Your space"
                                else -> "Messages"
                            },
                            color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    GlassIcon(Icons.Rounded.Search, "Search") { searchOpen = !searchOpen }
                    Spacer(Modifier.size(10.dp))
                    GlassIcon(Icons.Rounded.Edit, "Compose") { selectedTab = "Contacts" }
                }
                AnimatedVisibility(searchOpen) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                            .clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .08f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Muted)
                        Spacer(Modifier.size(10.dp))
                        Text("Search chats and messages", color = Muted, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    FilterPill("All", true) {}
                    FilterPill("Personal", false) {}
                    FilterPill("Groups", false) {}
                    FilterPill("Unread", false) {}
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Archive, null, tint = Muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Archived chats", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.weight(1f))
                    Text("DEMO PREVIEW", color = Blue.copy(alpha = .8f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                }
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sampleChats) { chat -> ChatRow(chat) { selectedTab = "Chats" } }
                    item { Spacer(Modifier.height(12.dp)) }
                }
                BottomGlassBar(selectedTab) { selectedTab = it }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun GlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        color = Color.White.copy(alpha = .08f),
        shape = CircleShape,
        tonalElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (selected) 1f else .75f, label = "pillAlpha")
    Surface(
        modifier = Modifier.alpha(alpha).clip(CircleShape).clickable(onClick = onClick),
        color = if (selected) Blue.copy(alpha = .2f) else Color.White.copy(alpha = .055f),
        shape = CircleShape
    ) {
        Text(
            label,
            color = if (selected) Blue else Muted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun ChatRow(chat: ChatPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
            .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = .075f), Color.White.copy(alpha = .025f))))
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(53.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(chat.color, chat.color.copy(alpha = .55f)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(chat.initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.size(13.dp))
        Column(Modifier.weight(1f)) {
            Text(chat.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(chat.message, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.size(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(chat.time, color = Muted, fontSize = 10.sp)
            if (chat.unread > 0) {
                Box(
                    modifier = Modifier.clip(CircleShape).background(Blue).padding(horizontal = 7.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (chat.unread > 9) "9+" else chat.unread.toString(), color = Night, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BottomGlassBar(selected: String, onSelect: (String) -> Unit) {
    Surface(
        color = Color.White.copy(alpha = .075f),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomItem("Chats", Icons.Rounded.Star, selected == "Chats") { onSelect("Chats") }
            BottomItem("Contacts", Icons.Rounded.Groups, selected == "Contacts") { onSelect("Contacts") }
            BottomItem("Calls", Icons.Rounded.Edit, selected == "Calls") { onSelect("Calls") }
            BottomItem("Settings", Icons.Rounded.Settings, selected == "Settings") { onSelect("Settings") }
        }
    }
}

@Composable
private fun BottomItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) Blue else Muted, modifier = Modifier.size(20.dp))
        Text(label, color = if (selected) Blue else Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
