package com.demo.myapplication.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared layout tokens for the card-based screens (GoalConfig, Home), so
 * sections, cards and list rows line up identically across the app.
 */
object FocusLayout {
    val CardHorizontalPadding = 16.dp
    val SectionLabelGap = 10.dp
    val SectionGap = 28.dp

    // Below this height for a screen's flexible middle region (phone
    // landscape, split screen), sections sit side by side instead of stacked.
    val SideBySideHeightThreshold = 440.dp

    // List rows inside a SectionCard: leading 40dp tile, then the label.
    val RowMinHeight = 64.dp
    val RowIconSize = 40.dp
    val RowIconLabelGap = 14.dp
}

/**
 * The app draws edge-to-edge (targetSdk 35+). Screens pass this to
 * ResponsiveScreen's contentModifier (via windowInsetsPadding) so their top
 * and bottom content stays clear of the status bar, navigation bar and cutout.
 */
@Composable
fun screenVerticalInsets(): WindowInsets =
    WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .only(WindowInsetsSides.Vertical)

/**
 * Shared elevated container for a screen section: theme large radius (20dp),
 * card surface, and a soft low-opacity shadow. Content is clipped to the
 * shape so a scrolling list never paints past the rounded corners.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    val shadowTint = MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = shadowTint.copy(alpha = 0.05f),
                spotColor = shadowTint.copy(alpha = 0.14f)
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        content()
    }
}

/** Small all-caps heading that sits above a SectionCard. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/** Divider between rows in a SectionCard list, inset to line up with the row labels. */
@Composable
fun CardRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = FocusLayout.CardHorizontalPadding + FocusLayout.RowIconSize + FocusLayout.RowIconLabelGap,
            end = FocusLayout.CardHorizontalPadding
        ),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    )
}
