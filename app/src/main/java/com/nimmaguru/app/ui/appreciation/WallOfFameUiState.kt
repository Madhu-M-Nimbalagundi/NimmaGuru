package com.nimmaguru.app.ui.appreciation

import com.nimmaguru.app.domain.model.AppreciationMessage

data class WallOfFameUiState(
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val isPosted: Boolean = false,
    val messages: List<AppreciationMessage> = emptyList(),
    val errorMessage: String? = null
)

