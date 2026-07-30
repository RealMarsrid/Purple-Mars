package io.purple.mars.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SearchResultChannel(
    val login: String,
    val displayName: String,
    val title: String?,
    val game: String?,
    val viewers: Int?,
    val thumbnailUrl: String?,
    val isLive: Boolean
)

class TwitchSearchRepository {
    private val client = OkHttpClient()
    private val clientId = "kimne78kx3ncx6brgo4mv6wki5h1ko" // public anonymous web client id

    // Exact channel lookup (reliable) rather than fuzzy search (Twitch's real search
    // endpoint needs persisted query hashes we don't have access to).
    suspend fun lookup(login: String): SearchResultChannel? = withContext(Dispatchers.IO) {
        val clean = login.trim().lowercase()
        if (clean.isBlank()) return@withContext null

        val body = """
            {"query":"query{user(login:\"$clean\"){login displayName stream{title viewersCount game{name}}}}"}
        """.trimIndent()

        val request = Request.Builder()
            .url("https://gql.twitch.tv/gql")
            .addHeader("Client-Id", clientId)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val json = JSONObject(resp.body?.string() ?: return@withContext null)
                val user = json.getJSONObject("data").optJSONObject("user") ?: return@withContext null
                val stream = user.optJSONObject("stream")
                val actualLogin = user.getString("login")
                SearchResultChannel(
                    login = actualLogin,
                    displayName = user.getString("displayName"),
                    title = stream?.optString("title"),
                    game = stream?.optJSONObject("game")?.optString("name"),
                    viewers = stream?.optInt("viewersCount"),
                    thumbnailUrl = if (stream != null)
                        "https://static-cdn.jtvnw.net/previews-ttv/live_user_$actualLogin-320x180.jpg"
                    else null,
                    isLive = stream != null
                )
            }
        }.getOrNull()
    }

    suspend fun lookupMany(logins: List<String>): List<SearchResultChannel> = withContext(Dispatchers.IO) {
        logins.map { login -> async { lookup(login) } }.awaitAll().filterNotNull()
    }
}
