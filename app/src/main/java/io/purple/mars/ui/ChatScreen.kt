package io.purple.mars.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.purple.mars.emotes.Emote
import io.purple.mars.emotes.EmoteRepository
import io.purple.mars.twitch.ChatMessage
import io.purple.mars.twitch.TwitchEmoteRef
import io.purple.mars.twitch.TwitchIrcClient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(initialChannel: String? = null, showChannelInput: Boolean = true) {
    val client = remember { TwitchIrcClient() }
    val emoteRepo = remember { EmoteRepository() }
    val messages by client.messages.collectAsState()
    val connected by client.connected.collectAsState()
    var channel by remember { mutableStateOf(initialChannel ?: "") }
    var emoteMap by remember { mutableStateOf<Map<String, Emote>>(emptyMap()) }
    var emotesLoading by remember { mutableStateOf(false) }
    val imageLoader = rememberAnimatedImageLoader()
    val listState = rememberLazyListState()

    suspend fun loadEmotesFor(ch: String) {
        emotesLoading = true
        val global = emoteRepo.fetchGlobalEmotes()
        val channelEmotes = emoteRepo.fetchChannelEmotes(ch)
        emoteMap = (global + channelEmotes).associateBy { it.name }
        emotesLoading = false
    }

    LaunchedEffect(initialChannel) {
        if (!initialChannel.isNullOrBlank()) {
            channel = initialChannel
            client.connect(initialChannel)
            loadEmotesFor(initialChannel)
        }
    }

    DisposableEffect(Unit) {
        onDispose { client.disconnect() }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        if (showChannelInput) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = channel,
                    onValueChange = { channel = it },
                    placeholder = { Text("Channel name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    if (channel.isNotBlank()) {
                        client.connect(channel)
                    }
                }) {
                    Text(if (connected) "Switch" else "Join")
                }
            }

            LaunchedEffect(connected, channel) {
                if (connected && channel.isNotBlank()) {
                    loadEmotesFor(channel)
                }
            }

            Text(
                text = when {
                    !connected -> "Not connected"
                    emotesLoading -> "Connected to #$channel • loading emotes…"
                    else -> "Connected to #$channel"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(messages) { msg ->
                ChatMessageRow(msg, emoteMap, imageLoader)
            }
        }
    }
}

private sealed class MsgToken {
    data class TextPart(val text: String) : MsgToken()
    data class EmoteImage(val url: String, val key: String) : MsgToken()
}

private fun buildTokens(
    message: String,
    twitchEmotes: List<TwitchEmoteRef>,
    emoteMap: Map<String, Emote>
): List<MsgToken> {
    val tokens = mutableListOf<MsgToken>()
    val sorted = twitchEmotes.sortedBy { it.start }
    var cursor = 0

    fun addPlainTextSegment(segment: String) {
        val words = segment.split(" ")
        val runBuilder = StringBuilder()
        fun flushRun() {
            if (runBuilder.isNotEmpty()) {
                tokens += MsgToken.TextPart(runBuilder.toString())
                runBuilder.clear()
            }
        }
        words.forEach { word ->
            if (word.isEmpty()) return@forEach
            val thirdParty = emoteMap[word]
            if (thirdParty != null) {
                flushRun()
                tokens += MsgToken.EmoteImage(thirdParty.url, thirdParty.name)
            } else {
                if (runBuilder.isNotEmpty()) runBuilder.append(" ")
                runBuilder.append(word)
            }
        }
        flushRun()
    }

    for (ref in sorted) {
        if (ref.start > message.length - 1 || ref.end >= message.length || ref.start > ref.end) continue
        if (ref.start > cursor) {
            addPlainTextSegment(message.substring(cursor, ref.start))
        }
        val url = "https://static-cdn.jtvnw.net/emoticons/v2/${ref.id}/default/dark/2.0"
        tokens += MsgToken.EmoteImage(url, ref.id)
        cursor = ref.end + 1
    }
    if (cursor < message.length) {
        addPlainTextSegment(message.substring(cursor))
    }
    return tokens
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatMessageRow(
    msg: ChatMessage,
    emoteMap: Map<String, Emote>,
    imageLoader: coil.ImageLoader
) {
    val userColor = msg.colorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: MaterialTheme.colorScheme.primary

    val tokens = remember(msg.message, msg.twitchEmotes, emoteMap) {
        buildTokens(msg.message, msg.twitchEmotes, emoteMap)
    }

    FlowRow(
        modifier = Modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${msg.username}:",
            color = userColor,
            style = MaterialTheme.typography.bodyMedium
        )
        tokens.forEach { token ->
            when (token) {
                is MsgToken.TextPart -> Text(
                    text = token.text,
                    style = MaterialTheme.typography.bodyMedium
                )
                is MsgToken.EmoteImage -> AsyncImage(
                    model = token.url,
                    contentDescription = token.key,
                    imageLoader = imageLoader,
                    modifier = Modifier.size(22.dp),
                    alignment = Alignment.CenterStart
                )
            }
        }
    }
}
