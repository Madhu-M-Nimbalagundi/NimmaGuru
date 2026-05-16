package com.nimmaguru.app.ui.guru

import com.nimmaguru.app.domain.model.GuruProfile

data class GuruProfileUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val profile: GuruProfile? = null,
    val errorMessage: String? = null
)

