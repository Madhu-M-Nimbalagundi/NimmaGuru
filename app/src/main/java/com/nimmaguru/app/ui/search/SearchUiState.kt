package com.nimmaguru.app.ui.search

import com.nimmaguru.app.domain.model.Guru

data class SearchUiState(
    val isLoading: Boolean = false,
    val selectedSkills: Set<String> = emptySet(),
    val village: String = "",
    val gurus: List<Guru> = emptyList(),
    val errorMessage: String? = null
)
