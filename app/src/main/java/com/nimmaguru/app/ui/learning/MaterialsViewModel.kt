package com.nimmaguru.app.ui.learning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.learning.LearningRepository
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.LearningMaterial
import kotlinx.coroutines.launch

class MaterialsViewModel(
    private val repository: LearningRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(MaterialsUiState(isLoading = true))
    val uiState: LiveData<MaterialsUiState> = _uiState
    private var listenerRegistration: ListenerRegistration? = null

    fun startListening(subject: String = "", curriculumType: CurriculumType? = null) {
        listenerRegistration?.remove()
        _uiState.value = MaterialsUiState(isLoading = true)
        listenerRegistration = repository.listenMaterials(subject, curriculumType) { result ->
            result.onSuccess { materials ->
                _uiState.value = MaterialsUiState(materials = materials)
            }.onFailure { error ->
                _uiState.value = MaterialsUiState(errorMessage = error.userMessage())
            }
        }
    }

    fun toggleSaved(material: LearningMaterial, uid: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isSaving = true)
            runCatching {
                repository.toggleSavedMaterial(material.id, uid, material.isSavedBy(uid))
            }.onSuccess {
                _uiState.value = _uiState.value?.copy(
                    isSaving = false,
                    infoMessage = "Material updated."
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(
                    isSaving = false,
                    errorMessage = error.userMessage()
                )
            }
        }
    }

    fun uploadMaterial(material: LearningMaterial, uid: String) {
        if (material.title.isBlank() || material.subject.isBlank() || material.resourceUrl.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Title, subject and notes link are required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isSaving = true)
            runCatching {
                repository.uploadMaterial(material, uid)
            }.onSuccess {
                _uiState.value = _uiState.value?.copy(isSaving = false, infoMessage = "HIGH-FIVE! Notes uploaded. Contributor badge awarded.")
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(isSaving = false, errorMessage = error.userMessage())
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null, infoMessage = null)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }

    private fun Throwable.userMessage(): String = localizedMessage ?: "Could not load materials."

    class Factory(private val repository: LearningRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MaterialsViewModel::class.java)) {
                return MaterialsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
