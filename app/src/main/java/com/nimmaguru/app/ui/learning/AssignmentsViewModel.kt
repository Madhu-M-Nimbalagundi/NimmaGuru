package com.nimmaguru.app.ui.learning

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.learning.LearningRepository
import com.nimmaguru.app.data.task.TaskRepository
import com.nimmaguru.app.data.user.UserRoleRepository
import com.nimmaguru.app.domain.model.Assignment
import kotlinx.coroutines.launch

class AssignmentsViewModel(
    private val repository: LearningRepository,
    private val taskRepository: TaskRepository,
    private val userRoleRepository: UserRoleRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(AssignmentsUiState(isLoading = true))
    val uiState: LiveData<AssignmentsUiState> = _uiState
    private var listenerRegistration: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null

    private var lastAssignments = emptyList<Assignment>()
    private var lastTasks = emptyList<Assignment>()

    fun startListening(uid: String) {
        if (listenerRegistration != null) return
        _uiState.value = AssignmentsUiState(isLoading = true)
        
        listenerRegistration = repository.listenAssignments(uid) { result ->
            result.onSuccess { assignments ->
                lastAssignments = assignments
                updateMergedList()
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(isLoading = false, errorMessage = error.userMessage())
            }
        }

        viewModelScope.launch {
            val grade = userRoleRepository.loadGradeLevel(uid)
            tasksListener = taskRepository.listenAssignedTasks(grade) { result ->
                result.onSuccess { tasks ->
                    lastTasks = tasks.map { task ->
                        Assignment(
                            id = task.id,
                            title = task.title,
                            instructions = task.description,
                            dueDate = task.dueDate,
                            status = Assignment.STATUS_PENDING
                        )
                    }
                    updateMergedList()
                }.onFailure { error ->
                    _uiState.value = _uiState.value?.copy(isLoading = false, errorMessage = error.userMessage())
                }
            }
        }
    }

    private fun updateMergedList() {
        val merged = (lastAssignments + lastTasks)
            .distinctBy { it.id }
            .sortedByDescending { it.dueDate } // Newest first or based on your needs
        
        _uiState.value = _uiState.value?.copy(
            isLoading = false,
            assignments = merged
        )
    }

    fun submit(uid: String, assignmentId: String, submission: String) {
        if (submission.trim().isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Write your answer before submitting.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isSubmitting = true)
            runCatching {
                repository.submitAssignment(uid, assignmentId, submission)
            }.onSuccess {
                _uiState.value = _uiState.value?.copy(
                    isSubmitting = false,
                    infoMessage = "Assignment submitted."
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(
                    isSubmitting = false,
                    errorMessage = error.userMessage()
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null, infoMessage = null)
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        tasksListener?.remove()
        super.onCleared()
    }

    private fun Throwable.userMessage(): String = localizedMessage ?: "Could not load assignments."

    class Factory(
        private val repository: LearningRepository,
        private val taskRepository: TaskRepository,
        private val userRoleRepository: UserRoleRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AssignmentsViewModel::class.java)) {
                return AssignmentsViewModel(repository, taskRepository, userRoleRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
