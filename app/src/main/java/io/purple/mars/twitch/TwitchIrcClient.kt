package io.purple.mars.twitch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

data class TwitchEmoteRef(val id: String, val start: Int, val end: Int)

data class ChatMessage(
    val username: String,
    val message: String,
    val colorHex: String? = null,
    val twitchEmotes: List<TwitchEmoteRef> = emptyList()
)

class TwitchIrcClient {
    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    fun connect(channel: String) {
        disconnect()
        val anonNick = "justinfan${(10000..99999).random()}"
        val request = Request.Builder()
            .url("wss://irc-ws.chat.twitch.tv:443")
            .build()

        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("CAP REQ :twitch.tv/tags twitch.tv/commands")
                webSocket.send("PASS SCHMOOPIIE")
                webSocket.send("NICK $anonNick")
                webSocket.send("JOIN #${channel.lowercase()}")
                _connected.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                text.split("\r\n").filter { it.isNotBlank() }.forEach { line ->
                    parseLine(line, webSocket)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connected.value = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connected.value = false
            }
        })
    }

    private fun parseLine(line: String, webSocket: WebSocket) {
        if (line.startsWith("PING")) {
            webSocket.send("PONG :tmi.twitch.tv")
            return
        }
        if (!line.contains("PRIVMSG")) return

        val tagsPart = if (line.startsWith("@")) line.substringBefore(" ") else null

        val colorHex = tagsPart?.split(";")
            ?.firstOrNull { it.startsWith("color=") }
            ?.substringAfter("color=")
            ?.takeIf { it.isNotBlank() }

        val displayName = tagsPart?.split(";")
            ?.firstOrNull { it.startsWith("display-name=") }
            ?.substringAfter("display-name=")

        val emotesTag = tagsPart?.split(";")
            ?.firstOrNull { it.startsWith("emotes=") }
            ?.substringAfter("emotes=")
            ?.takeIf { it.isNotBlank() }

        val afterPrivmsg = line.substringAfter("PRIVMSG ", "")
        val messageText = afterPrivmsg.substringAfter(" :", "")
        val fallbackUser = line.substringAfter(":").substringBefore("!")

        if (messageText.isNotBlank()) {
            val emoteRefs = parseEmoteTag(emotesTag)
            _messages.update { current ->
                (current + ChatMessage(
                    username = displayName ?: fallbackUser,
                    message = messageText,
                    colorHex = colorHex,
                    twitchEmotes = emoteRefs
                )).takeLast(200) // cap history so memory stays sane
            }
        }
    }

    // Format: "emoteId:start-end,start-end/emoteId2:start-end"
    private fun parseEmoteTag(tag: String?): List<TwitchEmoteRef> {
        if (tag.isNullOrBlank()) return emptyList()
        val result = mutableListOf<TwitchEmoteRef>()
        tag.split("/").forEach { entry ->
            val id = entry.substringBefore(":")
            val ranges = entry.substringAfter(":", "")
            ranges.split(",").forEach { range ->
                val parts = range.split("-")
                if (parts.size == 2) {
                    val start = parts[0].toIntOrNull()
                    val end = parts[1].toIntOrNull()
                    if (start != null && end != null) {
                        result += TwitchEmoteRef(id, start, end)
                    }
                }
            }
        }
        return result
    }

    fun disconnect() {
        socket?.close(1000, "closing")
        socket = null
        _connected.value = false
    }
}
