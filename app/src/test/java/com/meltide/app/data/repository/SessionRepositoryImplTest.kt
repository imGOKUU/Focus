package com.meltide.app.data.repository

import com.meltide.app.data.local.entity.GoalStatus
import com.meltide.app.data.local.entity.SessionStatus
import com.meltide.app.testing.FakeDailyGoalDao
import com.meltide.app.testing.FakeSessionDao
import com.meltide.app.testing.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId

/**
 * The rules that decide how a user's time is counted (spec sections 4 and 7):
 * clock-based remaining time, pause/resume, honest crediting when a session
 * ends early or is interrupted, carrying today's remainder into the next
 * session, and completion. A bug here silently misreports someone's day.
 */
class SessionRepositoryImplTest {

    private lateinit var clock: FakeTimeProvider
    private lateinit var dailyGoals: FakeDailyGoalDao
    private lateinit var sessions: FakeSessionDao
    private lateinit var repo: SessionRepositoryImpl

    private val maths = 1L
    private val reading = 2L
    private val hour = 3600L

    @Before
    fun setUp() {
        clock = FakeTimeProvider()
        dailyGoals = FakeDailyGoalDao()
        sessions = FakeSessionDao()
        repo = SessionRepositoryImpl(dailyGoals, sessions, clock)
    }

    private fun remaining(id: Long) = clock.remainingSeconds(sessions.rows.value.getValue(id), clock.now())
    private suspend fun dailyFor(goalId: Long) =
        dailyGoals.getForGoalAndDate(goalId, clock.now().atZone(ZoneId.systemDefault()).toLocalDate())

    // ── Starting ─────────────────────────────────────────────────────────

    @Test
    fun start_createsTodaysRecordAndAFullSession() = runTest {
        val session = repo.startOrAttachSession(maths, hour)!!

        assertEquals(SessionStatus.STARTED, session.status)
        assertEquals(hour, session.allocatedSeconds)
        assertEquals(hour, dailyFor(maths)!!.allocatedSeconds)
        assertEquals(hour, remaining(session.id))
    }

    @Test
    fun start_sameGoalAgain_attachesToTheRunningSession() = runTest {
        val first = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(600)
        val again = repo.startOrAttachSession(maths, hour)!!

        assertEquals(first.id, again.id)
        assertEquals(1, sessions.rows.value.size)
        assertEquals(hour - 600, remaining(first.id))
    }

    // ── Time and pausing ─────────────────────────────────────────────────

