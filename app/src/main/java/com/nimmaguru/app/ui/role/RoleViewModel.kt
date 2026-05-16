package com.nimmaguru.app.ui.role

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nimmaguru.app.data.user.UserRoleRepository
import com.nimmaguru.app.domain.model.CurriculumType
import com.nimmaguru.app.domain.model.UserRole
import kotlinx.coroutines.launch

class RoleViewModel(
    private val userRoleRepository: UserRoleRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(RoleUiState())
    val uiState: LiveData<RoleUiState> = _uiState

    fun saveRole(uid: String, email: String, role: UserRole, curriculumType: CurriculumType, gradeLevel: String) {
        viewModelScope.launch {
            _uiState.value = RoleUiState(isLoading = true)
            runCatching { userRoleRepository.saveRole(uid, email, role, curriculumType, gradeLevel) }
                .onSuccess { _uiState.value = RoleUiState(isSaved = true) }
                .onFailure { error ->
                    _uiState.value = RoleUiState(
                        errorMessage = error.localizedMessage ?: "Could not save role. Please try again."
                    )
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null)
    }

    class Factory(
        private val userRoleRepository: UserRoleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RoleViewModel::class.java)) {
                return RoleViewModel(userRoleRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
