package com.nimmaguru.app.domain.model

data class ProgressSummary(
    val classesAttended: Int = 0,
    val classesTaken: Int = 0,
    val assignmentsSubmitted: Int = 0,
    val averageScore: Int = 0,
    val streakDays: Int = 0,
    val notesCount: Int = 0,
    val badges: List<String> = emptyList(),
    val recentActivity: List<String> = emptyList()
)
