package com.demo.myapplication.data.repository

import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.GoalStatus
import com.demo.myapplication.data.local.entity.Session
import com.demo.myapplication.data.local.entity.SessionStatus
import com.demo.myapplication.domain.repository.SessionRepository
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
    ): Session = mutex.withLock {
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
          if (timeProvider.remainingSeconds(activeSession, now) == 0L) {
              markCompleted(activeSession)
          } else {
              sessionDao.update(
                  activeSession.copy(
                      status = SessionStatus.INTERRUPTED,
                      endedAt = now
                  )
              )
          }
      }

      val today = now.atZone(ZoneId.systemDefault()).toLocalDate()
      val dailyGoalId = dailyGoalDao.getForGoalAndDate(goalId = goalId, date = today)?.id
          ?: dailyGoalDao.upsert(
              DailyGoal(
                  goalId = goalId,
                  date = today,
                  allocatedSeconds = dailyAllocatedSeconds
              )
          )
      // make a new session
      val newSession = Session(
          dailyGoalId = dailyGoalId,
          startedAt = now,
          runningSince = now,
          allocatedSeconds = dailyAllocatedSeconds,
          status = SessionStatus.STARTED
      )
      val id = sessionDao.upsert(newSession)
      newSession.copy(id = id)
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

    // Caller must hold [mutex] and pass a fresh copy of the session.
    private suspend fun markCompleted(session: Session) {
        sessionDao.update(session.copy(status = SessionStatus.COMPLETED, runningSince = null))
        dailyGoalDao.getById(session.dailyGoalId)?.let { dailyGoal ->
            dailyGoalDao.update(
                dailyGoal.copy(status = GoalStatus.COMPLETED, completedSeconds = dailyGoal.allocatedSeconds)
            )
        }
    }

}
