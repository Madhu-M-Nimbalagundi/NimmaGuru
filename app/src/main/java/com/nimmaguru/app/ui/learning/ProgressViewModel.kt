package com.nimmaguru.app.ui.learning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.badge.BadgeManager
import com.nimmaguru.app.data.learning.LearningRepository

class ProgressViewModel(
    private val repository: LearningRepository,
    private val badgeManager: BadgeManager
) : ViewModel() {
    private val _uiState = MutableLiveData(ProgressUiState(isLoading = true))
    val uiState: LiveData<ProgressUiState> = _uiState
    private var listenerRegistration: ListenerRegistration? = null
    private var badgeListenerRegistration: ListenerRegistration? = null

    fun startListening(uid: String) {
        if (listenerRegistration != null) return
        _uiState.value = ProgressUiState(isLoading = true)
        listenerRegistration = repository.listenProgress(uid) { result ->
            result.onSuccess { summary ->
                _uiState.value = ProgressUiState(summary = summary)
            }.onFailure { error ->
                _uiState.value = ProgressUiState(errorMessage = error.localizedMessage ?: "Could not load progress.")
            }
        }
        badgeListenerRegistration = badgeManager.listenBadges(uid) { result ->
            result.onSuccess { badges ->
                _uiState.value = _uiState.value?.copy(liveBadges = badges)
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(errorMessage = error.localizedMessage ?: "Could not load badges.")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        badgeListenerRegistration?.remove()
        super.onCleared()
    }

    class Factory(
        private val repository: LearningRepository,
        private val badgeManager: BadgeManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
                return ProgressViewModel(repository, badgeManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
