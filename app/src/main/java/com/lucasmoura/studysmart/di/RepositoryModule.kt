package com.lucasmoura.studysmart.di

import com.lucasmoura.studysmart.data.repository.SessionRepositoryImpl
import com.lucasmoura.studysmart.data.repository.SubjectRepositoryImpl
import com.lucasmoura.studysmart.data.repository.TaskRepositoryImpl
import com.lucasmoura.studysmart.domain.repository.SessionRepository
import com.lucasmoura.studysmart.domain.repository.SubjectRepository
import com.lucasmoura.studysmart.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindSubjectRepository(
        impl: SubjectRepositoryImpl
    ) : SubjectRepository

    @Singleton
    @Binds
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl
    ) : TaskRepository

    @Singleton
    @Binds
    abstract fun bindSessionRepository(
        impl: SessionRepositoryImpl
    ) : SessionRepository

}