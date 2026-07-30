package io.purple.mars.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.purple.mars.emotes.Emote
import io.purple.mars.emotes.EmoteRepository

@Composable
fun EmotesScreen() {
    val context = LocalContext.current
    val imageLoader = rememberAnimatedImageLoader()
    val repo = remember { EmoteRepository() }

    var globalEmotes by remember { mutableStateOf<List<Emote>>(emptyList()) }
    var channelEmotes by remember { mutableStateOf<List<Emote>>(emptyList()) }
    var channelInput by remember { mutableStateOf("") }
    var loadingGlobal by remember { mutableStateOf(true) }
    var loadingChannel by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        globalEmotes = repo.fetchGlobalEmotes()
        loadingGlobal = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = channelInput,
                onValueChange = { channelInput = it },
                placeholder = { Text("Channel name") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                if (channelInput.isNotBlank()) {
                    loadingChannel = true
                }
            }) {
                Text("Load")
            }
        }

        LaunchedEffect(loadingChannel) {
            if (loadingChannel) {
                channelEmotes = repo.fetchChannelEmotes(channelInput)
                loadingChannel = false
            }
        }

        if (channelEmotes.isNotEmpty() || loadingChannel) {
            Text(
                "Channel emotes — #$channelInput",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            if (loadingChannel) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            } else {
                EmoteGrid(channelEmotes, imageLoader, context, maxHeightItems = true)
            }
        }

        Text(
            "Global emotes",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )

        if (loadingGlobal) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Text("Loading emotes…", modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            EmoteGrid(globalEmotes, imageLoader, context, maxHeightItems = false)
        }
    }
}

@Composable
private fun EmoteGrid(
    emotes: List<Emote>,
    imageLoader: coil.ImageLoader,
    context: Context,
    maxHeightItems: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 64.dp),
        modifier = if (maxHeightItems) Modifier.fillMaxWidth() else Modifier.fillMaxSize()
    ) {
        items(emotes) { emote ->
            Column(
                modifier = Modifier
                    .padding(6.dp)
                    .clickable { copyToClipboard(context, emote.name) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = emote.url,
                    contentDescription = emote.name,
                    imageLoader = imageLoader,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = emote.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("emote", text))
}
