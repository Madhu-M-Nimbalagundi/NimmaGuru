package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class Task(
    val id: String = "",
    val classroomId: String = "",
    val ownerGuruId: String = "",
    val targetGrade: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val createdAt: Timestamp? = null
)
