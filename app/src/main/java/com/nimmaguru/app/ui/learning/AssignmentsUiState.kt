package com.nimmaguru.app.ui.learning

import com.nimmaguru.app.domain.model.Assignment

data class AssignmentsUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val assignments: List<Assignment> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
