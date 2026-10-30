package com.glasgram.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.glasgram.app.BuildConfig
import com.glasgram.app.data.telegram.TdLibGateway
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

enum class Stage { Boot, NoCredentials, EngineError, Loading, Phone, Code, Password, Unsupported, Ready }
enum class ChatKind { Private, Group, Channel, Secret }

data class ChatInfo(
    val id: Long,
    val title: String,
    val kind: ChatKind,
    val unread: Int,
    val markedUnread: Boolean,
    val lastText: String,
    val lastDate: Int,
    val lastOutgoing: Boolean,
    val lastMsgId: Long,
    val order: Long,
    val pinned: Boolean,
    val muted: Boolean,
    val notif: TdApi.ChatNotificationSettings?
)

data class Msg(
    val id: Long,
    val chatId: Long,
    val outgoing: Boolean,
    val date: Int,
    val text: String,
    val senderId: Long,
    val senderIsChat: Boolean,
    val replyToId: Long,
    val sending: Boolean,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val originalText: String? = null
)

/**
 * Single source of truth for the UI. Lives for the whole process so that rotating the
 * screen never creates a second TDLib client on the same database.
 */
class TgRepository private constructor(
    private val context: Context,
    val settings: GlasSettings
) {
    var stage by mutableStateOf(Stage.Boot)
        private set
    var status by mutableStateOf("Starting…")
        private set
    var error by mutableStateOf("")
    var notice by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    var me by mutableStateOf<TdApi.User?>(null)
        private set

    val chats = mutableStateMapOf<Long, ChatInfo>()
    val messages = mutableStateMapOf<Long, List<Msg>>()
    val readOutbox = mutableStateMapOf<Long, Long>()
    val userNames = mutableStateMapOf<Long, String>()

    @Volatile private var gateway: TdLibGateway? = null
    @Volatile private var foreground = true
    private val lock = Any()
    private val openChats = ConcurrentHashMap.newKeySet<Long>()
    private val busyChats = ConcurrentHashMap.newKeySet<Long>()
    private val endedChats = ConcurrentHashMap.newKeySet<Long>()
    private val selfDeleted = ConcurrentHashMap.newKeySet<Long>()
    private val askedUsers = ConcurrentHashMap.newKeySet<Long>()

    // ───────────────────────── lifecycle ─────────────────────────

    fun start() {
        if (gateway != null) return
        val id = BuildConfig.TELEGRAM_API_ID
        val hash = BuildConfig.TELEGRAM_API_HASH
        if (id <= 0 || hash.isBlank()) {
            stage = Stage.NoCredentials
            status = "Missing app credentials"
            error = "This build has no Telegram API credentials. Add TELEGRAM_API_ID and TELEGRAM_API_HASH as GitHub Actions secrets and rebuild."
            return
        }
        stage = Stage.Loading
        status = "Starting Telegram engine…"
        error = ""
        try {
            gateway = TdLibGateway.create(
                context, id, hash,
                onUpdate = { g, update -> onUpdate(g, update) },
                onError = { message -> onError(message) }
            )
        } catch (e: Throwable) {
            stage = Stage.EngineError
            status = "Engine failed to start"
            error = "Telegram engine could not start (${e.javaClass.simpleName}): ${e.message ?: "no details"}"
        }
    }

    fun retry() {
        gateway = null
        start()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        applyOnline()
    }

    /** "online" is only ever sent when the app is visible AND Always-Offline is off. */
    fun applyOnline() {
        val g = gateway ?: return
        if (stage != Stage.Ready) return
        g.send(TdApi.SetOption("online", TdApi.OptionValueBoolean(foreground && !settings.alwaysOffline)))
    }

    fun logout() {
        gateway?.send(TdApi.LogOut())
    }

    // ───────────────────────── auth ─────────────────────────

    fun submitPhone(phone: String) {
        busy = true; error = ""
        gateway?.submitPhoneNumber(phone.trim())
    }

    fun submitCode(code: String) {
        busy = true; error = ""
        gateway?.submitCode(code.trim())
    }

    fun submitPassword(password: String) {
        busy = true; error = ""
        gateway?.submitPassword(password)
    }

    private fun onError(message: String) {
        error = message
        notice = message
        busy = false
    }

    private fun onAuth(g: TdLibGateway, s: TdApi.AuthorizationState) {
        busy = false
        when (s) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> { stage = Stage.Loading; status = "Preparing secure session…" }
            is TdApi.AuthorizationStateWaitPhoneNumber -> { stage = Stage.Phone; status = "Enter your Telegram phone number" }
            is TdApi.AuthorizationStateWaitCode -> { stage = Stage.Code; status = "Enter the code Telegram sent you" }
            is TdApi.AuthorizationStateWaitPassword -> { stage = Stage.Password; status = "Two-step verification password" }
            is TdApi.AuthorizationStateReady -> { stage = Stage.Ready; status = "Connected"; error = ""; onReady(g) }
            is TdApi.AuthorizationStateLoggingOut, is TdApi.AuthorizationStateClosing -> { stage = Stage.Loading; status = "Signing out…" }
            is TdApi.AuthorizationStateClosed -> onClosed()
            else -> {
                stage = Stage.Unsupported
                status = "Unsupported login step"
                error = "This login step is not supported yet. Sign in once with the official Telegram app, then try again."
            }
        }
    }

    private fun onClosed() {
        gateway = null
        chats.clear(); messages.clear(); readOutbox.clear(); userNames.clear()
        openChats.clear(); busyChats.clear(); endedChats.clear(); askedUsers.clear()
        me = null
        stage = Stage.Boot
        Thread { start() }.start()
    }

    private fun onReady(g: TdLibGateway) {
        g.send(TdApi.GetMe()) { r ->
            if (r is TdApi.User) {
                me = r
                userNames[r.id] = fullName(r)
            }
        }
        g.send(TdApi.GetChats(TdApi.ChatListMain(), 200)) { r ->
            if (r is TdApi.Chats) {
                r.chatIds.forEach { id ->
                    if (!chats.containsKey(id)) {
                        g.send(TdApi.GetChat(id)) { c -> if (c is TdApi.Chat) upsertChat(c) }
                    }
                }
            }
        }
        applyOnline()
    }

    // ───────────────────────── updates ─────────────────────────

    private fun onUpdate(g: TdLibGateway, u: TdApi.Object) {
        if (gateway == null) gateway = g
        when (u) {
            is TdApi.UpdateAuthorizationState -> onAuth(g, u.authorizationState)
            is TdApi.UpdateNewChat -> upsertChat(u.chat)
            is TdApi.UpdateChatTitle -> patchChat(u.chatId) { it.copy(title = u.title) }
            is TdApi.UpdateChatLastMessage -> {
                val lm = u.lastMessage
                val p = u.positions.firstOrNull { it.list is TdApi.ChatListMain }
                patchChat(u.chatId) { c ->
                    val base = if (lm != null) {
                        c.copy(lastText = describe(lm.content), lastDate = lm.date, lastOutgoing = lm.isOutgoing, lastMsgId = lm.id)
                    } else {
                        c.copy(lastText = "", lastDate = 0, lastOutgoing = false, lastMsgId = 0L)
                    }
                    base.copy(order = p?.order ?: 0L, pinned = p?.isPinned ?: false)
                }
            }
            is TdApi.UpdateChatPosition -> {
                if (u.position.list is TdApi.ChatListMain) {
                    patchChat(u.chatId) { it.copy(order = u.position.order, pinned = u.position.isPinned) }
                }
            }
            is TdApi.UpdateChatReadInbox -> patchChat(u.chatId) { it.copy(unread = u.unreadCount) }
            is TdApi.UpdateChatReadOutbox -> readOutbox[u.chatId] = u.lastReadOutboxMessageId
            is TdApi.UpdateChatIsMarkedAsUnread -> patchChat(u.chatId) { it.copy(markedUnread = u.isMarkedAsUnread) }
            is TdApi.UpdateChatNotificationSettings -> patchChat(u.chatId) {
                it.copy(muted = u.notificationSettings.muteFor > 0, notif = u.notificationSettings)
            }
            is TdApi.UpdateUser -> userNames[u.user.id] = fullName(u.user)
            is TdApi.UpdateNewMessage -> {
                val m = toMsg(u.message)
                mutateMessages(m.chatId) { mergeMsgs(it, listOf(m)) }
                if (!m.outgoing && openChats.contains(m.chatId)) markRead(m.chatId)
            }
            is TdApi.UpdateMessageSendSucceeded -> {
                val m = toMsg(u.message)
                val oldId = u.oldMessageId
                mutateMessages(m.chatId) { list -> mergeMsgs(list.filter { it.id != oldId }, listOf(m)) }
            }
            is TdApi.UpdateMessageSendFailed -> notice = "A message could not be sent"
            is TdApi.UpdateMessageContent -> {
                val newText = describe(u.newContent)
                val messageId = u.messageId
                mutateMessages(u.chatId) { list ->
                    list.map { m ->
                        if (m.id == messageId && m.text != newText) {
                            m.copy(
                                text = newText,
                                edited = true,
                                originalText = if (settings.keepEdits) (m.originalText ?: m.text) else null
                            )
                        } else m
                    }
                }
                patchChat(u.chatId) { c -> if (c.lastMsgId == messageId) c.copy(lastText = newText) else c }
            }
            is TdApi.UpdateDeleteMessages -> {
                if (!u.fromCache) {
                    val ids = u.messageIds.toSet()
                    val keep = u.isPermanent && settings.keepDeleted
                    mutateMessages(u.chatId) { list ->
                        list.mapNotNull { m ->
                            when {
                                m.id !in ids -> m
                                keep && m.id !in selfDeleted -> m.copy(deleted = true)
                                else -> null
                            }
                        }
                    }
                    selfDeleted.removeAll(ids)
                }
            }
            else -> Unit
        }
    }

    private fun upsertChat(chat: TdApi.Chat) {
        val p = chat.positions.firstOrNull { it.list is TdApi.ChatListMain }
        val lm = chat.lastMessage
        readOutbox[chat.id] = chat.lastReadOutboxMessageId
        chats[chat.id] = ChatInfo(
            id = chat.id,
            title = chat.title,
            kind = kindOf(chat.type),
            unread = chat.unreadCount,
            markedUnread = chat.isMarkedAsUnread,
            lastText = if (lm != null) describe(lm.content) else "",
            lastDate = lm?.date ?: 0,
            lastOutgoing = lm?.isOutgoing ?: false,
            lastMsgId = lm?.id ?: 0L,
            order = p?.order ?: 0L,
            pinned = p?.isPinned ?: false,
            muted = chat.notificationSettings.muteFor > 0,
            notif = chat.notificationSettings
        )
    }

    private fun patchChat(id: Long, f: (ChatInfo) -> ChatInfo) {
        synchronized(lock) {
            val cur = chats[id] ?: return
            chats[id] = f(cur)
        }
    }

    private fun mutateMessages(chatId: Long, f: (List<Msg>) -> List<Msg>) {
        synchronized(lock) {
            val cur = messages[chatId] ?: return
            messages[chatId] = f(cur)
        }
    }

    private fun mergeMsgs(old: List<Msg>, add: List<Msg>): List<Msg> {
        val map = LinkedHashMap<Long, Msg>()
        old.forEach { map[it.id] = it }
        for (n in add) {
            val o = map[n.id]
            map[n.id] = if (o != null) {
                n.copy(deleted = o.deleted, originalText = o.originalText ?: n.originalText, edited = n.edited || o.edited)
            } else n
        }
        return map.values.sortedByDescending { it.id }
    }

    private fun toMsg(m: TdApi.Message): Msg {
        val s = m.senderId
        val sid: Long
        val isChat: Boolean
        if (s is TdApi.MessageSenderUser) { sid = s.userId; isChat = false }
        else if (s is TdApi.MessageSenderChat) { sid = s.chatId; isChat = true }
        else { sid = 0L; isChat = false }
        val r = m.replyTo
        val replyId = if (r is TdApi.MessageReplyToMessage) r.messageId else 0L
        return Msg(
            id = m.id,
            chatId = m.chatId,
            outgoing = m.isOutgoing,
            date = m.date,
            text = describe(m.content),
            senderId = sid,
            senderIsChat = isChat,
            replyToId = replyId,
            sending = m.sendingState != null,
            edited = m.editDate > 0
        )
    }

    // ───────────────────────── queries for UI ─────────────────────────

    fun sortedChats(): List<ChatInfo> =
        chats.values.filter { it.order > 0L }.sortedByDescending { it.order }

    fun senderName(m: Msg): String =
        if (m.senderIsChat) chats[m.senderId]?.title ?: "Channel" else userNames[m.senderId] ?: "…"

    fun requestUser(id: Long) {
        if (id == 0L || userNames.containsKey(id) || !askedUsers.add(id)) return
        gateway?.send(TdApi.GetUser(id)) { r -> if (r is TdApi.User) userNames[id] = fullName(r) }
    }

    // ───────────────────────── chat actions ─────────────────────────

    fun openChat(id: Long) {
        openChats.add(id)
        gateway?.send(TdApi.OpenChat(id))
        if (!messages.containsKey(id)) {
            messages[id] = emptyList()
            loadHistory(id)
        } else {
            markRead(id)
        }
    }

    fun closeChat(id: Long) {
        openChats.remove(id)
        gateway?.send(TdApi.CloseChat(id))
    }

    private fun loadHistory(chatId: Long) {
        if (!busyChats.add(chatId)) return
        fetchPage(chatId, 0L, 0)
    }

    fun loadOlder(chatId: Long) {
        if (endedChats.contains(chatId) || !busyChats.add(chatId)) return
        val oldest = messages[chatId]?.minOfOrNull { it.id }
        if (oldest == null) { busyChats.remove(chatId); return }
        fetchPage(chatId, oldest, 0)
    }

    private fun fetchPage(chatId: Long, from: Long, attempt: Int) {
        val g = gateway
        if (g == null) { busyChats.remove(chatId); return }
        g.send(TdApi.GetChatHistory(chatId, from, 0, 50, false)) { r ->
            if (r is TdApi.Messages) {
                val list = r.messages.map { toMsg(it) }
                mutateMessages(chatId) { mergeMsgs(it, list) }
                if (list.isEmpty()) {
                    endedChats.add(chatId); busyChats.remove(chatId)
                } else if (list.size < 20 && attempt < 2) {
                    // TDLib may return a short first page while it is still syncing.
                    fetchPage(chatId, list.minOf { it.id }, attempt + 1)
                } else {
                    busyChats.remove(chatId)
                }
                if (from == 0L && openChats.contains(chatId)) markRead(chatId)
            } else {
                busyChats.remove(chatId)
            }
        }
    }

    /** Respects Ghost Mode: when on, nothing is ever reported as read automatically. */
    fun markRead(chatId: Long) {
        if (settings.ghostRead) return
        if ((chats[chatId]?.unread ?: 0) <= 0) return
        markReadNow(chatId)
    }

    /** Explicit "mark as read" (works even in Ghost Mode because the user asked for it). */
    fun markReadNow(chatId: Long) {
        val g = gateway ?: return
        val id = messages[chatId]?.firstOrNull { !it.outgoing && !it.deleted }?.id
            ?: chats[chatId]?.lastMsgId
            ?: return
        if (id == 0L) return
        val req = TdApi.ViewMessages()
        req.chatId = chatId
        req.messageIds = longArrayOf(id)
        req.source = TdApi.MessageSourceChatHistory()
        req.forceRead = true
        g.send(req)
    }

    fun sendText(chatId: Long, text: String, replyTo: Long) {
        val g = gateway ?: return
        val req = TdApi.SendMessage()
        req.chatId = chatId
        req.inputMessageContent = textContent(text)
        if (replyTo != 0L) {
            val r = TdApi.InputMessageReplyToMessage()
            r.messageId = replyTo
            req.replyTo = r
        }
        g.send(req)
    }

    fun editMessage(chatId: Long, messageId: Long, text: String) {
        val g = gateway ?: return
        val req = TdApi.EditMessageText()
        req.chatId = chatId
        req.messageId = messageId
        req.inputMessageContent = textContent(text)
        g.send(req)
    }

    fun deleteMessage(chatId: Long, messageId: Long, forEveryone: Boolean) {
        val g = gateway ?: return
        selfDeleted.add(messageId)
        val req = TdApi.DeleteMessages()
        req.chatId = chatId
        req.messageIds = longArrayOf(messageId)
        req.revoke = forEveryone
        g.send(req)
    }

    fun togglePin(c: ChatInfo) {
        val req = TdApi.ToggleChatIsPinned()
        req.chatList = TdApi.ChatListMain()
        req.chatId = c.id
        req.isPinned = !c.pinned
        gateway?.send(req)
    }

    fun toggleMute(c: ChatInfo) {
        val n = c.notif ?: return
        n.useDefaultMuteFor = false
        n.muteFor = if (c.muted) 0 else 366 * 24 * 3600
        gateway?.send(TdApi.SetChatNotificationSettings(c.id, n))
    }

    private fun textContent(text: String): TdApi.InputMessageText {
        val c = TdApi.InputMessageText()
        c.text = TdApi.FormattedText(text, arrayOf<TdApi.TextEntity>())
        return c
    }

    companion object {
        @Volatile private var instance: TgRepository? = null

        fun get(context: Context): TgRepository =
            instance ?: synchronized(this) {
                instance ?: TgRepository(
                    context.applicationContext,
                    GlasSettings(context.applicationContext)
                ).also { instance = it }
            }
    }
}

