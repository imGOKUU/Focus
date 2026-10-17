package com.meltide.app.data.repository

import com.meltide.app.core.time.TimeProvider
import com.meltide.app.data.local.dao.DailyGoalDao
import com.meltide.app.data.local.dao.SessionDao
import com.meltide.app.data.local.entity.DailyGoal
import com.meltide.app.data.local.entity.GoalStatus
import com.meltide.app.data.local.entity.Session
import com.meltide.app.data.local.entity.SessionStatus
import com.meltide.app.domain.repository.SessionOutcome
import com.meltide.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject

/**
 * Sessions are written from several places (Focus screen, the completion
 * watcher, the blocking service), each holding its own possibly-stale copy.
 * So every write goes through [mutex] and re-reads the row from the DB first -
 * a stale copy must never overwrite newer state (e.g. a late "pause" reviving
 * a session that was already completed).
 */
class SessionRepositoryImpl @Inject constructor(
private val dailyGoalDao: DailyGoalDao,
    private val sessionDao: SessionDao,
    private val timeProvider: TimeProvider
): SessionRepository{
    private val mutex = Mutex()

  suspend override fun startOrAttachSession(
        goalId: Long,
        dailyAllocatedSeconds: Long
    ): Session? = mutex.withLock {
      val now = timeProvider.now()
      val activeSession = sessionDao.getActiveSession()
      if (activeSession != null) {
          val activeGoalId = dailyGoalDao.getById(activeSession.dailyGoalId)?.goalId
          if (activeGoalId == goalId) {
              // same goal countdown (running or paused) - attach to it as-is,
              // even if it was started before midnight
              return@withLock activeSession
          }
          // a diff session is in progress so end it - as COMPLETED if its time
          // already ran out (it just wasn't recorded yet), else INTERRUPTED
          // with the time it did get credited to its goal
          if (timeProvider.remainingSeconds(activeSession, now) == 0L) {
              markCompleted(activeSession)
          } else {
              markInterrupted(activeSession, now)
          }
      }

      val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
      val dailyGoal = dailyGoalDao.getForGoalAndDate(goalId = goalId, date = today)
          ?: DailyGoal(goalId = goalId, date = today, allocatedSeconds = dailyAllocatedSeconds).let {
              it.copy(id = dailyGoalDao.upsert(it))
          }
      // Only what's left of today's time - doing 20 of 60 minutes, then coming
      // back, continues from 40 minutes rather than starting the hour over.
      val remainingToday = dailyGoal.allocatedSeconds - dailyGoal.completedSeconds
      if (dailyGoal.status == GoalStatus.COMPLETED || remainingToday <= 0L) return@withLock null

      val newSession = Session(
          dailyGoalId = dailyGoal.id,
          startedAt = now,
          runningSince = now,
          allocatedSeconds = remainingToday,
          status = SessionStatus.STARTED
      )
      val id = sessionDao.upsert(newSession)
      newSession.copy(id = id)
  }

    override fun observeActiveSession(): Flow<Session?> = sessionDao.observeActiveSession()

    override suspend fun endSession(session: Session): SessionOutcome? = mutex.withLock {
        val current = sessionDao.getById(session.id) ?: return@withLock null
        if (current.status != SessionStatus.STARTED && current.status != SessionStatus.PAUSED) return@withLock null
        val now = timeProvider.now()
        if (timeProvider.remainingSeconds(current, now) == 0L) {
            markCompleted(current)
            return@withLock SessionOutcome(goalCompleted = true, focusedSeconds = current.allocatedSeconds, remainingTodaySeconds = 0L)
        }
        val focused = markInterrupted(current, now)
        val dailyGoal = dailyGoalDao.getById(current.dailyGoalId)
        SessionOutcome(
            goalCompleted = false,
            focusedSeconds = focused,
            remainingTodaySeconds = dailyGoal?.let { it.allocatedSeconds - it.completedSeconds }?.coerceAtLeast(0L) ?: 0L
        )
    }

    override suspend fun endActiveSessionForGoal(goalId: Long) = mutex.withLock {
        val active = sessionDao.getActiveSession() ?: return@withLock
        if (dailyGoalDao.getById(active.dailyGoalId)?.goalId != goalId) return@withLock
        markInterrupted(active, timeProvider.now())
        Unit
    }

    override suspend fun pauseSession(session: Session): Session = mutex.withLock {
        val current = sessionDao.getById(session.id) ?: return@withLock session
        if (current.status != SessionStatus.STARTED) return@withLock current
        val now = timeProvider.now()
        val additionalElapsed = current.runningSince?.let { Duration.between(it, now).seconds } ?: 0L
        val updatedSession = current.copy(
            status = SessionStatus.PAUSED,
            elapsedSeconds = current.elapsedSeconds + additionalElapsed,
            runningSince = null
        )
        sessionDao.update(updatedSession)
        updatedSession
    }

    override suspend fun resumeSession(session: Session): Session = mutex.withLock {
        val current = sessionDao.getById(session.id) ?: return@withLock session
        if (current.status != SessionStatus.PAUSED) return@withLock current
        val updatedSession = current.copy(
            status = SessionStatus.STARTED,
            runningSince = timeProvider.now()
        )
        sessionDao.update(updatedSession)
        updatedSession
    }

   suspend override fun completeSession(session: Session) = mutex.withLock {
       val current = sessionDao.getById(session.id) ?: return@withLock
       // Already completed/interrupted elsewhere - nothing to do.
       if (current.status != SessionStatus.STARTED && current.status != SessionStatus.PAUSED) return@withLock
       markCompleted(current)
    }

    override suspend fun completeIfExpired() = mutex.withLock {
        val active = sessionDao.getActiveSession() ?: return@withLock
        if (timeProvider.remainingSeconds(active, timeProvider.now()) == 0L) {
            markCompleted(active)
        }
    }

    // Caller must hold [mutex] and pass a fresh copy of the session. Credits
    // the time actually spent to today's goal and returns it.
    private suspend fun markInterrupted(session: Session, now: java.time.Instant): Long {
        val focused = session.allocatedSeconds - timeProvider.remainingSeconds(session, now)
        sessionDao.update(
            session.copy(
                status = SessionStatus.INTERRUPTED,
                endedAt = now,
                runningSince = null,
                elapsedSeconds = focused,
                completedSeconds = focused
            )
        )
        dailyGoalDao.getById(session.dailyGoalId)?.let { dailyGoal ->
            dailyGoalDao.update(
                dailyGoal.copy(completedSeconds = (dailyGoal.completedSeconds + focused).coerceAtMost(dailyGoal.allocatedSeconds))
            )
        }
        return focused
    }

    // Caller must hold [mutex] and pass a fresh copy of the session.
    private suspend fun markCompleted(session: Session) {
        sessionDao.update(
            session.copy(
                status = SessionStatus.COMPLETED,
                runningSince = null,
                endedAt = timeProvider.now(),
                completedSeconds = session.allocatedSeconds
            )
        )
        dailyGoalDao.getById(session.dailyGoalId)?.let { dailyGoal ->
            dailyGoalDao.update(
                dailyGoal.copy(status = GoalStatus.COMPLETED, completedSeconds = dailyGoal.allocatedSeconds)
            )
        }
    }

}
