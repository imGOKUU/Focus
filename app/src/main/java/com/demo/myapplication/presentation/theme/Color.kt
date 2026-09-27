package com.demo.myapplication.presentation.theme

import androidx.compose.ui.graphics.Color


val FocusBackground = Color(0xFFF7F5EF)   // cream page background, every screen
val FocusPrimaryText = Color(0xFF18212B)  // headings, timer digits, primary labels
val FocusSecondaryText = Color(0xFF66707A) // subtitles, muted labels, timestamps
val FocusPrimaryDark = Color(0xFF263640)   // filled buttons, active nav icon, dark chrome
val FocusSoftAccent = Color(0xFFDDE5EC)    // dividers, row separators, ice/track fill
val FocusOnPrimaryDark = Color(0xFFF7F5EF) // text/icons on FocusPrimaryDark (cream, not stark white)

// Minimum-mode (immersive) screen — near-black, intentionally low contrast.
// These are fixed regardless of light/dark app setting; see Theme.kt note.
val FocusMinimalBackground = Color(0xFF0A0A0A)
val FocusMinimalText = Color(0xFF5A5A5A)
val FocusMinimalTextDim = Color(0xFF3A3A3A)

// Blocked-app interception screen — dark backdrop, inverse of the normal theme.
val FocusBlockedBackground = Color(0xFF14171A)
val FocusBlockedText = Color(0xFFF2F2F0)
val FocusBlockedSecondaryText = Color(0xFF9AA0A6)

// Per-goal accent chips (the colored icon squares next to each goal name).
// One ramp per category shown in the mockup: Mathematics = teal, DSA = blue,
// Projects = green, Reading = purple. New goals cycle through these four
// until Day 4 decides whether users pick an icon/color explicitly.
data class GoalAccent(val chipBackground: Color, val onChip: Color)

object GoalAccents {
    val Teal = GoalAccent(chipBackground = Color(0xFFCDE8E1), onChip = Color(0xFF2F6F63))
    val Blue = GoalAccent(chipBackground = Color(0xFFD3E3F7), onChip = Color(0xFF35619E))
    val Green = GoalAccent(chipBackground = Color(0xFFD9EFD4), onChip = Color(0xFF3E8B45))
    val Purple = GoalAccent(chipBackground = Color(0xFFE3DAF6), onChip = Color(0xFF6B4FA0))

    val defaultCycle = listOf(Teal, Blue, Green, Purple)

    fun forIndex(index: Int): GoalAccent = defaultCycle[index.mod(defaultCycle.size)]
}

// Landing screen illustration — flat sun-and-mountain motif from the onboarding mockup.
val FocusSunAccent = Color(0xFFF0B98F)
val FocusMountainFar = Color(0xFFCBD5DE)
val FocusMountainMid = Color(0xFFA7B2BD)
val FocusMountainNear = Color(0xFF7C8794)
