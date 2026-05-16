package com.nimmaguru.app.domain.model

data class GuruProfile(
    val uid: String = "",
    val name: String = "",
    val skills: List<String> = emptyList(),
    val experience: String = "",
    val availableTime: String = "",
    val location: String = "",
    val samudayaBhavanaAvailable: Boolean = false,
    val samudayaBhavanaAddress: String = "",
    val thankYouCount: Int = 0,
    val classesTaken: Int = 0,
    val averageStudentReview: Double = 0.0,
    val badges: List<String> = emptyList()
)
