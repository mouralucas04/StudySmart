package com.lucasmoura.studysmart.presentation.dashboard

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasmoura.studysmart.domain.model.Session
import com.lucasmoura.studysmart.domain.model.Subject
import com.lucasmoura.studysmart.domain.model.Task
import com.lucasmoura.studysmart.domain.repository.SessionRepository
import com.lucasmoura.studysmart.domain.repository.SubjectRepository
import com.lucasmoura.studysmart.domain.repository.TaskRepository
import com.lucasmoura.studysmart.util.SnackbarEvent
import com.lucasmoura.studysmart.util.toHours
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state = combine(
        _state,
        subjectRepository.getTotalSubjectCount(),
        subjectRepository.getTotalGoalHours(),
        subjectRepository.getAllSubjects(),
        sessionRepository.getTotalSessionsDuration()
    ) { state, subjectCount, goalHours, subjects, totalSessionDuration ->
        state.copy(
            totalSubjectCount = subjectCount,
            totalGoalStudyHours = goalHours,
            subjects = subjects,
            totalStudiedHours = totalSessionDuration.toHours()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    val tasks: StateFlow<List<Task>> = taskRepository.getAllUpcomingTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentSessions: StateFlow<List<Session>> = sessionRepository.getRecentFiveSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.OnSubjectNameChange -> {
                _state.update {
                    it.copy(subjectName = event.name)
                }
            }

            is DashboardEvent.OnDeleteSessionButtonClick -> {
                _state.update {
                    it.copy(session = event.session)
                }
            }

            is DashboardEvent.OnGoalStudyHoursChange -> {
                _state.update {
                    it.copy(goalStudyHours = event.hours)
                }
            }

            is DashboardEvent.OnSubjectCardColorChange -> {
                _state.update {
                    it.copy(subjectCardColors = event.colors)
                }
            }
            DashboardEvent.SaveSubject -> saveSubject()

            is DashboardEvent.OnTaskIsCompleteChange -> updateTask(event.task)

            DashboardEvent.DeleteSession -> deleteSession()
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try{
                taskRepository.upsertTask(
                    task = task.copy(isComplete = !task.isComplete)
                )
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Saved in completed tasks"
                    )
                )
            } catch (e: Exception){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't update task. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }


    private val _snackBarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackBarEventFlow = _snackBarEventFlow.asSharedFlow()

    private fun saveSubject() {
        viewModelScope.launch {
            try{
                subjectRepository.upsertSubject(
                    subject = Subject(
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() }
                    )
                )
                _state.update {
                    it.copy(
                        subjectName = "",
                        goalStudyHours = "",
                        subjectCardColors = Subject.subjectCardColors.random()
                    )
                }
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Subject saved successfully"
                    )
                )
            } catch (e: Exception){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't save subject. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun deleteSession() {
        viewModelScope.launch {
            try{
                state.value.session?.let {
                    sessionRepository.deleteSession(it)
                }
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Session deleted successfully"
                    )
                )
            } catch (e: Exception){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't delete session. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }


}