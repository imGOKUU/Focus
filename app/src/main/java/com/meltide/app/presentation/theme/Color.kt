package com.meltide.app.presentation.theme

import androidx.compose.ui.graphics.Color


val FocusBackground = Color(0xFFF7F5EF)   // cream page background, every screen
val FocusPrimaryText = Color(0xFF18212B)  // headings, timer digits, primary labels
val FocusSecondaryText = Color(0xFF66707A) // subtitles, muted labels, timestamps
val FocusPrimaryDark = Color(0xFF263640)   // filled buttons, active nav icon, dark chrome
val FocusSoftAccent = Color(0xFFDDE5EC)    // dividers, row separators, track fill
val FocusOnPrimaryDark = Color(0xFFF7F5EF) // text/icons on FocusPrimaryDark (cream, not stark white)
val FocusCardSurface = Color(0xFFFFFFFF)   // elevated cards sitting on the cream background

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
    // Warm pair that sits naturally in the cream palette.
    val Amber = GoalAccent(chipBackground = Color(0xFFF6E3C6), onChip = Color(0xFF9A6420))
    val Rose = GoalAccent(chipBackground = Color(0xFFF4DAD6), onChip = Color(0xFFA34D45))

    val defaultCycle = listOf(Teal, Blue, Green, Purple)

    fun forIndex(index: Int): GoalAccent = defaultCycle[index.mod(defaultCycle.size)]
}

// Welcome screen illustration — flat sun-and-mountain motif. Exact values
// from the design canvas's inline CSS, not approximated.
val FocusSunAccent = Color(0xFFF0B99A)
val FocusMountainFar = Color(0xFFC9D3DA)
val FocusMountainMid = Color(0xFFA8B7C2)
val FocusMountainNear = Color(0xFF8494A0)

// Active session illustration — the melting ice covering the goal-accent-
// tinted can. No design mockup exists for this screen yet (spec sections
// 13-15 describe it in prose only) - picked to read as "ice/frost" against
// the cream background without needing a mockup to match exactly.
val FocusIceAccent = Color(0xFFEAF3F7)

// Depth system (components/Depth.kt). Shadows are tinted warm brown rather
// than black so they sit naturally on the cream background instead of
// reading as grey smudges; the card/inset tones are the same cream family,
// one step lighter (raised) and one step deeper (pressed in).
val FocusShadow = Color(0xFF5B4330)
val FocusCardSurfaceLow = Color(0xFFFBF8F2)   // bottom stop of a card's top-to-bottom gradient
val FocusInsetSurface = Color(0xFFF1ECE2)     // pressed-in wells (wheel band, chips)
val FocusBackgroundDeep = Color(0xFFEFE8DC)   // warm glow at the page's lower edge

// Destructive actions (End session, Delete goal) - a warm brick red from the
// Rose goal ramp rather than Material's stock error red, so it stays in the
// palette while still reading clearly as "careful".
val FocusDanger = Color(0xFFA34D45)
