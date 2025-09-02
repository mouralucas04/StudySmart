package com.lucasmoura.studysmart.presentation.subject

import com.lucasmoura.studysmart.domain.model.Session
import com.lucasmoura.studysmart.domain.model.Task

sealed class SubjectEvent {

    data object UpdateSubject : SubjectEvent()

    data object DeleteSubject : SubjectEvent()

    data object UpdateProgress : SubjectEvent()

    data object DeleteSession : SubjectEvent()

    data class OnTaskIsCompleteChange(val task: Task) : SubjectEvent()

    data class OnSubjectCardColorChange(val color: List<androidx.compose.ui.graphics.Color>) : SubjectEvent()

    data class OnSubjectNameChange(val name: String) : SubjectEvent()

    data class OnGoalStudyHoursChange(val hours: String) : SubjectEvent()

    data class OnDeleteSessionButtonClick(val session: Session) : SubjectEvent()
}