package com.nimmaguru.app.ui.student

import com.nimmaguru.app.domain.model.GuruProfile

data class StudentSearchUiState(
    val isLoading: Boolean = false,
    val mentors: List<GuruProfile> = emptyList(),
    val errorMessage: String? = null
)

