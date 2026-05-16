package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class ClassSession(
    val id: String = "",
    val date: String = "",
    val time: String = "",
    val mentor: String = "",
    val subject: String = "",
    val location: String = "",
    val fullAddress: String = "",
    val gradeLevel: String = "",
    val boardType: String = "",
    val startsAt: Timestamp? = null,
    val enrolledStudentIds: List<String> = emptyList()
)
