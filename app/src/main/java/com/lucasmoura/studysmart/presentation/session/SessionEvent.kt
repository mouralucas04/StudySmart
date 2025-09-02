package com.lucasmoura.studysmart.presentation.session

import com.lucasmoura.studysmart.domain.model.Session
import com.lucasmoura.studysmart.domain.model.Subject
import kotlin.time.Duration

sealed class SessionEvent {

    data class OnRelatedToSubjectChange(val subject: Subject) : SessionEvent()

    data class SaveSession(val duration: Long) : SessionEvent()

    data class OnDeleteSessionButtonClick(val session: Session) : SessionEvent()

    data object DeleteSession: SessionEvent()

    data object CheckSubjectId: SessionEvent()

    data class UpdateSubjectIdAndRelatedToSubject(
        val subjectId: Int,
        val relatedToSubject: String
    ): SessionEvent()

}