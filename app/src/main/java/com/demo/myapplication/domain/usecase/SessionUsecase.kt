package com.demo.myapplication.domain.usecase

import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.data.local.entity.Session
import com.demo.myapplication.domain.repository.SessionRepository
import javax.inject.Inject

class SessionUsecase @Inject constructor(
    private val repository: SessionRepository
){
    suspend operator fun invoke(goalId:Long,dailyAllocatedSeconds:Long): Session{
      return  repository.startOrAttachSession(
            goalId,
            dailyAllocatedSeconds
        )
    }
    suspend fun pauseSession(session: Session): Session {
        return repository.pauseSession(session)
    }
    suspend fun resumeSession(session: Session): Session {
        return repository.resumeSession(session)
    }
    suspend fun completeSession(session: Session){
        return repository.completeSession(session)
    }
}