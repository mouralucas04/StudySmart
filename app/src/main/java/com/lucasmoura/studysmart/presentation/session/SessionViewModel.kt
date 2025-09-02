package com.lucasmoura.studysmart.presentation.session

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lucasmoura.studysmart.domain.model.Session
import com.lucasmoura.studysmart.domain.repository.SessionRepository
import com.lucasmoura.studysmart.domain.repository.SubjectRepository
import com.lucasmoura.studysmart.util.SnackbarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val sessionRepository: SessionRepository
): ViewModel() {

    private val _snackBarEventFlow = MutableSharedFlow<SnackbarEvent>()
    val snackBarEventFlow = _snackBarEventFlow.asSharedFlow()

    private val _state = MutableStateFlow(SessionState())
    val state = combine(
        _state,
        subjectRepository.getAllSubjects(),
        sessionRepository.getAllSessions()
    ){ state, subjects, sessions ->
        state.copy(
            subjects = subjects,
            sessions = sessions,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = SessionState()
    )

    fun onEvent(event: SessionEvent){
        when(event){
            SessionEvent.CheckSubjectId -> { }
            SessionEvent.DeleteSession -> deleteSession()
            is SessionEvent.OnDeleteSessionButtonClick -> {
                _state.update {
                    it.copy(
                        session = event.session
                    )
                }
            }
            is SessionEvent.OnRelatedToSubjectChange ->{
                _state.update {
                    it.copy(
                        relatedToSubject = event.subject.name,
                        subjectId = event.subject.subjectId
                    )
                }
            }
            is SessionEvent.SaveSession -> insertSession(event.duration)
            is SessionEvent.UpdateSubjectIdAndRelatedToSubject -> {  }
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

    private fun insertSession(duration: Long) {
        viewModelScope.launch {
            if(duration < 36){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Single session can not be less than 36 seconds"
                    )
                )
                return@launch
            }
            try{
                sessionRepository.insertSession(
                    session = Session(
                        sessionSubjectId = state.value.subjectId ?: -1,
                        relatedToSubject = state.value.relatedToSubject ?: "",
                        date = Instant.now().toEpochMilli(),
                        duration = duration
                    )
                )
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Session saved Successfully",
                    )
                )
            } catch (e: Exception){
                _snackBarEventFlow.emit(
                    SnackbarEvent.ShowSnackbar(
                        message = "Couldn't save session. ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                )

            }
        }

    }


}