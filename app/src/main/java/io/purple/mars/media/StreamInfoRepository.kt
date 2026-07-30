package io.purple.mars.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class StreamInfoRepository {
    private val client = OkHttpClient()
    private val clientId = "kimne78kx3ncx6brgo4mv6wki5h1ko" // public anonymous web client id

    suspend fun fetch(channel: String): StreamInfo? = withContext(Dispatchers.IO) {
        val query = """
            {"query":"query{user(login:\"${channel.lowercase()}\"){stream{title viewersCount game{name}}}}"}
        """.trimIndent()

        val body = query.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://gql.twitch.tv/gql")
            .addHeader("Client-Id", clientId)
            .post(body)
            .build()

        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string() ?: return@withContext null)
                val stream = json.getJSONObject("data").getJSONObject("user").optJSONObject("stream")
                    ?: return@withContext null
                StreamInfo(
                    title = stream.optString("title", null),
                    game = stream.optJSONObject("game")?.optString("name"),
                    viewers = stream.optInt("viewersCount")
                )
            }
        }.getOrNull()
    }
}
