package io.purple.mars.data.model

data class Stream(
    val username: String,
    val title: String,
    val viewers: Int,
    val category: String,
    val isLive: Boolean
)