    @Test
    fun remainingTime_followsTheClock() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(25 * 60)
        assertEquals(35 * 60L, remaining(s.id))
    }

    @Test
    fun pause_stopsTheClock_andResumeContinuesFromThere() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(600)
        repo.pauseSession(s)
        clock.advanceSeconds(3 * hour) // a long break doesn't eat into the session
        assertEquals(hour - 600, remaining(s.id))

        repo.resumeSession(s)
        clock.advanceSeconds(300)
        assertEquals(hour - 900, remaining(s.id))
    }

    @Test
    fun pauseTwice_countsElapsedTimeOnce() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(600)
        repo.pauseSession(s)
        repo.pauseSession(s)
        assertEquals(600L, sessions.getById(s.id)!!.elapsedSeconds)
    }

    @Test
    fun remainingTime_neverGoesNegative() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(5 * hour)
        assertEquals(0L, remaining(s.id))
    }

    // ── Ending early (honest credit, no completion) ──────────────────────

    @Test
    fun endEarly_creditsTimeSpent_withoutCompletingTheGoal() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(18 * 60)

        val outcome = repo.endSession(s)!!

        assertFalse(outcome.goalCompleted)
        assertEquals(18 * 60L, outcome.focusedSeconds)
        assertEquals(42 * 60L, outcome.remainingTodaySeconds)
        assertEquals(SessionStatus.INTERRUPTED, sessions.getById(s.id)!!.status)
        val daily = dailyFor(maths)!!
        assertEquals(18 * 60L, daily.completedSeconds)
        assertEquals(GoalStatus.PENDING, daily.status)
    }

    @Test
    fun endEarly_whilePaused_creditsOnlyTheRunningTime() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(600)
        repo.pauseSession(s)
        clock.advanceSeconds(hour)

        val outcome = repo.endSession(s)!!

        assertEquals(600L, outcome.focusedSeconds)
    }

    @Test
    fun startingAgainAfterEndingEarly_continuesWithWhatsLeftToday() = runTest {
        val first = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(20 * 60)
        repo.endSession(first)

        val second = repo.startOrAttachSession(maths, hour)!!

        assertTrue(second.id != first.id)
        assertEquals(40 * 60L, second.allocatedSeconds)
    }

    @Test
    fun endSession_afterTimeRanOut_countsAsCompleted() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(hour + 5)

        val outcome = repo.endSession(s)!!

        assertTrue(outcome.goalCompleted)
        assertEquals(GoalStatus.COMPLETED, dailyFor(maths)!!.status)
    }

    @Test
    fun endSession_onAnAlreadyEndedSession_doesNothing() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(600)
        repo.endSession(s)

        assertNull(repo.endSession(s))
        assertEquals(600L, dailyFor(maths)!!.completedSeconds) // not credited twice
    }

    // ── Switching goals ──────────────────────────────────────────────────

    @Test
    fun startingAnotherGoal_interruptsTheFirst_andCreditsItsTime() = runTest {
        val first = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(15 * 60)

        val second = repo.startOrAttachSession(reading, 30 * 60)!!

        assertEquals(SessionStatus.INTERRUPTED, sessions.getById(first.id)!!.status)
        assertEquals(15 * 60L, dailyFor(maths)!!.completedSeconds)
        assertEquals(SessionStatus.STARTED, second.status)
        assertEquals(second.id, sessions.getActiveSession()!!.id)
    }

    @Test
    fun startingAnotherGoal_afterTheFirstRanOut_completesTheFirst() = runTest {
        repo.startOrAttachSession(maths, hour)
        clock.advanceSeconds(hour + 60)

        repo.startOrAttachSession(reading, 30 * 60)

        assertEquals(GoalStatus.COMPLETED, dailyFor(maths)!!.status)
    }

    // ── Completion ───────────────────────────────────────────────────────

    @Test
    fun completeIfExpired_completesSessionAndTodaysGoal() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(hour)

        repo.completeIfExpired()

        assertEquals(SessionStatus.COMPLETED, sessions.getById(s.id)!!.status)
        val daily = dailyFor(maths)!!
        assertEquals(GoalStatus.COMPLETED, daily.status)
        assertEquals(hour, daily.completedSeconds)
        assertNull(sessions.getActiveSession())
    }

    @Test
    fun completeIfExpired_withTimeLeft_changesNothing() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(hour - 1)

        repo.completeIfExpired()

        assertEquals(SessionStatus.STARTED, sessions.getById(s.id)!!.status)
    }

    @Test
    fun aCompletedGoal_cantBeStartedAgainToday() = runTest {
        repo.startOrAttachSession(maths, hour)
        clock.advanceSeconds(hour)
        repo.completeIfExpired()

        assertNull(repo.startOrAttachSession(maths, hour))
    }

    @Test
    fun aLatePauseFromAStaleCopy_neverRevivesACompletedSession() = runTest {
        val staleCopy = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(hour)
        repo.completeIfExpired()

        val result = repo.pauseSession(staleCopy)

        assertEquals(SessionStatus.COMPLETED, result.status)
        assertEquals(SessionStatus.COMPLETED, sessions.getById(staleCopy.id)!!.status)
    }

    // ── Deleting a goal mid-session ──────────────────────────────────────

    @Test
    fun endActiveSessionForGoal_endsOnlyThatGoalsSession() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(300)

        repo.endActiveSessionForGoal(reading) // a different goal: no effect
        assertNotNull(sessions.getActiveSession())

        repo.endActiveSessionForGoal(maths)
        assertNull(sessions.getActiveSession())
        assertEquals(SessionStatus.INTERRUPTED, sessions.getById(s.id)!!.status)
    }

    // ── Midnight ─────────────────────────────────────────────────────────

    @Test
    fun aNewDay_getsItsOwnRecordWithTheFullTime() = runTest {
        val s = repo.startOrAttachSession(maths, hour)!!
        clock.advanceSeconds(20 * 60)
        repo.endSession(s)

        clock.advanceSeconds(24 * hour)
        val tomorrow = repo.startOrAttachSession(maths, hour)!!

        assertEquals(hour, tomorrow.allocatedSeconds)
        assertEquals(2, dailyGoals.rows.value.size)
    }
}
