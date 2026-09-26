package com.demo.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring daily intention - "Mathematics, 1 hour" - not a task.
 * See spec section 6.
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dailyDurationSeconds: Long,
    val icon: String,
    val color: String,
    val isActive: Boolean = true
)
