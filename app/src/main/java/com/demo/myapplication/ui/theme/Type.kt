package com.demo.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Two weights only, matching the mockup: Normal for body/secondary text,
// Medium for headings and anything meant to draw the eye.

val FocusTypography = Typography(
    headlineLarge = TextStyle(         // "Your time. Your rules." / "What do you want to give time to?"
        fontWeight = FontWeight.Medium,
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
    lineHeight = 48.sp
)

val TimerDisplayMinimal = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 30.sp,
    lineHeight = 34.sp
)
