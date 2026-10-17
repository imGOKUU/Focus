package com.demo.myapplication.presentation.components

/**
 * "1h 30m", "2h", "45m", "<1m" - for durations read at a glance. Time still
 * to go rounds a part minute up (1:49 left reads "2m", never "1m"); time
 * already given passes roundUp = false so it's never overstated.
 */
fun Long.toShortDuration(roundUp: Boolean = true): String {
    val partMinute = this >= 60 && this % 60 != 0L
    val rounded = if (roundUp && partMinute) (this / 60 + 1) * 60 else this
    val h = rounded / 3600
    val m = (rounded % 3600) / 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        m > 0 -> "${m}m"
        // Seconds read as noise next to "of 1h" and "3m planned"; under a
        // minute is shown as such everywhere a short duration appears.
        this > 0 -> "<1m"
        else -> "0m"
    }
}

/** "1:02:03" or "41:23" - a live countdown, hours only when there are any. */
fun Long.toClock(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
