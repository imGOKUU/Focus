package com.meltide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One countdown attempt. startedAt/endedAt are the source of truth for
 * "remaining" - see spec section 7. Never derive remaining time from an
 * in-memory counter.
 */
// dailyGoalId: a day's sessions; status: "the active session" lookups.
@Entity(
    tableName = "sessions",
    indices = [Index(value = ["dailyGoalId"]), Index(value = ["status"])]
)
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyGoalId: Long,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val allocatedSeconds: Long,
    val elapsedSeconds: Long=0,
    val runningSince: Instant?=null,
    val completedSeconds: Long = 0,
    val status: SessionStatus = SessionStatus.STARTED
)
