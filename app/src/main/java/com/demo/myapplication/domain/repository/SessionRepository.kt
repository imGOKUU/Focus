package com.demo.myapplication.domain.repository

import com.demo.myapplication.data.local.entity.Session

interface SessionRepository{
  suspend  fun startOrResumeSession(goalId:Long,dailyAllocatedSeconds:Long): Session

  suspend  fun completeSession(session: Session)
}