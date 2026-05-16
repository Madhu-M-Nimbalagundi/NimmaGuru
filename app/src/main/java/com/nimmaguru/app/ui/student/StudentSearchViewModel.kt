package com.nimmaguru.app.ui.student

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.guru.GuruProfileRepository

class StudentSearchViewModel(
    private val repository: GuruProfileRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(StudentSearchUiState())
    val uiState: LiveData<StudentSearchUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    fun search(skillFilters: List<String>, location: String) {
        listenerRegistration?.remove()
        _uiState.value = StudentSearchUiState(isLoading = true)

        listenerRegistration = repository.listenProfiles(skillFilters, location) { result ->
            result
                .onSuccess { mentors ->
                    _uiState.value = StudentSearchUiState(mentors = mentors)
                }
                .onFailure { error ->
                    _uiState.value = StudentSearchUiState(errorMessage = error.userMessage())
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onCleared()
    }

    private fun Throwable.userMessage(): String {
        return localizedMessage ?: "Could not load mentors. Please try again."
    }

    class Factory(
        private val repository: GuruProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudentSearchViewModel::class.java)) {
                return StudentSearchViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

