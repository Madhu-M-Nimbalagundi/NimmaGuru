package com.nimmaguru.app.domain.model

import com.google.firebase.Timestamp

data class LearningMaterial(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val curriculumType: String = "",
    val classLevel: String = "",
    val type: String = "",
    val size: String = "",
    val description: String = "",
    val resourceUrl: String = "",
    val createdAt: Timestamp? = null,
    val savedBy: List<String> = emptyList()
) {
    fun isSavedBy(uid: String): Boolean = savedBy.contains(uid)
}
