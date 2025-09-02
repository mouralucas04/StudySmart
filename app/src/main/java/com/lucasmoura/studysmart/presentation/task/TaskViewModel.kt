package com.lucasmoura.studysmart.presentation.task

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucasmoura.studysmart.domain.model.Task
import com.lucasmoura.studysmart.domain.repository.SubjectRepository
import com.lucasmoura.studysmart.domain.repository.TaskRepository
import com.lucasmoura.studysmart.util.Priority
import com.lucasmoura.studysmart.util.SnackbarEvent
import com.ramcosta.composedestinations.generated.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val navArgs: TaskScreenNavArgs = savedStateHandle.navArgs()

    private val _state = MutableStateFlow(TaskState())
    val state = combine(
        _state,
        subjectRepository.getAllSubjects()
    ) { state, subjects ->
        state.copy(
            subjects = subjects
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = TaskState()
    )

    private val _snackBarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackBarEventFlow = _snackBarEventFlow.asSharedFlow()

    init {
        fetchTask()
        fetchSubject()
    }


    fun onEvent(event: TaskEvent) {
        when (event) {
            is TaskEvent.OnTitleChange -> {
                _state.update { it.copy(title = event.title) }
            }

            is TaskEvent.OnDescriptionChange -> {
                _state.update { it.copy(description = event.description) }
            }

            is TaskEvent.OnDateChange -> {
                _state.update { it.copy(dueDate = event.millis) }
            }

            is TaskEvent.OnPriorityChange -> {
                _state.update { it.copy(priority = event.priority) }
            }

            TaskEvent.OnIsCompleteChange -> {
                _state.update { it.copy(isTaskComplete = !_state.value.isTaskComplete) }
            }

            is TaskEvent.OnRelatedSubjectSelect -> {
                _state.update {
                    it.copy(
                        relatedToSubject = event.subject.name,
                        subjectId = event.subject.subjectId
                    )
                }
            }

            TaskEvent.SaveTask -> saveTask()
            TaskEvent.DeleteTask -> deleteTask()
        }
    }

    private fun deleteTask() {
        viewModelScope.launch {
            try {
                val currentTaskId = state.value.currentTaskId
                if (currentTaskId != null) {
                    withContext(Dispatchers.IO) {
                        taskRepository.deleteTask(taskId = currentTaskId)
                    }
                    _snackBarEventFlow.emit(
                        SnackbarEvent.ShowSnackbar(
                            message = "Task deleted successfully"
                        )
                    )
                    _snackBarEventFlow.emit(SnackbarEvent.NavigateUp)
                } else {
                    SnackbarEvent.ShowSnackbar(
                        message = "No task to delete"
                    )
                }
            } catch (e: Exception) {
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't delete task. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun saveTask() {
        viewModelScope.launch {
            val state = _state.value
            if (state.subjectId == null || state.relatedToSubject == null) {
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Please select subject related to the task.",
                        duration = SnackbarDuration.Long
                    )
                )
                return@launch
            }
            try {
                taskRepository.upsertTask(
                    task = Task(
                        title = state.title,
                        description = state.description,
                        dueDate = state.dueDate ?: Instant.now().toEpochMilli(),
                        priority = state.priority.value,
                        relatedToSubject = state.relatedToSubject,
                        isComplete = state.isTaskComplete,
                        taskId = state.currentTaskId,
                        taskSubjectId = state.subjectId
                    )
                )
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Task Saved Successfully",
                        duration = SnackbarDuration.Long
                    )
                )
                _snackBarEventFlow.emit(
                    SnackbarEvent.NavigateUp
                )
            } catch (e: Exception) {
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't save task. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )
            }
        }
    }

    private fun fetchTask() {
        viewModelScope.launch {
            try{
                navArgs.taskId?.let { id ->
                    taskRepository.getTaskById(id)?.let { task ->
                        _state.update {
                            it.copy(
                                title = task.title,
                                description = task.description,
                                dueDate = task.dueDate,
                                priority = Priority.fromInt(task.priority),
                                relatedToSubject = task.relatedToSubject,
                                subjectId = task.taskSubjectId,
                                currentTaskId = task.taskId,
                                isTaskComplete = task.isComplete
                            )
                        }
                    }
                }
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Task fecthed Successfully",
                        duration = SnackbarDuration.Long
                    )
                )

            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun fetchSubject() {
        viewModelScope.launch {
            navArgs.subjectId?.let { id ->
                subjectRepository.getSubjectById(id)?.let { subject ->
                    _state.update {
                        it.copy(
                            relatedToSubject = subject.name,
                            subjectId = subject.subjectId
                        )
                    }
                }
            }
        }
    }

}