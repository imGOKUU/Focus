package com.meltide.app.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun shortDuration_readsAtAGlance() {
        assertEquals("1h 30m", 5400L.toShortDuration())
        assertEquals("2h", 7200L.toShortDuration())
        assertEquals("45m", 2700L.toShortDuration())
        assertEquals("0m", 0L.toShortDuration())
    }

    @Test
    fun shortDuration_underAMinute_isNeverShownInSeconds() {
        assertEquals("<1m", 14L.toShortDuration())
        assertEquals("<1m", 14L.toShortDuration(roundUp = false))
    }

    @Test
    fun timeLeft_roundsUp_butTimeGiven_roundsDown() {
        // 1:49 left reads "2m" (never an understated "1m")...
        assertEquals("2m", 109L.toShortDuration())
        // ...while 1:49 already given reads "1m" (never overstated).
        assertEquals("1m", 109L.toShortDuration(roundUp = false))
    }

    @Test
    fun clock_showsHoursOnlyWhenThereAreAny() {
        assertEquals("41:23", (41 * 60 + 23L).toClock())
        assertEquals("1:02:03", 3723L.toClock())
    }
}
