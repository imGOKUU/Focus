package com.demo.myapplication.core.time

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around Instant.now(). Exists so CalculateRemainingTime
 * and session logic (Day 2) can be unit-tested with a fake clock instead
 * of depending on real wall-clock time - critical since spec section 7
 * requires remaining = sessionEndTime - currentTime to hold even when
 * system time changes.
 */
interface TimeProvider {
    fun now(): Instant
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun now(): Instant = Instant.now()
}
