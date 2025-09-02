package com.lucasmoura.studysmart.domain.repository

import com.lucasmoura.studysmart.domain.model.Subject
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {

    suspend fun upsertSubject(subject: Subject)

    fun getTotalSubjectCount() : Flow<Int>

    fun getTotalGoalHours() : Flow<Float>

    suspend fun getSubjectById(subjectId: Int) : Subject?

    fun getAllSubjects() : Flow<List<Subject>>

    suspend fun deleteSubject(subjectId: Int)
}