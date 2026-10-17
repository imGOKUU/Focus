package com.meltide.app.presentation.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.width
import com.meltide.app.core.platform.PermissionStatus
import com.meltide.app.presentation.components.insetSurface
import com.meltide.app.presentation.components.rememberCheckOnResume
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.meltide.app.presentation.components.Depth
import com.meltide.app.presentation.components.FocusBackButton
import com.meltide.app.presentation.components.FocusDialog
import com.meltide.app.presentation.components.FocusLayout
import com.meltide.app.presentation.components.FocusPrimaryButton
import com.meltide.app.presentation.components.FocusSecondaryButton
import com.meltide.app.presentation.components.FocusSurface
import com.meltide.app.presentation.components.ResponsiveScreen
import com.meltide.app.presentation.components.ValueChip
import com.meltide.app.presentation.components.toShortDuration
import com.meltide.app.presentation.theme.FocusDanger
import com.meltide.app.presentation.theme.FocusMinimalBackground
import com.meltide.app.presentation.theme.FocusMinimalText
import com.meltide.app.presentation.theme.FocusMinimalTextDim
import com.meltide.app.presentation.theme.GoalAccent
import com.meltide.app.presentation.theme.GoalAccents
import com.meltide.app.presentation.theme.TileShape
import com.meltide.app.presentation.theme.TimerDisplayLarge
import com.meltide.app.presentation.theme.TimerDisplayMinimal
import kotlinx.coroutines.delay


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
    onEndSession: () -> Unit = {},
    startInMinimalMode: Boolean = false,
    keepScreenOn: Boolean = false,
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

    var isMinimal by rememberSaveable { mutableStateOf(startInMinimalMode) }

    // Minimal mode keeps the screen on by itself; this covers the normal view.
    val view = LocalView.current
    DisposableEffect(keepScreenOn, isMinimal) {
        if (keepScreenOn && !isMinimal) view.keepScreenOn = true
        onDispose { if (!isMinimal) view.keepScreenOn = false }
    }
    var confirmEnd by remember { mutableStateOf(false) }

    // In minimal mode, back leaves minimal mode first rather than the session.
    BackHandler(enabled = isMinimal) { isMinimal = false }

    AnimatedContent(
        targetState = isMinimal,
        transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(350)) },
        label = "focusMode"
    ) { minimal ->
        if (minimal) {
            MinimalFocusMode(
                goalName = goal?.name.orEmpty(),
                remainingSeconds = remainingSeconds,
                isPaused = isPaused,
                onPause = onPause,
                onResume = onResume,
                onExit = { isMinimal = false }
            )
        } else {
            NormalFocusMode(
                goal = goal,
                remainingSeconds = remainingSeconds,
                safeTotal = safeTotal,
                progressRemaining = progressRemaining,
                blockedAppsCount = blockedAppsCount,
                accent = accent,
                isPaused = isPaused,
                onPause = onPause,
                onResume = onResume,
                onEndRequest = { confirmEnd = true },
                onMinimal = { isMinimal = true },
                onBack = onBack
            )
        }
    }

    if (confirmEnd) {
        FocusDialog(
            title = "End session?",
            message = "You still have ${remainingSeconds.toShortDuration()} left. " +
                "The time you've put in is kept, and your apps will be available again.",
            confirmText = "End session",
            dismissText = "Keep going",
            destructive = true,
            onConfirm = {
                confirmEnd = false
                onEndSession()
            },
            onDismiss = { confirmEnd = false }
        )
    }
}

