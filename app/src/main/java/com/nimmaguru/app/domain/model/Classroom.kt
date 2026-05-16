package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class Classroom(
    val id: String = "",
    val ownerGuruId: String = "",
    val title: String = "",
    val boardType: String = "",
    val gradeLevel: String = "",
    val studentIds: List<String> = emptyList(),
    val createdAt: Timestamp? = null
)
