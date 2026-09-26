package com.demo.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Blocking is per-goal, not global: Mathematics can block YouTube
 * while Projects allows it.
 */
@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val packageName: String,
    val appName: String
)
