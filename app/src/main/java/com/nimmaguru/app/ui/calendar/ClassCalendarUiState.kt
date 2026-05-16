package com.nimmaguru.app.ui.calendar

import com.nimmaguru.app.domain.model.ClassSession

data class ClassCalendarUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val sessions: List<ClassSession> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
