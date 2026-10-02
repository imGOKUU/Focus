package com.demo.myapplication.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light scheme = the approved mockup, mapped onto M3 slots.
private val FocusLightColorScheme = lightColorScheme(
    background = FocusBackground,
    surface = FocusBackground,
    onBackground = FocusPrimaryText,
    onSurface = FocusPrimaryText,
    primary = FocusPrimaryDark,
    onPrimary = FocusOnPrimaryDark,
    secondary = FocusSecondaryText,
    onSecondary = FocusBackground,
    outline = FocusSoftAccent,
    outlineVariant = FocusSoftAccent,
    surfaceVariant = FocusSoftAccent,
    onSurfaceVariant = FocusSecondaryText
)

// Dark scheme is a placeholder, not something from the mockup — the
// reference only shows the light/cream design. Settings > Appearance
// (spec section 4) offers Dark, so this keeps the app from falling back
// to raw Material purple until a real dark treatment is designed.
private val FocusDarkColorScheme = darkColorScheme(
    background = Color(0xFF14181C),
    surface = Color(0xFF1C2228),
    onBackground = Color(0xFFECEFF2),
    onSurface = Color(0xFFECEFF2),
    primary = Color(0xFF8FB4C7),
    onPrimary = Color(0xFF14181C),
    secondary = Color(0xFF8B939B),
    onSecondary = Color(0xFF14181C),
    outline = Color(0xFF2A323A),
    outlineVariant = Color(0xFF2A323A),
    surfaceVariant = Color(0xFF232B33),
    onSurfaceVariant = Color(0xFF8B939B)
)

@Composable
fun FocusTheme(
    // Not isSystemInDarkTheme(): the approved design only exists in light
    // mode right now, and there's no Settings > Appearance toggle yet either
    // (spec section 25) - following the system setting means the app renders
    // in the undesigned placeholder dark palette on any device/emulator with
    // system dark mode on, which looks nothing like the approved mockup.
    // Revisit once a real dark treatment exists and/or that setting is built.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FocusDarkColorScheme else FocusLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusTypography,
        shapes = FocusShapes,
        content = content
    )
}
