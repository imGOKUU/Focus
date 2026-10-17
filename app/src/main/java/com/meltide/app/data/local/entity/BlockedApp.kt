package com.meltide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Blocking is per-goal, not global: Mathematics can block YouTube
 * while Projects allows it.
 */
@Entity(tableName = "blocked_apps", indices = [Index(value = ["goalId"])])
data class BlockedApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val packageName: String,
    val appName: String
)
