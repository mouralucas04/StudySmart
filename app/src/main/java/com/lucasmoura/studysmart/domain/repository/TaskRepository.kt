package com.lucasmoura.studysmart.domain.repository

import androidx.room.Query
import androidx.room.Upsert
import com.lucasmoura.studysmart.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    suspend fun upsertTask(task: Task)

    suspend fun deleteTask(taskId: Int)

    suspend fun getTaskById(taskId: Int) : Task?

    fun getUpcomingTasksForSubject(subjectId: Int) : Flow<List<Task>>

    fun getCompletedTasksForSubject(subjectId: Int) : Flow<List<Task>>

    fun getAllUpcomingTasks() : Flow<List<Task>>
}