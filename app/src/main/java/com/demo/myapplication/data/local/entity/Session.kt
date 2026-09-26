package com.demo.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * One countdown attempt. startedAt/endedAt are the source of truth for
 * "remaining" - see spec section 7. Never derive remaining time from an
 * in-memory counter.
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dailyGoalId: Long,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val allocatedSeconds: Long,
    val completedSeconds: Long = 0,
    val status: SessionStatus = SessionStatus.STARTED
)
