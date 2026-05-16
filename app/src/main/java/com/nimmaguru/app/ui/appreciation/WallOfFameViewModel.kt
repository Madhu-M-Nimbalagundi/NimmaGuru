package com.nimmaguru.app.ui.appreciation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.appreciation.AppreciationRepository
import kotlinx.coroutines.launch

class WallOfFameViewModel(
    private val repository: AppreciationRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(WallOfFameUiState(isLoading = true))
    val uiState: LiveData<WallOfFameUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    fun startListening() {
        if (listenerRegistration != null) return

        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)
        listenerRegistration = repository.listenMessages { result ->
            val current = _uiState.value ?: WallOfFameUiState()
            result
                .onSuccess { messages ->
                    _uiState.value = current.copy(isLoading = false, messages = messages)
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isLoading = false,
                        errorMessage = error.userMessage()
                    )
                }
        }
    }

    fun postMessage(studentUid: String, studentEmail: String, studentName: String, guruName: String, message: String) {
        if (message.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Write a Thank You message.")
            return
        }
        if (guruName.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Please mention the Guru you want to thank.")
            return
        }

        viewModelScope.launch {
            val current = _uiState.value ?: WallOfFameUiState()
            _uiState.value = current.copy(isPosting = true, isPosted = false, errorMessage = null)

            runCatching {
                repository.postMessage(studentUid, studentEmail, studentName, guruName, message)
            }
                .onSuccess {
                    val latest = _uiState.value ?: WallOfFameUiState()
                    _uiState.value = latest.copy(isPosting = false, isPosted = true)
                }
                .onFailure { error ->
                    val latest = _uiState.value ?: WallOfFameUiState()
                    _uiState.value = latest.copy(
                        isPosting = false,
                        errorMessage = error.userMessage()
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null, isPosted = false)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onCleared()
    }

    private fun Throwable.userMessage(): String {
        return localizedMessage ?: "Could not update Wall of Fame. Please try again."
    }

    class Factory(
        private val repository: AppreciationRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WallOfFameViewModel::class.java)) {
                return WallOfFameViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

