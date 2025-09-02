package com.lucasmoura.studysmart.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lucasmoura.studysmart.domain.model.Session
import com.lucasmoura.studysmart.domain.model.Subject
import com.lucasmoura.studysmart.domain.model.Task

@Database(
    entities = [Task::class, Subject::class, Session::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(ColorListConverter::class)
abstract class AppDatabase : RoomDatabase(){

    abstract fun taskDao(): TaskDao

    abstract fun subjectDao(): SubjectDao

    abstract fun sessionDao(): SessionDao

}