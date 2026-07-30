package io.purple.mars.emotes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class EmoteRepository {
    private val client = OkHttpClient()
    private val twitchClientId = "kimne78kx3ncx6brgo4mv6wki5h1ko" // public anonymous web client id

    private fun get(url: String): String? {
        val request = Request.Builder().url(url).build()
        return runCatching {
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() else null
            }
        }.getOrNull()
    }

    suspend fun fetchGlobalEmotes(): List<Emote> = withContext(Dispatchers.IO) {
        fetch7tvGlobal() + fetchBttvGlobal() + fetchFfzGlobal()
    }

    suspend fun fetchChannelEmotes(channelLogin: String): List<Emote> = withContext(Dispatchers.IO) {
        val twitchId = fetchTwitchId(channelLogin)
        val sevenTv = twitchId?.let { fetch7tvChannel(it) } ?: emptyList()
        val bttv = twitchId?.let { fetchBttvChannel(it) } ?: emptyList()
        val ffz = fetchFfzChannel(channelLogin)
        sevenTv + bttv + ffz
    }

    private fun fetchTwitchId(login: String): String? {
        val body = """
            {"query":"query{user(login:\"${login.lowercase()}\"){id}}"}
        """.trimIndent()
        val request = Request.Builder()
            .url("https://gql.twitch.tv/gql")
            .addHeader("Client-Id", twitchClientId)
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        return runCatching {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bodyStr = resp.body?.string() ?: return@use null
                val json = JSONObject(bodyStr)
                json.getJSONObject("data").optJSONObject("user")?.optString("id")
            }
        }.getOrNull()
    }

    private fun fetch7tvGlobal(): List<Emote> {
        val body = get("https://7tv.io/v3/emote-sets/global") ?: return emptyList()
        return parse7tvEmoteSet(body)
    }

    private fun fetch7tvChannel(twitchId: String): List<Emote> {
        val body = get("https://7tv.io/v3/users/twitch/$twitchId") ?: return emptyList()
        return runCatching {
            val json = JSONObject(body)
            val emoteSet = json.optJSONObject("emote_set") ?: return emptyList()
            parse7tvEmotesArray(emoteSet.optJSONArray("emotes") ?: JSONArray())
        }.getOrElse { emptyList() }
    }

    private fun parse7tvEmoteSet(body: String): List<Emote> = runCatching {
        val json = JSONObject(body)
        parse7tvEmotesArray(json.getJSONArray("emotes"))
    }.getOrElse { emptyList() }

    private fun parse7tvEmotesArray(emotes: JSONArray): List<Emote> {
        return (0 until emotes.length()).map { i ->
            val e = emotes.getJSONObject(i)
            val id = e.getString("id")
            val data = e.optJSONObject("data")
            val animated = data?.optBoolean("animated", false) ?: false
            Emote(
                name = e.getString("name"),
                url = "https://cdn.7tv.app/emote/$id/2x.webp",
                source = "7TV",
                animated = animated
            )
        }
    }

    private fun fetchBttvGlobal(): List<Emote> {
        val body = get("https://api.betterttv.net/3/cached/emotes/global") ?: return emptyList()
        return runCatching {
            val arr = JSONArray(body)
            parseBttvArray(arr)
        }.getOrElse { emptyList() }
    }

    private fun fetchBttvChannel(twitchId: String): List<Emote> {
        val body = get("https://api.betterttv.net/3/cached/users/twitch/$twitchId") ?: return emptyList()
        return runCatching {
            val json = JSONObject(body)
            val channelEmotes = json.optJSONArray("channelEmotes") ?: JSONArray()
            val sharedEmotes = json.optJSONArray("sharedEmotes") ?: JSONArray()
            parseBttvArray(channelEmotes) + parseBttvArray(sharedEmotes)
        }.getOrElse { emptyList() }
    }

    private fun parseBttvArray(arr: JSONArray): List<Emote> {
        return (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            val id = e.getString("id")
            val imageType = e.optString("imageType", "png")
            Emote(
                name = e.getString("code"),
                url = "https://cdn.betterttv.net/emote/$id/2x",
                source = "BTTV",
                animated = imageType == "gif"
            )
        }
    }

    private fun fetchFfzGlobal(): List<Emote> {
        val body = get("https://api.frankerfacez.com/v1/set/global") ?: return emptyList()
        return runCatching {
            val json = JSONObject(body)
            parseFfzSets(json.getJSONObject("sets"))
        }.getOrElse { emptyList() }
    }

    private fun fetchFfzChannel(channelLogin: String): List<Emote> {
        val body = get("https://api.frankerfacez.com/v1/room/${channelLogin.lowercase()}") ?: return emptyList()
        return runCatching {
            val json = JSONObject(body)
            parseFfzSets(json.getJSONObject("sets"))
        }.getOrElse { emptyList() }
    }

    private fun parseFfzSets(sets: JSONObject): List<Emote> {
        val result = mutableListOf<Emote>()
        sets.keys().forEach { key ->
            val set = sets.getJSONObject(key)
            val emoticons = set.getJSONArray("emoticons")
            for (i in 0 until emoticons.length()) {
                val e = emoticons.getJSONObject(i)
                val urls = e.getJSONObject("urls")
                val url = if (urls.has("2")) urls.getString("2") else urls.getString("1")
                result += Emote(
                    name = e.getString("name"),
                    url = if (url.startsWith("http")) url else "https:$url",
                    source = "FFZ",
                    animated = false
                )
            }
        }
        return result
    }
}
