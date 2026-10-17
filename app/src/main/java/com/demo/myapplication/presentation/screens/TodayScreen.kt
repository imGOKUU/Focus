package com.demo.myapplication.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.myapplication.presentation.components.AccessibilityPermissionBanner
import com.demo.myapplication.presentation.components.CardRowDivider
import com.demo.myapplication.presentation.components.Depth
import com.demo.myapplication.presentation.components.FocusBottomSheet
import com.demo.myapplication.presentation.components.FocusLayout
import com.demo.myapplication.presentation.components.FocusPrimaryButton
import com.demo.myapplication.presentation.components.FocusSecondaryButton
import com.demo.myapplication.presentation.components.FocusSurface
import com.demo.myapplication.presentation.components.IconTile
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.components.SectionCard
import com.demo.myapplication.presentation.components.SectionHeader
import com.demo.myapplication.presentation.components.SectionLabel
import com.demo.myapplication.presentation.components.ValueChip
import com.demo.myapplication.presentation.components.entrance
import com.demo.myapplication.presentation.components.insetSurface
import com.demo.myapplication.presentation.components.rememberAppIcon
import com.demo.myapplication.presentation.components.screenVerticalInsets
import com.demo.myapplication.presentation.components.toClock
import com.demo.myapplication.presentation.components.toShortDuration
import com.demo.myapplication.presentation.theme.AddGoalsHeadline
import com.demo.myapplication.presentation.theme.TileShape
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The app's home: what's planned today, how much of it is left, and the one
 * thing to do next. Tapping a goal opens a review sheet first (what will be
 * blocked, for how long) so a session never starts by accident.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    userName: String = "",
    onStartGoal: (GoalListItem) -> Unit = {},
    onEditGoal: (GoalListItem) -> Unit = {},
    onAddGoal: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    var reviewGoalId by rememberSaveable { mutableStateOf<Long?>(null) }
    val openGoal: (GoalListItem) -> Unit = { goal ->
        // A running or paused goal goes straight back to its session -
        // it's already been reviewed and started.
        if (goal.hasActiveSession) onStartGoal(goal) else reviewGoalId = goal.id
    }

    ResponsiveScreen(
        contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())
    ) {
        Spacer(modifier = Modifier.height(FocusLayout.ScreenTopGap + 12.dp))

        TodayHeader(userName = userName, onOpenHistory = onOpenHistory, onOpenSettings = onOpenSettings, modifier = Modifier.entrance(index = 0))

        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Short screens (phone landscape): overview beside the goal list.
            if (maxHeight < FocusLayout.SideBySideHeightThreshold) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ScrollingColumn(modifier = Modifier.weight(1f)) {
                        TodayOverview(state = state, onOpenGoal = openGoal)
                    }
                    ScrollingColumn(modifier = Modifier.weight(1f)) {
                        GoalsSection(state = state, onGoalClick = openGoal, onAddGoal = onAddGoal)
                    }
                }
            } else {
                ScrollingColumn(modifier = Modifier.fillMaxSize()) {
                    TodayOverview(state = state, onOpenGoal = openGoal)
                    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
                    GoalsSection(state = state, onGoalClick = openGoal, onAddGoal = onAddGoal)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TodayPrimaryAction(
            state = state,
            onAddGoal = onAddGoal,
            onResume = onStartGoal,
            onReview = { reviewGoalId = it.id },
            modifier = Modifier.entrance(index = 4)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    state.goals.firstOrNull { it.id == reviewGoalId }?.let { goal ->
        StartGoalSheet(
            goal = goal,
            onStart = {
                reviewGoalId = null
                onStartGoal(goal)
            },
            onEdit = {
                reviewGoalId = null
                onEditGoal(goal)
            },
            onDismiss = { reviewGoalId = null }
        )
    }
}

@Composable
private fun ScrollingColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Top/bottom padding inside the scroll area keeps the first and last
    // cards' shadows from being cut off at its edges.
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        content()
    }
}

@Composable
private fun TodayHeader(userName: String, onOpenHistory: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    // Greeting as the small label ("Good evening, Sam" - or the weekday if no
    // name was given), the date beside the title. Kept as separate pieces so
    // large system fonts wrap cleanly instead of breaking mid-label.
    val label = remember(userName) {
        val greeting = when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
        if (userName.isBlank()) greeting else "$greeting, ${userName.trim()}"
    }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM")) }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            Text(
                text = "Today",
                style = AddGoalsHeadline.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
    HeaderButton(icon = Icons.Outlined.CalendarMonth, label = "History", onClick = onOpenHistory)
    Spacer(modifier = Modifier.width(10.dp))
    HeaderButton(icon = Icons.Outlined.Settings, label = "Settings", onClick = onOpenSettings)
    }
}

