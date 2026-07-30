package io.purple.mars.emotes

data class Emote(
    val name: String,
    val url: String,
    val source: String, // "7TV", "BTTV", "FFZ"
    val animated: Boolean = false
)
