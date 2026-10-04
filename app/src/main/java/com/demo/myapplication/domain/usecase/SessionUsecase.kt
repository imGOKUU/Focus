package com.demo.myapplication.domain.usecase

import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.data.local.entity.Session
import com.demo.myapplication.domain.repository.SessionRepository
import javax.inject.Inject

class SessionUsecase @Inject constructor(
    private val repository: SessionRepository
){
    suspend operator fun invoke(goalId:Long,dailyAllocatedSeconds:Long): Session{
      return  repository.startOrResumeSession(
            goalId,
            dailyAllocatedSeconds
        )
    }
    suspend fun endSession(session: Session){
        return repository.completeSession(session)
    }
}