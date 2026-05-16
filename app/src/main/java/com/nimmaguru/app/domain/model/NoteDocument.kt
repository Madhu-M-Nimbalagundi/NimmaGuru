package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class NoteDocument(
    val id: String = "",
    val fileUrl: String = "",
    val boardType: String = "",
    val subject: String = "",
    val uploadedBy: String = "",
    val createdAt: Timestamp? = null
)
