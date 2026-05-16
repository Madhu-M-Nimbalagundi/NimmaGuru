package com.nimmaguru.app.domain.model

data class SyllabusTopic(
    val board: String,
    val classLevel: String,
    val subject: String,
    val chapter: String,
    val topics: List<String>
)
