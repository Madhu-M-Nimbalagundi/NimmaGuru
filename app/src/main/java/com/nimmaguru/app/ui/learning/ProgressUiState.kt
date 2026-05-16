package com.nimmaguru.app.ui.learning

import com.nimmaguru.app.domain.model.ProgressSummary

data class ProgressUiState(
    val isLoading: Boolean = false,
    val summary: ProgressSummary = ProgressSummary(),
    val liveBadges: List<String> = emptyList(),
    val errorMessage: String? = null
)
