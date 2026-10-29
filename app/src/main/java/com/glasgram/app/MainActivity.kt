package com.glasgram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.data.telegram.TdLibGateway
import kotlinx.coroutines.flow.collect
import org.drinkless.tdlib.TdApi

private val Night = Color(0xFF080B14)
private val Glass = Color(0xFF171D2C)
private val Blue = Color(0xFF8BB8FF)
private val Muted = Color(0xFF929DB4)

private data class LiveChat(val id: Long, val title: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GlasgramApp() }
    }
}

@androidx.compose.runtime.Composable
private fun GlasgramApp() {
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf("credentials") }
    var status by remember { mutableStateOf("Connect your Telegram account") }
    var error by remember { mutableStateOf("") }
    var gateway by remember { mutableStateOf<TdLibGateway?>(null) }
    val chats = remember { mutableStateListOf<LiveChat>() }

    LaunchedEffect(gateway) {
        val active = gateway ?: return@LaunchedEffect
        active.updates.collect { update ->
            when (update) {
                is TdApi.UpdateAuthorizationState -> {
                    when (val auth = update.authorizationState) {
                        is TdApi.AuthorizationStateWaitTdlibParameters -> status = "Preparing secure Telegram session…"
                        is TdApi.AuthorizationStateWaitPhoneNumber -> { stage = "phone"; status = "Enter your Telegram phone number" }
                        is TdApi.AuthorizationStateWaitCode -> { stage = "code"; status = "Enter the login code sent by Telegram" }
                        is TdApi.AuthorizationStateWaitPassword -> { stage = "password"; status = "Two-step verification password required" }
                        is TdApi.AuthorizationStateReady -> {
                            stage = "chats"; status = "Connected to Telegram"; error = ""
                            active.send(TdApi.GetChats(TdApi.ChatListMain(), 100)) { result ->
                                if (result is TdApi.Chats) result.chatIds.forEach { chatId ->
                                    active.send(TdApi.GetChat(chatId)) { chatResult ->
                                        if (chatResult is TdApi.Chat && chats.none { it.id == chatResult.id }) {
                                            chats.add(LiveChat(chatResult.id, chatResult.title))
                                        }
                                    }
                                }
                            }
                        }
                        is TdApi.AuthorizationStateLoggingOut -> { stage = "loading"; status = "Signing out…" }
                        is TdApi.AuthorizationStateClosing -> { stage = "loading"; status = "Closing session…" }
                        is TdApi.AuthorizationStateClosed -> { stage = "credentials"; status = "Session closed" }
                        else -> Unit
                    }
                }
                is TdApi.UpdateNewChat -> {
                    val chat = update.chat
                    if (chats.none { it.id == chat.id }) chats.add(LiveChat(chat.id, chat.title))
                }
                is TdApi.UpdateChatTitle -> {
                    val index = chats.indexOfFirst { it.id == update.chatId }
                    if (index >= 0) chats[index] = chats[index].copy(title = update.title)
                }
                else -> Unit
            }
        }
    }

    LaunchedEffect(gateway) {
        gateway?.errors?.collect { error = it }
    }

    MaterialTheme {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF121B31), Night, Color(0xFF080A11)))
            )
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                Text("GLASGRAM", color = Blue, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (stage == "chats") "Your messages" else "Telegram, reimagined.",
                    color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold
                )
                Text(status, color = Muted, fontSize = 14.sp)
                if (error.isNotBlank()) {
                    Surface(color = Color(0xFF48252D), shape = RoundedCornerShape(14.dp)) {
                        Text(error, color = Color(0xFFFFC3CC), modifier = Modifier.padding(12.dp), fontSize = 12.sp)
                    }
                }
                when (stage) {
                    "credentials" -> {
                        Text("Telegram API credentials", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Create your own API ID and hash at my.telegram.org. They stay on this device and are not committed to the repository.", color = Muted, fontSize = 12.sp)
                        GlassField(apiId, { apiId = it }, "API ID", KeyboardType.Number)
                        GlassField(apiHash, { apiHash = it }, "API hash")
                        PrimaryAction("Continue securely") {
                            val id = apiId.toIntOrNull()
                            if (id == null || apiHash.isBlank()) {
                                error = "Enter a valid API ID and API hash from my.telegram.org."
                            } else {
                                try {
                                    gateway = TdLibGateway.create(GlasgramApplicationContext.app, id, apiHash.trim())
                                    stage = "loading"
                                    status = "Starting Telegram engine…"
                                    error = ""
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not start Telegram engine."
                                }
                            }
                        }
                    }
                    "phone" -> {
                        GlassField(phone, { phone = it }, "Phone number, including country code", KeyboardType.Phone)
                        PrimaryAction("Send login code") {
                            if (phone.isBlank()) error = "Enter the phone number linked to Telegram."
                            else { gateway?.submitPhoneNumber(phone.trim()); stage = "loading"; status = "Requesting Telegram login code…"; error = "" }
                        }
                    }
                    "code" -> {
                        GlassField(code, { code = it }, "Telegram login code", KeyboardType.Number)
                        PrimaryAction("Verify code") {
                            if (code.isBlank()) error = "Enter the code Telegram sent you."
                            else { gateway?.submitCode(code.trim()); stage = "loading"; status = "Verifying login code…"; error = "" }
                        }
                    }
                    "password" -> {
                        OutlinedTextField(
                            value = password, onValueChange = { password = it }, label = { Text("Two-step verification password") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        PrimaryAction("Verify password") {
                            if (password.isBlank()) error = "Enter your Telegram two-step verification password."
                            else { gateway?.submitPassword(password); stage = "loading"; status = "Verifying password…"; error = "" }
                        }
                    }
                    "loading" -> {
                        CircularProgressIndicator(color = Blue)
                        Text("Waiting for Telegram…", color = Muted, fontSize = 13.sp)
                    }
                    "chats" -> {
                        if (chats.isEmpty()) {
                            Text("Syncing your Telegram chats…", color = Muted)
                            CircularProgressIndicator(color = Blue)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(chats, key = { it.id }) { chat ->
                                    Row(
                                        Modifier.fillMaxWidth().background(Glass, RoundedCornerShape(18.dp))
                                            .clickable { status = "Selected: ${chat.title} · chat ID ${chat.id}" }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(42.dp).background(Color(0xFF293A5A), CircleShape), contentAlignment = Alignment.Center) {
                                            Text(chat.title.take(1).uppercase(), color = Blue, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.size(12.dp))
                                        Column {
                                            Text(chat.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text("Telegram chat · live data", color = Muted, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text("Glasgram uses Telegram's TDLib client engine. Login codes and passwords are handled locally by TDLib.", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@androidx.compose.runtime.Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = Night)
    ) { Text(label, fontWeight = FontWeight.SemiBold) }
}
