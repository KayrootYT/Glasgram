package com.glasgram.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glasgram.app.data.Stage
import com.glasgram.app.data.TgRepository

@Composable
fun AuthScreen(repo: TgRepository) {
    val a = accent()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    GlassBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("GLASGRAM", color = a, fontSize = 12.sp, letterSpacing = 4.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "Telegram,\nreimagined.",
                color = Color.White, fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(repo.status, color = Muted, fontSize = 15.sp)
            Spacer(Modifier.height(28.dp))

            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(30.dp), boost = 1.2f)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (repo.stage) {
                    Stage.Phone -> {
                        GlassTextField(phone, { phone = it }, "Phone number with country code", keyboardType = KeyboardType.Phone)
                        GlassButton(if (repo.busy) "Please wait…" else "Send code", enabled = phone.isNotBlank() && !repo.busy) {
                            repo.submitPhone(phone)
                        }
                    }
                    Stage.Code -> {
                        GlassTextField(code, { code = it }, "Login code", keyboardType = KeyboardType.Number)
                        GlassButton(if (repo.busy) "Please wait…" else "Verify code", enabled = code.isNotBlank() && !repo.busy) {
                            repo.submitCode(code)
                        }
                    }
                    Stage.Password -> {
                        GlassTextField(password, { password = it }, "Two-step verification password", password = true)
                        GlassButton(if (repo.busy) "Please wait…" else "Unlock", enabled = password.isNotBlank() && !repo.busy) {
                            repo.submitPassword(password)
                        }
                    }
                    Stage.NoCredentials, Stage.EngineError, Stage.Unsupported -> {
                        Text("Can't continue", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                        GlassButton("Try again", primary = false) { repo.retry() }
                    }
                    else -> {
                        CircularProgressIndicator(color = a, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            }

            if (repo.error.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(repo.error, color = Danger, fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Sign-in is handled by Telegram's official TDLib. Never share your login code or password with anyone.",
                color = Muted, fontSize = 11.sp
            )
        }
    }
}
