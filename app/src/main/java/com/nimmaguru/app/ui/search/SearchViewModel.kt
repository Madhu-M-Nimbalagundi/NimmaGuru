package com.nimmaguru.app.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.FirestoreRepository

class SearchViewModel(
    private val repository: FirestoreRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(SearchUiState(isLoading = true))
    val uiState: LiveData<SearchUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    init {
        refresh()
    }

    fun toggleSkill(skill: String) {
        val current = _uiState.value ?: SearchUiState()
        val updatedSkills = current.selectedSkills.toMutableSet().apply {
            if (!add(skill)) remove(skill)
        }
        _uiState.value = current.copy(selectedSkills = updatedSkills, isLoading = true)
        refresh()
    }

    fun setVillage(village: String) {
        val current = _uiState.value ?: SearchUiState()
        _uiState.value = current.copy(village = village, isLoading = true)
        refresh()
    }

    fun clearFilters() {
        _uiState.value = SearchUiState(isLoading = true)
        refresh()
    }

    fun refresh() {
        val current = _uiState.value ?: SearchUiState()
        listenerRegistration?.remove()
        listenerRegistration = repository.listenGurus(
            skillFilters = current.selectedSkills.toList(),
            village = current.village
        ) { result ->
            result
                .onSuccess { gurus ->
                    _uiState.value = (_uiState.value ?: SearchUiState()).copy(
                        isLoading = false,
                        gurus = gurus,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = (_uiState.value ?: SearchUiState()).copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Could not load Gurus."
                    )
                }
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onCleared()
    }

    class Factory(
        private val repository: FirestoreRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
