package com.meltide.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
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
    val ScreenTopGap = 8.dp
    val HeaderGap = 20.dp

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
 * Shared elevated container for a screen section: the app-wide card radius
 * (16dp), raised with the shared depth system (see Depth.kt). Content is
 * clipped to the shape so a scrolling list never paints past the corners.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    depth: Depth = Depth.Medium,
    content: @Composable () -> Unit
) {
    FocusSurface(modifier = modifier, depth = depth) {
        content()
    }
}

/**
 * [SectionLabel] with an optional live summary on the right (e.g. "1h 30m a
 * day", "3 selected"), so a section's current value is readable at a glance.
 */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(text, modifier = Modifier.weight(1f))
        if (trailing != null) {
            AnimatedContent(
                targetState = trailing,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "sectionTrailing"
            ) { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
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
