package com.demo.myapplication.data.local

import androidx.room.TypeConverter
import com.demo.myapplication.data.local.entity.GoalStatus
import com.demo.myapplication.data.local.entity.SessionStatus
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromGoalStatus(value: String?): GoalStatus? = value?.let { GoalStatus.valueOf(it) }

    @TypeConverter
    fun goalStatusToString(status: GoalStatus?): String? = status?.name

    @TypeConverter
    fun fromSessionStatus(value: String?): SessionStatus? = value?.let { SessionStatus.valueOf(it) }

    @TypeConverter
    fun sessionStatusToString(status: SessionStatus?): String? = status?.name
}
