package com.nimmaguru.app.ui.guru

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.data.guru.GuruProfileRepository
import com.nimmaguru.app.domain.model.GuruProfile
import kotlinx.coroutines.launch

class GuruProfileViewModel(
    private val repository: GuruProfileRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(GuruProfileUiState())
    val uiState: LiveData<GuruProfileUiState> = _uiState

    fun loadProfile(uid: String) {
        viewModelScope.launch {
            _uiState.value = GuruProfileUiState(isLoading = true)
            runCatching { repository.getProfile(uid) }
                .onSuccess { profile ->
                    _uiState.value = GuruProfileUiState(profile = profile)
                }
                .onFailure { error ->
                    _uiState.value = GuruProfileUiState(errorMessage = error.userMessage())
                }
        }
    }

    fun saveProfile(profile: GuruProfile) {
        val validationMessage = validate(profile)
        if (validationMessage != null) {
            _uiState.value = _uiState.value?.copy(errorMessage = validationMessage)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)
            runCatching { repository.saveProfile(profile) }
                .onSuccess {
                    _uiState.value = GuruProfileUiState(isSaved = true, profile = profile)
                }
                .onFailure { error ->
                    _uiState.value = GuruProfileUiState(errorMessage = error.userMessage())
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null, isSaved = false)
    }

    private fun validate(profile: GuruProfile): String? {
        return when {
            profile.name.isBlank() -> "Name is required."
            profile.skills.isEmpty() -> "Select at least one skill."
            profile.experience.isBlank() -> "Experience is required."
            profile.availableTime.isBlank() -> "Available time is required."
            profile.location.isBlank() -> "Location is required."
            else -> null
        }
    }

    private fun Throwable.userMessage(): String {
        return localizedMessage ?: "Could not save Guru profile. Please try again."
    }

    class Factory(
        private val repository: GuruProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GuruProfileViewModel::class.java)) {
                return GuruProfileViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

