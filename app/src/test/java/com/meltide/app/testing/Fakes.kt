package com.meltide.app.testing

import com.meltide.app.core.time.TimeProvider
import com.meltide.app.data.local.dao.DailyGoalDao
import com.meltide.app.data.local.dao.SessionDao
import com.meltide.app.data.local.entity.DailyGoal
import com.meltide.app.data.local.entity.Session
import com.meltide.app.data.local.entity.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

/** A clock tests move by hand - "an hour passes" takes no time. */
class FakeTimeProvider(var current: Instant = Instant.parse("2026-10-17T09:00:00Z")) : TimeProvider {
    override fun now(): Instant = current
    fun advanceSeconds(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}

/** In-memory DailyGoalDao with the same contract as the Room one. */
class FakeDailyGoalDao : DailyGoalDao {
    val rows = MutableStateFlow<Map<Long, DailyGoal>>(emptyMap())
    private var nextId = 1L

    override fun observeForDate(date: LocalDate): Flow<List<DailyGoal>> =
        rows.map { all -> all.values.filter { it.date == date }.sortedBy { it.id } }

    override fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<DailyGoal>> =
        rows.map { all -> all.values.filter { it.date in start..end }.sortedWith(compareBy({ it.date }, { it.id })) }

    override suspend fun getById(id: Long): DailyGoal? = rows.value[id]

    override suspend fun upsert(dailyGoal: DailyGoal): Long {
        val id = if (dailyGoal.id == 0L) nextId++ else dailyGoal.id
        rows.value = rows.value + (id to dailyGoal.copy(id = id))
        return id
    }

    override suspend fun update(dailyGoal: DailyGoal) {
        if (dailyGoal.id in rows.value) rows.value = rows.value + (dailyGoal.id to dailyGoal)
    }

    override suspend fun getForGoalAndDate(goalId: Long, date: LocalDate): DailyGoal? =
        rows.value.values.firstOrNull { it.goalId == goalId && it.date == date }

    override suspend fun countForGoalOnDate(goalId: Long, date: LocalDate): Int =
        rows.value.values.count { it.goalId == goalId && it.date == date }
}

/** In-memory SessionDao with the same contract as the Room one. */
class FakeSessionDao : SessionDao {
    val rows = MutableStateFlow<Map<Long, Session>>(emptyMap())
    private var nextId = 1L

    private fun active(all: Map<Long, Session>): Session? =
        all.values.firstOrNull { it.status == SessionStatus.STARTED || it.status == SessionStatus.PAUSED }

    override fun observeForDailyGoal(dailyGoalId: Long): Flow<List<Session>> =
        rows.map { all -> all.values.filter { it.dailyGoalId == dailyGoalId }.sortedByDescending { it.startedAt } }

    override suspend fun getActiveSession(): Session? = active(rows.value)

    override fun observeActiveSession(): Flow<Session?> = rows.map { active(it) }

    override suspend fun getById(id: Long): Session? = rows.value[id]

    override suspend fun upsert(session: Session): Long {
        val id = if (session.id == 0L) nextId++ else session.id
        rows.value = rows.value + (id to session.copy(id = id))
        return id
    }

    override suspend fun update(session: Session) {
        if (session.id in rows.value) rows.value = rows.value + (session.id to session)
    }
}
