package com.lucasmoura.studysmart.presentation.subject

import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasmoura.studysmart.domain.model.Subject
import com.lucasmoura.studysmart.domain.model.Task
import com.lucasmoura.studysmart.domain.repository.SessionRepository
import com.lucasmoura.studysmart.domain.repository.SubjectRepository
import com.lucasmoura.studysmart.domain.repository.TaskRepository
import com.lucasmoura.studysmart.util.SnackbarEvent
import com.lucasmoura.studysmart.util.toHours
import com.ramcosta.composedestinations.generated.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navArgs: SubjectScreenNavArgs = savedStateHandle.navArgs()

    private val _snackBarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackBarEventFlow = _snackBarEventFlow.asSharedFlow()

    private val _state = MutableStateFlow(SubjectState())
    val state = combine(
        _state,
        taskRepository.getUpcomingTasksForSubject(navArgs.subjectId),
        taskRepository.getCompletedTasksForSubject(navArgs.subjectId),
        sessionRepository.getRecentTenSessionsForSubject(navArgs.subjectId),
        sessionRepository.getTotalSessionsDurationBySubject(navArgs.subjectId)
    ){ state, upcomingTasks, completedTasks, recentSessions, totalSessionsDuration ->
        state.copy(
            upcomingTasks = upcomingTasks,
            completedTasks = completedTasks,
            recentSessions = recentSessions,
            studiedHours = totalSessionsDuration.toHours()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = SubjectState()
    )

    init {
        println(navArgs)
        fetchSubject()
    }

    fun onEvent(event: SubjectEvent){
        when(event){
            is SubjectEvent.OnSubjectCardColorChange -> {
                _state.update {
                    it.copy(subjectCardColors = event.color)
                }
            }
            is SubjectEvent.OnSubjectNameChange -> {
                _state.update {
                    it.copy(subjectName = event.name)
                }
            }
            is SubjectEvent.OnGoalStudyHoursChange -> {
                _state.update {
                    it.copy(goalStudyHours = event.hours)
                }
            }
            SubjectEvent.UpdateSubject -> updateSubject()
            SubjectEvent.DeleteSession -> deleteSession()
            SubjectEvent.DeleteSubject -> deleteSubject()
            is SubjectEvent.OnDeleteSessionButtonClick -> {
                _state.update {
                    it.copy(session = event.session)
                }
            }
            is SubjectEvent.OnTaskIsCompleteChange -> updateTask(event.task)
            SubjectEvent.UpdateProgress -> {
                val goalStudyHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f
                _state.update {
                    it.copy(
                        progress = (state.value.studiedHours / goalStudyHours).coerceIn(0f,1f)
                    )
                }
            }
        }
    }

    private fun updateTask(task: Task) {
        viewModelScope.launch {
            try{
                taskRepository.upsertTask(
                    task = task.copy(isComplete = !task.isComplete)
                )
                if(task.isComplete){
                    _snackBarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(message = "Saved in upcoming tasks.")
                    )
                } else{
                    _snackBarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(message = "Saved in completed tasks.")
                    )
                }

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

    private fun updateSubject() {
        viewModelScope.launch {
            try {
                subjectRepository.upsertSubject(
                    subject = Subject(
                        name = state.value.subjectName,
                        goalHours = state.value.goalStudyHours.toFloatOrNull() ?: 1f,
                        colors = state.value.subjectCardColors.map { it.toArgb() },
                        subjectId = state.value.currentSubjectId
                    )
                )
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Subject updated sucessfully"
                    )
                )
            } catch (e: Exception){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't update subject. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun fetchSubject(){
        viewModelScope.launch {
            try{
                subjectRepository
                    .getSubjectById(navArgs.subjectId)?.let { subject ->
                        _state.update {
                            it.copy(
                                subjectName = subject.name,
                                goalStudyHours = subject.goalHours.toString(),
                                subjectCardColors = subject.colors.map { Color(it) },
                                currentSubjectId = subject.subjectId
                            )
                        }
                    }
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Subject fetched successfully",
                    )
                )
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun deleteSubject(){
        viewModelScope.launch {
            try {
                val currentSubjectId = state.value.currentSubjectId
                if(currentSubjectId != null){
                    withContext(Dispatchers.IO){
                        subjectRepository.deleteSubject(subjectId = currentSubjectId)
                    }
                    _snackBarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Subject deleted successfully"
                        )
                    )
                    _snackBarEventFlow.emit(SnackbarEvent.NavigateUp)
                } else{
                    SnackbarEvent.ShowSnackbar(
                        message = "No subject to delete"
                    )
                }
            }catch (e: Exception){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't delete subject. ${e.message}",
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