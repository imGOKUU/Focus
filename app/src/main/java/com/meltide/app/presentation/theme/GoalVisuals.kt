package com.meltide.app.presentation.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The goal's icon and color are stored as plain keys on Goal (icon, color)
 * so the database never depends on Compose types. This is the one place
 * those keys map to what's drawn - every screen goes through it.
 */
object GoalVisuals {
    val icons: List<Pair<String, ImageVector>> = listOf(
        "bar_chart" to Icons.Outlined.BarChart,
        "calculate" to Icons.Outlined.Calculate,
        "code" to Icons.Outlined.Code,
        "computer" to Icons.Outlined.Computer,
        "menu_book" to Icons.AutoMirrored.Outlined.MenuBook,
        "science" to Icons.Outlined.Science,
        "translate" to Icons.Outlined.Translate,
        "edit_note" to Icons.Outlined.EditNote,
        "brush" to Icons.Outlined.Brush,
        "music_note" to Icons.Outlined.MusicNote,
        "fitness_center" to Icons.Outlined.FitnessCenter,
        "self_improvement" to Icons.Outlined.SelfImprovement
    )

    val accents: List<Pair<String, GoalAccent>> = listOf(
        "teal" to GoalAccents.Teal,
        "blue" to GoalAccents.Blue,
        "green" to GoalAccents.Green,
        "purple" to GoalAccents.Purple,
        "amber" to GoalAccents.Amber,
        "rose" to GoalAccents.Rose
    )

    const val DefaultIcon = "bar_chart"

    fun icon(key: String): ImageVector =
        icons.firstOrNull { it.first == key }?.second ?: Icons.Outlined.BarChart

    fun accent(key: String): GoalAccent =
        accents.firstOrNull { it.first == key }?.second ?: GoalAccents.Teal

    /** A new goal's color: the next one along, so goals don't all look alike. */
    fun accentKeyFor(goalCount: Int): String = accents[goalCount.mod(accents.size)].first
}
