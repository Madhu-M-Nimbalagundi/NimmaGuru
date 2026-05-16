package com.nimmaguru.app.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.nimmaguru.app.data.calendar.ClassSessionRepository
import com.nimmaguru.app.domain.model.ClassSession
import com.nimmaguru.app.util.NimmaDateFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ClassCalendarViewModel(
    private val repository: ClassSessionRepository
) : ViewModel() {
    private val _uiState = MutableLiveData(ClassCalendarUiState(isLoading = true))
    val uiState: LiveData<ClassCalendarUiState> = _uiState

    private var listenerRegistration: ListenerRegistration? = null

    fun startListening() {
        if (listenerRegistration != null) return

        _uiState.value = _uiState.value?.copy(isLoading = true, errorMessage = null)
        listenerRegistration = repository.listenUpcomingSessions { result ->
            result
                .onSuccess { sessions ->
                    _uiState.value = ClassCalendarUiState(sessions = sessions)
                }
                .onFailure { error ->
                    _uiState.value = ClassCalendarUiState(errorMessage = error.userMessage())
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value?.copy(errorMessage = null, infoMessage = null)
    }

    fun createSession(
        date: String,
        time: String,
        mentor: String,
        subject: String,
        location: String,
        gradeLevel: String,
        boardType: String,
        sessionId: String = "",
        fullAddress: String = "",
        enrolledStudentIds: List<String> = emptyList()
    ) {
        if (date.isBlank() || time.isBlank() || mentor.isBlank() || subject.isBlank() || location.isBlank() || gradeLevel.isBlank() || boardType.isBlank()) {
            _uiState.value = _uiState.value?.copy(errorMessage = "Fill all class details.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isSaving = true)
            runCatching {
                repository.saveSession(
                    ClassSession(
                        id = sessionId,
                        date = date.trim(),
                        time = time.trim(),
                        mentor = mentor.trim(),
                        subject = subject.trim(),
                        location = location.trim(),
                        fullAddress = fullAddress.trim(),
                        gradeLevel = gradeLevel.trim(),
                        boardType = boardType.trim(),
                        startsAt = parseStartsAt(date, time),
                        enrolledStudentIds = enrolledStudentIds
                    )
                )
            }.onSuccess {
                _uiState.value = _uiState.value?.copy(isSaving = false, infoMessage = "Class session created.")
            }.onFailure { error ->
                _uiState.value = _uiState.value?.copy(isSaving = false, errorMessage = error.userMessage())
            }
        }
    }

    fun deleteSession(session: ClassSession) {
        if (session.id.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isSaving = true)
            runCatching { repository.deleteSession(session.id) }
                .onSuccess {
                    _uiState.value = _uiState.value?.copy(isSaving = false, infoMessage = "Class session deleted.")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value?.copy(isSaving = false, errorMessage = error.userMessage())
                }
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        listenerRegistration = null
        super.onCleared()
    }

    private fun Throwable.userMessage(): String {
        return localizedMessage ?: "Could not load upcoming sessions. Please try again."
    }

    private fun parseStartsAt(date: String, time: String): Timestamp {
        val parsedDate = NimmaDateFormatter.parse(date) ?: Date()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        val parsedTime = try {
            timeFormat.parse(time.trim().uppercase(Locale.US)) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val calDate = Calendar.getInstance().apply { setTime(parsedDate) }
        val calTime = Calendar.getInstance().apply { setTime(parsedTime) }
        
        val resultCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, calDate.get(Calendar.YEAR))
            set(Calendar.MONTH, calDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, calDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, calTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        return Timestamp(resultCal.time)
    }

    class Factory(
        private val repository: ClassSessionRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClassCalendarViewModel::class.java)) {
                return ClassCalendarViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
