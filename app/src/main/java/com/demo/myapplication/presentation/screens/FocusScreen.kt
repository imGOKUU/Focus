package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.theme.ButtonLabel
import com.demo.myapplication.presentation.theme.ButtonPillShape
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalAccents
import com.demo.myapplication.presentation.theme.TimerDisplayLarge


@Composable
fun FocusScreen(
    goal: GoalListItem?,
    remainingSeconds: Long = 0L,
    totalSeconds: Long = 1L,
    blockedAppsCount: Int = 0,
    accent: GoalAccent = GoalAccents.Teal,
    isPaused: Boolean = false,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onBack:()->Unit,
) {
    val safeTotal = totalSeconds.coerceAtLeast(1L)
    // totalSeconds defaults to 1L (UiState's placeholder) until FocusViewModel.start()'s
    // async session fetch resolves - a fresh FocusViewModel is created on every nav into
    // this screen, so that unloaded window recurs each time. Without this guard,
    // remainingSeconds=0 / totalSeconds=1 computes progress=0 for that first frame, and
    // since it's the illustration's very first composition there's no prior value for
    // animateFloatAsState to tween from - it snaps straight to "fully melted" before the
    // real (near-full) value arrives a moment later, instead of smoothly animating in.
    val sessionLoaded = totalSeconds > 1L
    val progressRemaining = if (sessionLoaded) {
        (remainingSeconds.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    ResponsiveScreen {
        IconButton(
            onClick = {
                println("Clicked: Focus Back button")
                onBack()
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = SpacingXS)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(SpacingM))

        Text(
            text = goal?.name.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingM)
        )

        Spacer(modifier = Modifier.height(SpacingS))

        Text(
            text = remainingSeconds.toHeroTimerDisplay(),
            style = TimerDisplayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SpacingL))

        // Fraction + aspectRatio keeps the can's silhouette correct at any
        // width, and sizeIn clamps it so it neither shrinks to a speck on a
        // small phone nor balloons disproportionately in the wider tablet
        // column (ResponsiveScreen caps that column at 600dp, not the full
        // screen width).
        MeltingCanAnimation(
            progress = progressRemaining,
            canColor = accent.onChip,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.52f)
                .aspectRatio(CanAspectRatio)
                .sizeIn(
                    minWidth = 120.dp,
                    maxWidth = 200.dp,
                    minHeight = 160.dp,
                    maxHeight = 260.dp
                )
        )

        Spacer(modifier = Modifier.height(SpacingL))

        // Session meta: one quiet, scannable line instead of a divider rule
        // plus two stacked lines - stays subtle and doesn't compete with the
        // timer above it.
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = SpacingM),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${safeTotal.toTotalLabel()} total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "   ·   ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = blockedAppsCount.toBlockedAppsLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = {
                if (isPaused) {
                    println("Clicked: Focus Resume button")
                    onResume()
                } else {
                    println("Clicked: Focus Pause button")
                    onPause()
                }
            },
            shape = ButtonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = SpacingS)
                .height(56.dp)
        ) {
            Text(
                text = if (isPaused) "Resume" else "Pause",
                style = ButtonLabel
            )
        }

        Spacer(modifier = Modifier.height(SpacingM))
    }
}

// A small, consistent spacing scale used throughout FocusScreen instead of
// one-off dp values, so the rhythm between title/timer/illustration/meta/
// button stays intentional rather than arbitrary.
private val SpacingXS = 4.dp
private val SpacingS = 12.dp
private val SpacingM = 16.dp
private val SpacingL = 28.dp

// Matches melting_can.json's native 400x560 composition size (see
// tools/generate_melting_can.py), so the Lottie content never letterboxes
// or gets cropped inside its modifier bounds.
private const val CanAspectRatio = 400f / 560f

// "00:42:17" - always zero-padded HH:MM:SS, per the spec's own hero-timer
// example, so width never shifts as digits change (see also the "tnum"
// tabular-figure setting on TimerDisplayLarge).
private fun Long.toHeroTimerDisplay(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

// "1:00:00 total" - hour NOT zero-padded, per the spec's own total-label
// example. Deliberately a different format from the hero timer above.
private fun Long.toTotalLabel(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return "%d:%02d:%02d".format(h, m, s)
}

private fun Int.toBlockedAppsLabel(): String =
    if (this == 1) "1 app blocked" else "$this apps blocked"
