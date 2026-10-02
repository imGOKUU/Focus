package com.demo.myapplication.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Two weights only, matching the mockup: Normal for body/secondary text,
// Medium for headings and anything meant to draw the eye.

val FocusTypography = Typography(
    headlineLarge = TextStyle(         // goal-name headline (GoalConfigScreen, e.g. "Mathematics")
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(            // "Today", "Mathematics" (screen-level titles)
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(           // goal row names
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(            // "Give time to what matters..."
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelSmall = TextStyle(            // durations, timestamps, nav labels
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

// The countdown is the hero (spec section 2) — it isn't one of Material's
// named type slots, so it's a standalone style rather than forced into
// displayLarge. Tabular figures keep the digits from jittering in width
// as they count down.
val TimerDisplayLarge = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = 44.sp,
    lineHeight = 48.sp,
    fontFeatureSettings = "tnum"
)

val TimerDisplayMinimal = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 30.sp,
    lineHeight = 34.sp,
    fontFeatureSettings = "tnum"
)

// Screen-specific headlines that don't share a size with headlineLarge or
// each other - named here instead of local .copy() overrides so the values
// live in one place and match the design canvas exactly (29/24, both 1.25x
// line-height, both SemiBold - the weight the design canvas actually uses,
// not Bold).
val WelcomeHeadline = TextStyle(       // "Your time.\nYour rules." (WelcomeScreen)
    fontWeight = FontWeight.SemiBold,
    fontSize = 29.sp,
    lineHeight = 36.sp
)

val AddGoalsHeadline = TextStyle(      // "What do you want\nto give time to?" (AddGoalsScreen)
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp
)

// CTA button label ("Get Started" / "Continue" / "Save") - SemiBold/15sp
// per the design canvas, distinct from titleMedium (goal row names).
val ButtonLabel = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp
)
