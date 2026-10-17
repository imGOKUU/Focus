package com.demo.myapplication.domain.repository

import com.demo.myapplication.data.local.entity.Session
import kotlinx.coroutines.flow.Flow

/** What a finished session amounted to - shown on the session result screen. */
data class SessionOutcome(
  val goalCompleted: Boolean,
  val focusedSeconds: Long,
  val remainingTodaySeconds: Long
)

interface SessionRepository{
  // Null when today's time for this goal is already used up - there is
  // nothing left to start.
  suspend  fun startOrAttachSession(goalId:Long,dailyAllocatedSeconds:Long): Session?

  fun observeActiveSession(): Flow<Session?>

  // Ends a running/paused session early. Time already spent is credited to
  // today's goal (honestly - no completion), the rest stays available.
  suspend fun endSession(session: Session): SessionOutcome?

  // Ends whatever session is active for this goal (e.g. the goal is deleted).
  suspend fun endActiveSessionForGoal(goalId: Long)

  suspend fun pauseSession(session: Session): Session

  suspend fun resumeSession(session: Session): Session

  suspend  fun completeSession(session: Session)

  // Marks the active session COMPLETED if its time has run out. Safe to call
  // any time, from anywhere - it's a no-op if nothing is active or time remains.
  suspend fun completeIfExpired()
}