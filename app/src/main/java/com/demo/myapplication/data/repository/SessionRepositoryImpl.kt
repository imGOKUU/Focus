package com.demo.myapplication.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.demo.myapplication.core.time.TimeProvider
import com.demo.myapplication.data.local.dao.DailyGoalDao
import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.DailyGoal
import com.demo.myapplication.data.local.entity.Session
import com.demo.myapplication.data.local.entity.SessionStatus
import com.demo.myapplication.domain.repository.SessionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.Long
import kotlin.concurrent.timer

class SessionRepositoryImpl @Inject constructor(
private val dailyGoalDao: DailyGoalDao,
    private val sessionDao: SessionDao,
    private val timerProvider: TimeProvider
): SessionRepository{
  @RequiresApi(Build.VERSION_CODES.O)
  suspend override fun startOrResumeSession(
        goalId: Long,
        dailyAllocatedSeconds: Long
    ): Session {
      val dailyGoal = dailyGoalDao.getForGoalAndDate(
          goalId = goalId, date = timerProvider.now().atZone(
              ZoneId.systemDefault()
          ).toLocalDate()
      )
      if (dailyGoal != null) {
          val isActiveSession = sessionDao.getActiveSession()
          if (isActiveSession == null) {
              // make a new session
              val newSession = Session(
                  dailyGoalId = dailyGoal.id,
                  startedAt = timerProvider.now(),
                  allocatedSeconds = dailyAllocatedSeconds,
                  status = SessionStatus.STARTED
              )
              val id = sessionDao.upsert(newSession)
              return newSession.copy(id = id)
          } else if (isActiveSession.dailyGoalId == dailyGoal.id) {
              //same goal countdown return as it is
              return isActiveSession
          } else {
//a diff session is in progress so end it
              sessionDao.update(
                  isActiveSession.copy(
                      status = SessionStatus.INTERRUPTED,
                      endedAt = timerProvider.now()
                  )
              )
              // and create a new session
              val newSession = Session(
                  dailyGoalId = dailyGoal.id,
                  startedAt = timerProvider.now(),
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
                  date = timerProvider.now().atZone(
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
                     endedAt = timerProvider.now()
                 )
             )
         }
          val newSession = Session(
              dailyGoalId = newId,
              startedAt = timerProvider.now(),
              allocatedSeconds = dailyAllocatedSeconds,
              status = SessionStatus.STARTED
          )
          val id = sessionDao.upsert(newSession)
          return newSession.copy(id = id)
      }
  }


   suspend override fun completeSession(session: Session) {
       val updatedSession = session.copy(status = SessionStatus.COMPLETED)
        sessionDao.update(
            updatedSession
        )
    }

}