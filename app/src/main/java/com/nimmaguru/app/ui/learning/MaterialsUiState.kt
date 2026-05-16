package com.nimmaguru.app.ui.learning

import com.nimmaguru.app.domain.model.LearningMaterial

data class MaterialsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val materials: List<LearningMaterial> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)
