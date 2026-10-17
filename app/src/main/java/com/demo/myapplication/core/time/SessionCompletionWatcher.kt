package com.demo.myapplication.core.time

import com.demo.myapplication.data.local.dao.SessionDao
import com.demo.myapplication.data.local.entity.SessionStatus
import com.demo.myapplication.di.ApplicationScope
import com.demo.myapplication.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Marks the active session COMPLETED when its time runs out, whether or not
 * the Focus screen is open. Without this, a session left running (user went
 * Home, or the process was killed) stayed STARTED forever, and the blocking
 * service kept blocking apps long after the goal's time was up.
 *
 * Runs for the life of the process, which is alive whenever the app or the
 * blocking service is. On process start (incl. after reboot), the first
 * emission completes any session that expired while nothing was running.
 */
@Singleton
class SessionCompletionWatcher @Inject constructor(
    private val sessionDao: SessionDao,
    private val sessionRepository: SessionRepository,
    private val timeProvider: TimeProvider,
    private val sessionEndAlarm: SessionEndAlarm,
    @ApplicationScope private val applicationScope: CoroutineScope
) {
    fun start() {
        applicationScope.launch {
            // collectLatest restarts the wait whenever the session changes
            // (paused, resumed, completed, replaced).
            sessionDao.observeActiveSession().collectLatest { session ->
                // Backstop for when this process is frozen in the background.
                if (session?.status == SessionStatus.STARTED) {
                    val remainingNow = timeProvider.remainingSeconds(session, timeProvider.now())
                    sessionEndAlarm.schedule(timeProvider.now().toEpochMilli() + remainingNow * 1000)
                } else {
                    sessionEndAlarm.cancel()
                }
                if (session == null) return@collectLatest
                var remaining = timeProvider.remainingSeconds(session, timeProvider.now())
                while (remaining > 0 && session.status == SessionStatus.STARTED) {
                    // Wake at least once a minute: delay() doesn't advance during
                    // deep sleep, so a single long delay could fire far too late.
                    delay(minOf(remaining, MAX_WAIT_SECONDS).seconds)
                    remaining = timeProvider.remainingSeconds(session, timeProvider.now())
                }
                if (remaining == 0L) sessionRepository.completeIfExpired()
            }
        }
    }

    private companion object {
        const val MAX_WAIT_SECONDS = 60L
    }
}
