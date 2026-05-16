package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class Assignment(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val dueDate: String = "",
    val instructions: String = "",
    val status: String = STATUS_PENDING,
    val submissionText: String = "",
    val score: Int? = null,
    val feedback: String = "",
    val updatedAt: Timestamp? = null
) {
    val isSubmitted: Boolean
        get() = status == STATUS_SUBMITTED || status == STATUS_REVIEWED

    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SUBMITTED = "submitted"
        const val STATUS_REVIEWED = "reviewed"
    }
}
