package com.demo.myapplication.domain.repository

import com.demo.myapplication.data.local.entity.Session

interface SessionRepository{
  suspend  fun startOrAttachSession(goalId:Long,dailyAllocatedSeconds:Long): Session

  suspend fun pauseSession(session: Session): Session

  suspend fun resumeSession(session: Session): Session

  suspend  fun completeSession(session: Session)

  // Marks the active session COMPLETED if its time has run out. Safe to call
  // any time, from anywhere - it's a no-op if nothing is active or time remains.
  suspend fun completeIfExpired()
}