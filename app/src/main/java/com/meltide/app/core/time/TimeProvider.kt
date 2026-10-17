package com.meltide.app.core.time

import com.meltide.app.data.local.entity.Session
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Thin wrapper around Instant.now(). Exists so CalculateRemainingTime
 * and session logic (Day 2) can be unit-tested with a fake clock instead
 * of depending on real wall-clock time - critical since spec section 7
 * requires remaining = sessionEndTime - currentTime to hold even when
 * system time changes.
 */
interface TimeProvider {
    fun now(): Instant
    fun remainingSeconds(session: Session, now: Instant): Long {
        val timeUsed = session.elapsedSeconds + (session.runningSince?.let { Duration.between(it, now).seconds } ?: 0L)
        return max(session.allocatedSeconds - timeUsed, 0)
    }
}

@Singleton
class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()
}
