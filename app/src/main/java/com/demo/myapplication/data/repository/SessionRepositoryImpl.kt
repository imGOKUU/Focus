package com.demo.myapplication.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.GoalStatus
import com.demo.myapplication.data.local.entity.Session
import com.demo.myapplication.data.local.entity.SessionStatus
import com.demo.myapplication.domain.repository.SessionRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
private val dailyGoalDao: DailyGoalDao,
    private val sessionDao: SessionDao,
    private val timeProvider: TimeProvider
): SessionRepository{
  @RequiresApi(Build.VERSION_CODES.O)
  suspend override fun startOrAttachSession(
        goalId: Long,
        dailyAllocatedSeconds: Long
    ): Session {
      val dailyGoal = dailyGoalDao.getForGoalAndDate(
          goalId = goalId, date = timeProvider.now().atZone(
              ZoneId.systemDefault()
          ).toLocalDate()
      )
      if (dailyGoal != null) {
          val isActiveSession = sessionDao.getActiveSession()
          if (isActiveSession == null) {
              // make a new session
              val newSession = Session(
                  dailyGoalId = dailyGoal.id,
                  startedAt = timeProvider.now(),
                  allocatedSeconds = dailyAllocatedSeconds,
                  runningSince = timeProvider.now(),
                  status = SessionStatus.STARTED
              )
              val id = sessionDao.upsert(newSession)
              return newSession.copy(id = id)
          } else if (isActiveSession.dailyGoalId == dailyGoal.id) {
              //same goal countdown (running or paused) - attach to it as-is
              return isActiveSession
          } else {
//a diff session is in progress so end it
              sessionDao.update(
                  isActiveSession.copy(
                      status = SessionStatus.INTERRUPTED,
                      endedAt = timeProvider.now()
                  )
              )
              // and create a new session
              val newSession = Session(
                  dailyGoalId = dailyGoal.id,
                  startedAt = timeProvider.now(),
                  runningSince = timeProvider.now(),
                  allocatedSeconds = dailyAllocatedSeconds,
                  status = SessionStatus.STARTED
              )
              val id = sessionDao.upsert(newSession)
              return newSession.copy(id = id)

          }
      } else {
         val newId = dailyGoalDao.upsert(
              DailyGoal(
                  goalId = goalId,
                  date = timeProvider.now().atZone(
                      ZoneId.systemDefault()
                  ).toLocalDate(),
                  allocatedSeconds = dailyAllocatedSeconds
              )
          )
          val isActiveSession = sessionDao.getActiveSession()
         if(isActiveSession!=null&& isActiveSession.dailyGoalId != newId){
             sessionDao.update(
                 isActiveSession.copy(
                     status = SessionStatus.INTERRUPTED,
                     endedAt = timeProvider.now()
                 )
             )
         }
          val newSession = Session(
              dailyGoalId = newId,
              startedAt = timeProvider.now(),
              runningSince = timeProvider.now(),
              allocatedSeconds = dailyAllocatedSeconds,
              status = SessionStatus.STARTED
          )
          val id = sessionDao.upsert(newSession)
          return newSession.copy(id = id)
      }
  }

    override suspend fun pauseSession(session: Session): Session {
        if (session.status != SessionStatus.STARTED) return session
        val now = timeProvider.now()
        val additionalElapsed = session.runningSince?.let { Duration.between(it, now).seconds } ?: 0L
        val updatedSession = session.copy(
            status = SessionStatus.PAUSED,
            elapsedSeconds = session.elapsedSeconds + additionalElapsed,
            runningSince = null
        )
        sessionDao.update(updatedSession)
        return updatedSession
    }

    override suspend fun resumeSession(session: Session): Session {
        if (session.status != SessionStatus.PAUSED) return session
        val updatedSession = session.copy(
            status = SessionStatus.STARTED,
            runningSince = timeProvider.now()
        )
        sessionDao.update(updatedSession)
        return updatedSession
    }


   suspend override fun completeSession(session: Session) {
       val updatedSession = session.copy(status = SessionStatus.COMPLETED, runningSince = null)
        sessionDao.update(
            updatedSession
        )
        dailyGoalDao.getById(session.dailyGoalId)?.let { dailyGoal ->
            dailyGoalDao.update(
                dailyGoal.copy(status = GoalStatus.COMPLETED, completedSeconds = dailyGoal.allocatedSeconds)
            )
        }
    }

}