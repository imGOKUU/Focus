package com.meltide.app.domain.usecase

import com.meltide.app.data.local.entity.Goal
import com.meltide.app.data.local.entity.Session
import com.meltide.app.domain.repository.SessionOutcome
import com.meltide.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionUsecase @Inject constructor(
    private val repository: SessionRepository
){
    suspend operator fun invoke(goalId:Long,dailyAllocatedSeconds:Long): Session?{
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
    fun observeActiveSession(): Flow<Session?> = repository.observeActiveSession()

    suspend fun endSession(session: Session): SessionOutcome? = repository.endSession(session)

    suspend fun endActiveSessionForGoal(goalId: Long) = repository.endActiveSessionForGoal(goalId)

    suspend fun completeIfExpired() {
        repository.completeIfExpired()
    }
}