@Composable
private fun HeaderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    FocusSurface(
        modifier = Modifier.size(44.dp),
        shape = TileShape,
        depth = Depth.Low,
        onClick = onClick,
        onClickLabel = "Open ${label.lowercase()}",
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TodayOverview(state: TodayUiState, onOpenGoal: (GoalListItem) -> Unit) {
    if (state.isLoaded && state.goals.isEmpty()) {
        EmptyDayCard(modifier = Modifier.entrance(index = 1))
    } else {
        SummaryCard(state = state, modifier = Modifier.entrance(index = 1))
    }

    val active = state.activeGoal
    AnimatedVisibility(
        visible = active != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        // Keep showing the last goal while the card animates out.
        var lastActive by remember { mutableStateOf(active) }
        if (active != null) lastActive = active
        lastActive?.let { goal ->
            ActiveSessionCard(
                goal = goal,
                onClick = { onOpenGoal(goal) },
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }

    if (state.goals.isNotEmpty()) {
        AccessibilityPermissionBanner(
            modifier = Modifier
                .padding(top = 16.dp)
                .entrance(index = 2)
        )
    }
}

/** Time left today is the headline; the ring shows how many goals are done. */
@Composable
private fun SummaryCard(state: TodayUiState, modifier: Modifier = Modifier) {
    val allDone = state.goals.isNotEmpty() && state.doneCount == state.goals.size
    FocusSurface(modifier = modifier.fillMaxWidth(), depth = Depth.Medium) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (allDone) "All done" else state.remainingSeconds.toShortDuration(),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 34.sp,
                        lineHeight = 40.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (allDone) "One less thing on your mind today." else "left to give today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!allDone && state.totalSeconds > 0) {
                    // What's already been given, so "left" is never ambiguous:
                    // left + done = planned.
                    val given = (state.totalSeconds - state.remainingSeconds).coerceAtLeast(0L)
                    Spacer(modifier = Modifier.height(10.dp))
                    ProgressBar(
                        fraction = given.toFloat() / state.totalSeconds,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${given.toShortDuration(roundUp = false)} done · ${state.totalSeconds.toShortDuration()} planned",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            GoalsDoneRing(done = state.doneCount, total = state.goals.size)
        }
    }
}

@Composable
private fun GoalsDoneRing(done: Int, total: Int) {
    val fraction by animateFloatAsState(
        targetValue = if (total == 0) 0f else done.toFloat() / total,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "doneRing"
    )
    val track = MaterialTheme.colorScheme.surfaceContainer
    val fill = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            if (fraction > 0f) {
                drawArc(color = fill, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, style = stroke)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$done/$total",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "done",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyDayCard(modifier: Modifier = Modifier) {
    FocusSurface(modifier = modifier.fillMaxWidth(), depth = Depth.Medium) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(color = MaterialTheme.colorScheme.primary, glowColor = MaterialTheme.colorScheme.primary, size = 48.dp) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Plan your day",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Add something you want to give time to. While its timer runs, the apps you pick stay blocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Shown while a session is running or paused, so it's never forgotten. */
@Composable
private fun ActiveSessionCard(goal: GoalListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isRunning = goal.progress == GoalProgress.Running
    FocusSurface(
        modifier = modifier.fillMaxWidth(),
        depth = Depth.Medium,
        onClick = onClick,
        onClickLabel = "Open session"
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(color = goal.accent.chipBackground, glowColor = goal.accent.onChip) {
                Icon(goal.icon, contentDescription = null, tint = goal.accent.onChip, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(color = goal.accent.onChip, pulsing = isRunning)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRunning) "IN SESSION" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp),
                        color = goal.accent.onChip
                    )
                }
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = goal.remainingSeconds.toClock(),
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
internal fun LiveDot(color: androidx.compose.ui.graphics.Color, pulsing: Boolean) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = Modifier
            .size(7.dp)
            .alpha(if (pulsing) pulse else 0.6f)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun GoalsSection(
    state: TodayUiState,
    onGoalClick: (GoalListItem) -> Unit,
    onAddGoal: () -> Unit
) {
    SectionHeader(
        text = "YOUR GOALS",
        trailing = state.goals.takeIf { it.isNotEmpty() }?.let { "${it.size} today" },
        modifier = Modifier.entrance(index = 3)
    )
    Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .entrance(index = 3)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            state.goals.forEach { goal ->
                GoalRow(goal = goal, onClick = { onGoalClick(goal) })
                CardRowDivider()
            }
            AddGoalRow(onClick = onAddGoal, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun GoalRow(goal: GoalListItem, onClick: () -> Unit) {
    val done = goal.isCompletedToday
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = if (goal.hasActiveSession) "Open session" else "Review and start", onClick = onClick)
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(
            color = goal.accent.chipBackground,
            glowColor = goal.accent.onChip,
            modifier = Modifier.alpha(if (done) 0.75f else 1f)
        ) {
            Icon(goal.icon, contentDescription = null, tint = goal.accent.onChip, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))

        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (done) 0.7f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = goal.statusLine(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (goal.hasActiveSession) goal.accent.onChip else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            val spent = goal.dailySeconds - goal.remainingSeconds
            if (!done && spent > 0 && goal.dailySeconds > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                ProgressBar(fraction = spent.toFloat() / goal.dailySeconds, color = goal.accent.onChip)
            }
        }

        if (done) {
            ValueChip(text = "Done", icon = Icons.Outlined.Check, contentColor = goal.accent.onChip)
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun GoalListItem.statusLine(): String = when (progress) {
    GoalProgress.NotStarted -> "${dailySeconds.toShortDuration()} a day"
    GoalProgress.Partial -> "${remainingSeconds.toShortDuration()} left of ${dailySeconds.toShortDuration()}"
    GoalProgress.Running -> "In session · ${remainingSeconds.toClock()} left"
    GoalProgress.Paused -> "Paused · ${remainingSeconds.toClock()} left"
    GoalProgress.Done -> "Done for today · ${dailySeconds.toShortDuration()}"
}

@Composable
private fun ProgressBar(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "goalProgress")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .insetSurface(CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun AddGoalRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(color = MaterialTheme.colorScheme.primary, glowColor = MaterialTheme.colorScheme.primary) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
        Text(
            text = "Add goal",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * The one obvious next step: add a first goal, go back to a running
 * session, or start the next goal that isn't done. Hidden once the day's done.
 */
@Composable
private fun TodayPrimaryAction(
    state: TodayUiState,
    onAddGoal: () -> Unit,
    onResume: (GoalListItem) -> Unit,
    onReview: (GoalListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isLoaded) return
    val active = state.activeGoal
    val next = state.nextGoal
    when {
        state.goals.isEmpty() -> FocusPrimaryButton(
            text = "Add your first goal",
            leadingIcon = Icons.Outlined.Add,
            onClick = onAddGoal,
            modifier = modifier.fillMaxWidth()
        )
        active != null -> FocusPrimaryButton(
            text = "Back to ${active.name}",
            leadingIcon = Icons.Outlined.PlayArrow,
            onClick = { onResume(active) },
            modifier = modifier.fillMaxWidth()
        )
        next != null -> FocusPrimaryButton(
            text = if (next.progress == GoalProgress.Partial) "Continue ${next.name}" else "Start ${next.name}",
            leadingIcon = Icons.Outlined.PlayArrow,
            onClick = { onReview(next) },
            modifier = modifier.fillMaxWidth()
        )
    }
}

/**
 * Pre-session review (spec: "Start Goal" + "Commitment Confirmation" in one
 * step): how long, and exactly which apps will be blocked, before starting.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StartGoalSheet(
    goal: GoalListItem,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    FocusBottomSheet(onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(color = goal.accent.chipBackground, glowColor = goal.accent.onChip, size = 52.dp) {
                Icon(goal.icon, contentDescription = null, tint = goal.accent.onChip, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        goal.isCompletedToday -> "Done for today"
                        goal.progress == GoalProgress.Partial -> "${goal.remainingSeconds.toShortDuration()} left today"
                        else -> "${goal.dailySeconds.toShortDuration()} today"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("WHILE YOU FOCUS")
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        SectionCard(modifier = Modifier.fillMaxWidth(), depth = Depth.Low) {
            Column(modifier = Modifier.padding(FocusLayout.CardHorizontalPadding)) {
                if (goal.blockedApps.isEmpty()) {
                    Text(
                        text = "No apps will be blocked for this goal. You can choose some in Edit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goal.blockedApps.forEach { BlockedAppChip(it) }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "These stay blocked until your session ends.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (goal.isCompletedToday) {
            Text(
                text = "You've given ${goal.name} its time today. It'll be ready again tomorrow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            FocusSecondaryButton(text = "Edit goal", onClick = onEdit, modifier = Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusSecondaryButton(text = "Edit", onClick = onEdit, modifier = Modifier.weight(1f))
                FocusPrimaryButton(
                    text = "Start focus",
                    leadingIcon = Icons.Outlined.PlayArrow,
                    onClick = onStart,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}

/** A blocked app as its real icon plus name, so it's recognisable at a glance. */
@Composable
private fun BlockedAppChip(app: BlockedAppInfo) {
    val icon by rememberAppIcon(app.packageName)
    Row(
        modifier = Modifier
            .insetSurface(com.demo.myapplication.presentation.theme.ChipShape)
            .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(22.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            icon?.let { androidx.compose.foundation.Image(bitmap = it, contentDescription = null, modifier = Modifier.size(22.dp)) }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}
