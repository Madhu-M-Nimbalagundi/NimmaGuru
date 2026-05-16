package com.nimmaguru.app.domain.model

data class Guru(
    val id: String = "",
    val name: String = "",
    val skills: List<String> = emptyList(),
    val freeHours: String = "",
    val village: String = "",
    val rating: Double = 0.0
)
