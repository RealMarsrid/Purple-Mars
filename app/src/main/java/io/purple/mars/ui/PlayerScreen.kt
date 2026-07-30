package io.purple.mars.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.purple.mars.media.StreamInfo
import io.purple.mars.media.StreamInfoRepository

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlayerScreen(channel: String) {
    var muted by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var streamInfo by remember { mutableStateOf<StreamInfo?>(null) }

    LaunchedEffect(channel) {
        streamInfo = StreamInfoRepository().fetch(channel)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        val html = """
                            <html><body style="margin:0;padding:0;background:#000;">
                            <div id="twitch-embed"></div>
                            <script src="https://embed.twitch.tv/embed/v1.js"></script>
                            <script>
                              var player = new Twitch.Player("twitch-embed", {
                                channel: "$channel",
                                parent: ["localhost"],
                                autoplay: true,
                                muted: false,
                                width: "100%",
                                height: "100%"
                              });
                              window.twitchPlayer = player;
                            </script>
                            </body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://localhost", html, "text/html", "utf-8", null)
                        webViewRef = this
                    }
                }
            )

            Text(
                text = "#$channel",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            streamInfo?.let { info ->
                Text(
                    text = listOfNotNull(
                        info.title,
                        info.game,
                        info.viewers?.let { "$it viewers" }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } ?: Text(
                text = "Loading stream info…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    muted = !muted
                    webViewRef?.evaluateJavascript(
                        "window.twitchPlayer.setMuted(${muted});", null
                    )
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (muted) "Unmute" else "Mute")
            }
        }

        // Live chat for this channel, auto-connected, no channel input needed
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ChatScreen(initialChannel = channel, showChannelInput = false)
        }
    }
}
