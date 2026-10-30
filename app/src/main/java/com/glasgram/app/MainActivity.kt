package com.glasgram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.data.Stage
import com.glasgram.app.data.TgRepository
import com.glasgram.app.ui.AuthScreen
import com.glasgram.app.ui.ChatScreen
import com.glasgram.app.ui.ChatsScreen
import com.glasgram.app.ui.GlassBackground
import com.glasgram.app.ui.GlassTabBar
import com.glasgram.app.ui.GlasgramSettingsScreen
import com.glasgram.app.ui.LocalGlas
import com.glasgram.app.ui.SettingsScreen
import com.glasgram.app.ui.TabItem
import com.glasgram.app.ui.glass
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var repo: TgRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        repo = TgRepository.get(applicationContext)
        repo.start()
        setContent {
            CompositionLocalProvider(LocalGlas provides repo.settings) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    GlasgramRoot(repo)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        repo.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        repo.setForeground(false)
    }
}

@Composable
private fun GlasgramRoot(repo: TgRepository) {
    LaunchedEffect(repo.settings.alwaysOffline) { repo.applyOnline() }

    Box(Modifier.fillMaxSize()) {
        if (repo.stage == Stage.Ready) MainShell(repo) else AuthScreen(repo)

        val notice = repo.notice
        if (notice != null) {
            LaunchedEffect(notice) {
                delay(4000)
                repo.notice = null
            }
            if (repo.stage == Stage.Ready) {
                Text(
                    notice,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .glass(RoundedCornerShape(20.dp), boost = 2f, tint = Color(0xFFFF6B7A))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun MainShell(repo: TgRepository) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openChat by rememberSaveable { mutableStateOf<Long?>(null) }
    var lastChat by rememberSaveable { mutableLongStateOf(0L) }
    var showGlas by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showGlas) { showGlas = false }
    BackHandler(enabled = openChat != null) { openChat = null }

    val tabs = listOf(
        TabItem(Icons.Rounded.ChatBubble, "Chats"),
        TabItem(Icons.Rounded.Settings, "Settings")
    )

    Box(Modifier.fillMaxSize()) {
        GlassBackground {
            when (tab) {
                0 -> ChatsScreen(repo, onOpen = { id -> lastChat = id; openChat = id })
                else -> SettingsScreen(repo, onOpenGlasgram = { showGlas = true })
            }
            GlassTabBar(
                items = tabs,
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            )
        }

        AnimatedVisibility(
            visible = openChat != null,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            ChatScreen(repo, lastChat, onBack = { openChat = null })
        }

        AnimatedVisibility(
            visible = showGlas,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            GlasgramSettingsScreen(repo, onBack = { showGlas = false })
        }
    }
}
