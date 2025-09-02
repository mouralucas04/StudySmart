package com.lucasmoura.studysmart.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Task(
    val title: String,
    val description: String,
    val dueDate: Long,
    val priority: Int,
    val relatedToSubject: String,
    val isComplete: Boolean,
    @PrimaryKey(autoGenerate = true)
    val taskId: Int? = null,
    val taskSubjectId: Int
)