private fun kindOf(t: TdApi.ChatType): ChatKind = when (t) {
    is TdApi.ChatTypePrivate -> ChatKind.Private
    is TdApi.ChatTypeSecret -> ChatKind.Secret
    is TdApi.ChatTypeBasicGroup -> ChatKind.Group
    is TdApi.ChatTypeSupergroup -> if (t.isChannel) ChatKind.Channel else ChatKind.Group
    else -> ChatKind.Private
}

private fun fullName(u: TdApi.User): String =
    listOf(u.firstName, u.lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "User" }

private fun describe(c: TdApi.MessageContent): String = when (c) {
    is TdApi.MessageText -> c.text.text
    is TdApi.MessagePhoto -> if (c.caption.text.isBlank()) "📷 Photo" else "📷 " + c.caption.text
    is TdApi.MessageVideo -> if (c.caption.text.isBlank()) "🎬 Video" else "🎬 " + c.caption.text
    is TdApi.MessageAnimation -> "GIF"
    is TdApi.MessageVoiceNote -> "🎤 Voice message"
    is TdApi.MessageVideoNote -> "⏺ Video message"
    is TdApi.MessageAudio -> "🎵 " + c.audio.title.ifBlank { "Audio" }
    is TdApi.MessageDocument -> "📄 " + c.document.fileName.ifBlank { "File" }
    is TdApi.MessageSticker -> c.sticker.emoji + " Sticker"
    is TdApi.MessageLocation -> "📍 Location"
    is TdApi.MessageContact -> "👤 Contact"
    is TdApi.MessagePoll -> "📊 Poll"
    is TdApi.MessageCall -> "📞 Call"
    else -> "Message"
}
