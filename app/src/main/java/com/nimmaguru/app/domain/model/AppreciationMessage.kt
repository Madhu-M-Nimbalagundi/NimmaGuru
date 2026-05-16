package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class AppreciationMessage(
    val id: String = "",
    val studentUid: String = "",
    val studentEmail: String = "",
    val studentName: String = "",
    val guruName: String = "",
    val message: String = "",
    val createdAt: Timestamp? = null
)

