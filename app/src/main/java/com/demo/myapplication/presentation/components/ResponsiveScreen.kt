package com.demo.myapplication.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Target platform is phone AND tablet (not phone-only - see memory/decisions
 * log). Every onboarding/goal screen is built with Modifier.weight() to
 * distribute space between fixed-size elements and flexible gaps, which only
 * reads right at the width/height it was designed around - stretched across
 * a full tablet viewport, weighted gaps balloon and the layout looks broken.
 *
 * Rather than pretending every device is phone-sized (that was the earlier,
 * wrong approach - see DECISIONS.md), this follows Material's window-size-class
 * pattern: compact widths (phones) get the full available width, same as
 * always; medium/expanded widths (tablets) get a wider reading-width column
 * (600dp) that's still centered rather than stretched edge-to-edge, so the
 * extra space is used - not left as a small phone card floating in a void,
 * but also not stretched so far that the design's spacing breaks.
 *
 * The height cap serves the same goal for the vertical axis: it keeps
 * weighted spacers from ballooning on a tall tablet screen, while still
 * being wide/tall enough to feel like a genuine adapted layout rather than
 * a shrunk-down phone screen.
 */
@Composable
fun ResponsiveScreen(
    modifier: Modifier = Modifier,
    // For anything the inner column needs beyond sizing/padding, e.g.
    // Modifier.verticalScroll(...) on a screen long enough to need it.
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val compactWidthBreakpoint = 600.dp
        val tabletContentWidth = 600.dp
        val contentWidth = if (maxWidth < compactWidthBreakpoint) maxWidth else tabletContentWidth

        Column(
            modifier = Modifier
                .width(contentWidth)
                .heightIn(max = 1000.dp)
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
                .then(contentModifier),
            content = content
        )
    }
}
