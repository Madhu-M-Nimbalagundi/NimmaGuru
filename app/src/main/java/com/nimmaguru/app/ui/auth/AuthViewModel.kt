package com.nimmaguru.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.data.auth.AuthRepository
import com.nimmaguru.app.data.user.UserRoleRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRoleRepository: UserRoleRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(AuthUiState())
    val uiState: LiveData<AuthUiState> = _uiState

    val isUserLoggedIn: Boolean
        get() = authRepository.currentUser != null

    fun login(email: String, password: String) {
        if (!validate(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { authRepository.login(email, password) }
                .onSuccess { user -> resolveRole(user.uid) }
                .onFailure { _uiState.value = AuthUiState(errorMessage = it.userMessage()) }
        }
    }

    fun signup(email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _uiState.value = AuthUiState(errorMessage = "Passwords do not match.")
            return
        }

        if (!validate(email, password)) return

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { authRepository.signup(email, password) }
                .onSuccess { _uiState.value = AuthUiState(isSuccess = true, needsRoleSelection = true) }
                .onFailure { _uiState.value = AuthUiState(errorMessage = it.userMessage()) }
        }
    }

    fun resolveCurrentUserRole() {
        val user = authRepository.currentUser
        if (user == null) {
            _uiState.value = AuthUiState()
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            runCatching { userRoleRepository.loadRole(user.uid) }
                .onSuccess { role ->
                    _uiState.value = AuthUiState(
                        isSuccess = true,
                        needsRoleSelection = role == null
                    )
                }
                .onFailure { _uiState.value = AuthUiState(errorMessage = it.userMessage()) }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    private fun validate(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = AuthUiState(errorMessage = "Email is required.")
                false
            }
            password.length < 6 -> {
                _uiState.value = AuthUiState(errorMessage = "Password must be at least 6 characters.")
                false
            }
            else -> true
        }
    }

    private fun Throwable.userMessage(): String {
        return localizedMessage ?: "Authentication failed. Please try again."
    }

    private suspend fun resolveRole(uid: String) {
        val role = userRoleRepository.loadRole(uid)
        _uiState.value = AuthUiState(
            isSuccess = true,
            needsRoleSelection = role == null
        )
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val userRoleRepository: UserRoleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(authRepository, userRoleRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