@Composable
private fun NormalFocusMode(
    goal: GoalListItem?,
    remainingSeconds: Long,
    safeTotal: Long,
    progressRemaining: Float,
    blockedAppsCount: Int,
    accent: GoalAccent,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEndRequest: () -> Unit,
    onMinimal: () -> Unit,
    onBack: () -> Unit
) {
    ResponsiveScreen {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = FocusLayout.ScreenTopGap),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FocusBackButton(onClick = {
                println("Clicked: Focus Back button")
                onBack()
            })
            FocusSurface(
                modifier = Modifier.size(44.dp),
                shape = TileShape,
                depth = Depth.Low,
                onClick = onMinimal,
                onClickLabel = "Minimal mode",
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = "Minimal mode",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Flexible space above and (more) below keeps the timer block a little
        // above centre on any height, instead of hugging the top.
        Spacer(modifier = Modifier.weight(0.5f))

        SessionStatePill(
            isPaused = isPaused,
            color = accent.onChip,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(SpacingS))

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

        // Dims smoothly on pause rather than snapping.
        val timerAlpha by animateFloatAsState(if (isPaused) 0.45f else 1f, tween(400), label = "timerAlpha")
        Text(
            text = remainingSeconds.toHeroTimerDisplay(),
            style = TimerDisplayLarge.copy(fontSize = 54.sp, lineHeight = 60.sp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = timerAlpha),
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = isPaused, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                text = "Paused - your apps are available until you resume",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

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

        // Session meta as two quiet pressed-in chips - scannable, and clearly
        // information rather than something to tap.
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = SpacingM),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ValueChip(text = "${safeTotal.toTotalLabel()} total", icon = Icons.Outlined.Schedule)
            BlockingChip(blockedAppsCount = blockedAppsCount)
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = SpacingS),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FocusSecondaryButton(
                text = "End",
                onClick = onEndRequest,
                contentColor = FocusDanger,
                height = 56.dp,
                modifier = Modifier.weight(1f)
            )
            FocusPrimaryButton(
                text = if (isPaused) "Resume" else "Pause",
                leadingIcon = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                onClick = {
                    if (isPaused) {
                        println("Clicked: Focus Resume button")
                        onResume()
                    } else {
                        println("Clicked: Focus Pause button")
                        onPause()
                    }
                },
                height = 56.dp,
                modifier = Modifier.weight(2f)
            )
        }

        Spacer(modifier = Modifier.height(SpacingM))
    }
}

/**
 * Spec's "Minimum mode": near-black, only the goal and the time, for leaving
 * the phone face-up beside you. Tapping shows controls briefly, then they
 * fade away again. Keeps the screen on and hides the system bars.
 */
@Composable
private fun MinimalFocusMode(
    goalName: String,
    remainingSeconds: Long,
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    var controlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(ControlsAutoHideMillis)
            controlsVisible = false
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val wasLightStatusBars = controller?.isAppearanceLightStatusBars
        view.keepScreenOn = true
        controller?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
            isAppearanceLightStatusBars = false
        }
        onDispose {
            view.keepScreenOn = false
            controller?.apply {
                show(WindowInsetsCompat.Type.systemBars())
                wasLightStatusBars?.let { isAppearanceLightStatusBars = it }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusMinimalBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Show controls"
            ) { controlsVisible = !controlsVisible },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = goalName,
                style = MaterialTheme.typography.titleMedium,
                color = FocusMinimalTextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = remainingSeconds.toHeroTimerDisplay(),
                style = TimerDisplayMinimal,
                color = FocusMinimalText.copy(alpha = if (isPaused) 0.5f else 1f)
            )
            if (isPaused) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "paused", style = MaterialTheme.typography.labelSmall, color = FocusMinimalTextDim)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 32.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TextButton(onClick = if (isPaused) onResume else onPause) {
                    Text(if (isPaused) "Resume" else "Pause", color = FocusMinimalText)
                }
                TextButton(onClick = onExit) {
                    Text("Exit minimal", color = FocusMinimalText)
                }
            }
        }
    }
}

private const val ControlsAutoHideMillis = 3_000L

// A small, consistent spacing scale used throughout FocusScreen instead of
// one-off dp values, so the rhythm between title/timer/illustration/meta/
// button stays intentional rather than arbitrary.
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
internal fun Long.toHeroTimerDisplay(): String {
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

/** "In session" with a live dot, or "Paused" - the session's state at a glance. */
@Composable
private fun SessionStatePill(isPaused: Boolean, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .insetSurface(com.meltide.app.presentation.theme.ChipShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiveDot(color = color, pulsing = !isPaused)
        Spacer(modifier = Modifier.width(8.dp))
        AnimatedContent(targetState = isPaused, label = "statePill") { paused ->
            Text(
                text = if (paused) "PAUSED" else "IN SESSION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
                color = color
            )
        }
    }
}

/**
 * How many apps are blocked - but only when blocking actually works. With
 * the Accessibility service off it says so (and links to turn it on),
 * rather than claiming apps are blocked when they aren't.
 */
@Composable
private fun BlockingChip(blockedAppsCount: Int) {
    val context = LocalContext.current
    val blockingOn by rememberCheckOnResume(PermissionStatus::isAccessibilityServiceEnabled)
    when {
        blockedAppsCount == 0 -> ValueChip(text = "No apps blocked", icon = Icons.Outlined.Block)
        blockingOn -> ValueChip(text = blockedAppsCount.toBlockedAppsLabel(), icon = Icons.Outlined.Block)
        else -> ValueChip(
            text = "Blocking is off · Turn on",
            icon = Icons.Outlined.Block,
            contentColor = FocusDanger,
            modifier = Modifier
                .clip(com.meltide.app.presentation.theme.ChipShape)
                .clickable(onClickLabel = "Turn on app blocking") {
                    context.startActivity(PermissionStatus.accessibilitySettingsIntent())
                }
        )
    }
}
