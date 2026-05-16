package com.nimmaguru.app.ui.auth

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val needsRoleSelection: Boolean = false,
    val errorMessage: String? = null
)
