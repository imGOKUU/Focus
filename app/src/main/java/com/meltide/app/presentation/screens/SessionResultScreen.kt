package com.meltide.app.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meltide.app.presentation.components.Depth
import com.meltide.app.presentation.components.FocusPrimaryButton
import com.meltide.app.presentation.components.FocusSurface
import com.meltide.app.presentation.components.IconTile
import com.meltide.app.presentation.components.ResponsiveScreen
import com.meltide.app.presentation.components.ValueChip
import com.meltide.app.presentation.components.entrance
import com.meltide.app.presentation.components.screenVerticalInsets
import com.meltide.app.presentation.components.toShortDuration
import com.meltide.app.presentation.theme.GoalAccent
import com.meltide.app.presentation.theme.GoalAccents
import com.meltide.app.presentation.theme.TimerDisplayLarge

/**
 * The calm close of a session (spec: "Session Complete" / "Ending Early").
 * Completed: "Done." plus the time given. Ended early: honest about what
 * was focused and what's still left today - no partial credit, no shaming.
 */
@Composable
fun SessionResultScreen(
    goalName: String,
    completed: Boolean,
    focusedSeconds: Long,
    remainingTodaySeconds: Long,
    accent: GoalAccent = GoalAccents.Teal,
    onClose: () -> Unit = {}
) {
    // The badge springs in a beat after the screen appears.
    val badgeScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        badgeScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
    }

    ResponsiveScreen(contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())) {
        Spacer(modifier = Modifier.weight(1f))

        IconTile(
            color = if (completed) accent.chipBackground else androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer,
            glowColor = accent.onChip,
            size = 88.dp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .graphicsLayer {
                    scaleX = badgeScale.value
                    scaleY = badgeScale.value
                }
        ) {
            Icon(
                imageVector = if (completed) Icons.Outlined.Check else Icons.Outlined.HourglassBottom,
                contentDescription = null,
                tint = accent.onChip,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        FocusSurface(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 1),
            depth = Depth.Medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = goalName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (completed) "Done." else "Session ended",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = focusedSeconds.toHeroTimerDisplay(),
                    style = TimerDisplayLarge,
                    color = accent.onChip
                )
                Text(
                    text = "focused",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                if (completed) {
                    Text(
                        text = "One less thing today.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.Center) {
                        ValueChip(text = "${remainingTodaySeconds.toShortDuration()} still left today")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Pick it up again whenever you're ready.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        FocusPrimaryButton(
            text = "Close",
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 2)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
