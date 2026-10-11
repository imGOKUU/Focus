package com.demo.myapplication.domain.repository

import com.demo.myapplication.data.local.entity.Session

interface SessionRepository{
  suspend  fun startOrAttachSession(goalId:Long,dailyAllocatedSeconds:Long): Session

  suspend fun pauseSession(session: Session): Session

  suspend fun resumeSession(session: Session): Session

  suspend  fun completeSession(session: Session)
}