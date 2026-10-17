package com.meltide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Today's instance of a recurring Goal. completedSeconds is honest -
 * ending a session early does NOT mark this COMPLETED (spec section 4,
 * "Ending Early": remaining time is preserved, no partial credit).
 */
// goalId+date: "today's record for this goal"; date: Today/History by day.
@Entity(
    tableName = "daily_goals",
    indices = [Index(value = ["goalId", "date"]), Index(value = ["date"])]
)
data class DailyGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val date: LocalDate,
    val allocatedSeconds: Long,
    val completedSeconds: Long = 0,
    val status: GoalStatus = GoalStatus.PENDING
)
