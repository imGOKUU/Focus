package com.meltide.app.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meltide.app.presentation.components.CardRowDivider
import com.meltide.app.presentation.components.Depth
import com.meltide.app.presentation.components.FocusBackButton
import com.meltide.app.presentation.components.FocusLayout
import com.meltide.app.presentation.components.FocusSurface
import com.meltide.app.presentation.components.IconTile
import com.meltide.app.presentation.components.ResponsiveScreen
import com.meltide.app.presentation.components.SectionCard
import com.meltide.app.presentation.components.SectionHeader
import com.meltide.app.presentation.components.ValueChip
import com.meltide.app.presentation.components.entrance
import com.meltide.app.presentation.components.insetSurface
import com.meltide.app.presentation.components.screenVerticalInsets
import com.meltide.app.presentation.components.toShortDuration
import com.meltide.app.presentation.theme.FocusSoftAccent
import com.meltide.app.presentation.theme.TileShape
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * History, as the spec frames it: a month calendar with a dot per day, the
 * chosen day's goals, and how much time each goal got this month. Facts
 * only - time given against time planned. No scores, streaks or badges.
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit = {},
    onSelectDate: (LocalDate) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {}
) {
    ResponsiveScreen(
        contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())
    ) {
        Spacer(modifier = Modifier.height(FocusLayout.ScreenTopGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusBackButton(onClick = onBack)
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val calendar: @Composable () -> Unit = {
                MonthCard(
                    state = state,
                    onSelectDate = onSelectDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    modifier = Modifier.entrance(index = 1)
                )
            }
            val details: @Composable () -> Unit = {
                DayDetail(state = state, modifier = Modifier.entrance(index = 2))
                Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
                MonthTotals(state = state, modifier = Modifier.entrance(index = 3))
            }
            // Short screens (phone landscape): calendar beside the details.
            if (maxHeight < FocusLayout.SideBySideHeightThreshold) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HistoryColumn(Modifier.weight(1f)) { calendar() }
                    HistoryColumn(Modifier.weight(1f)) { details() }
                }
            } else {
                HistoryColumn(Modifier.fillMaxSize()) {
                    calendar()
                    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
                    details()
                }
            }
        }
    }
}

@Composable
private fun HistoryColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Month calendar
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun MonthCard(
    state: HistoryUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    FocusSurface(modifier = modifier.fillMaxWidth(), depth = Depth.Medium) {
        Column(modifier = Modifier.padding(FocusLayout.CardHorizontalPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthArrow(
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    label = "Previous month",
                    enabled = true,
                    onClick = onPreviousMonth
                )
                AnimatedContent(
                    targetState = state.month,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "monthTitle",
                    modifier = Modifier.weight(1f)
                ) { month ->
                    Text(
                        text = month.format(MonthTitleFormat),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                MonthArrow(
                    icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    label = "Next month",
                    enabled = state.canGoForward,
                    onClick = onNextMonth
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Slides left/right with the direction of the month change.
            AnimatedContent(
                targetState = state.month,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(tween(280)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(200)) { if (forward) -it / 4 else it / 4 } + fadeOut(tween(200)))
                },
                label = "monthGrid"
            ) { month ->
                MonthGrid(
                    month = month,
                    state = state,
                    onSelectDate = onSelectDate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            CalendarLegend()
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthStat(
                    value = state.monthGivenSeconds.toShortDuration(roundUp = false),
                    label = "focused this month",
                    modifier = Modifier.weight(1f)
                )
                MonthStat(
                    value = state.activeDayCount.toString(),
                    label = if (state.activeDayCount == 1) "day you focused" else "days you focused",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonthArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    FocusSurface(
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.35f),
        shape = TileShape,
        depth = Depth.Low,
        onClick = if (enabled) onClick else null,
        onClickLabel = label,
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    state: HistoryUiState,
    onSelectDate: (LocalDate) -> Unit
) {
    // Read through the configuration so a language/region change recomposes.
    val locale: Locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val weekdays = remember(firstDayOfWeek) { (0L until 7L).map { firstDayOfWeek.plus(it) } }
    val leadingBlanks = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val cells: List<LocalDate?> = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column {
        Row {
            weekdays.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                summary = state.days[date],
                                isToday = date == state.today,
                                isSelected = date == state.selectedDate,
                                isFuture = date > state.today,
                                onClick = { onSelectDate(date) }
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    summary: DaySummary?,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit
) {
    val description = buildString {
        append(date.format(DayTitleFormat))
        if (summary != null) append(", ${summary.givenSeconds.toShortDuration(roundUp = false)} of ${summary.plannedSeconds.toShortDuration()}")
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(TileShape)
            // Selected: solid primary. Today (when not selected): a primary
            // ring. Future: dimmed and not tappable. Past: plain.
            .then(
                when {
                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                    isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), TileShape)
                    else -> Modifier
                }
            )
            .clickable(enabled = !isFuture, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFeatureSettings = "tnum"
                ),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(3.dp))
            DayDot(summary = summary, onSelected = isSelected)
        }
    }
}

/** Solid when the day's plan was met, lighter the less of it was given. */
@Composable
private fun DayDot(summary: DaySummary?, onSelected: Boolean = false) {
    // On the solid selected cell the dot switches to the cream "on" colour.
    val primary = if (onSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val color = when {
        summary == null -> Color.Transparent
        summary.givenSeconds == 0L -> FocusSoftAccent
        summary.fraction >= 1f -> primary
        else -> primary.copy(alpha = 0.25f + 0.5f * summary.fraction)
    }
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun CalendarLegend() {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            FocusSoftAccent to "not started",
            primary.copy(alpha = 0.45f) to "partly",
            primary to "all done"
        ).forEachIndexed { index, (color, label) ->
            if (index > 0) Spacer(modifier = Modifier.width(14.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonthStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .insetSurface(TileShape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Selected day + month totals
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DayDetail(state: HistoryUiState, modifier: Modifier = Modifier) {
    val day = state.selectedDay
    Column(modifier = modifier) {
        SectionHeader(
            text = if (state.selectedDate == state.today) "TODAY" else state.selectedDate.format(DayTitleFormat).uppercase(),
            trailing = day?.let { "${it.givenSeconds.toShortDuration(roundUp = false)} / ${it.plannedSeconds.toShortDuration()}" }
        )
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            AnimatedContent(
                targetState = state.selectedDate to day,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "dayDetail"
            ) { (date, summary) ->
                if (summary == null) {
                    Text(
                        text = if (date == state.today) "Nothing started yet today." else "Nothing planned this day.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 20.dp)
                    )
                } else {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // The whole day at a glance before the per-goal rows.
                        Column(modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 12.dp)) {
                            HistoryBar(fraction = summary.fraction, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${summary.givenSeconds.toShortDuration(roundUp = false)} of ${summary.plannedSeconds.toShortDuration()} planned that day",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        CardRowDivider()
                        summary.entries.forEachIndexed { index, entry ->
                            DayEntryRow(entry)
                            if (index < summary.entries.lastIndex) CardRowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEntryRow(entry: DayGoalEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(color = entry.accent.chipBackground, glowColor = entry.accent.onChip) {
            Icon(entry.icon, contentDescription = null, tint = entry.accent.onChip, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            HistoryBar(
                fraction = if (entry.plannedSeconds == 0L) 0f else entry.givenSeconds.toFloat() / entry.plannedSeconds,
                color = entry.accent.onChip
            )
        }
        when {
            entry.isDone -> ValueChip(text = entry.plannedSeconds.toShortDuration(), icon = Icons.Outlined.Check, contentColor = entry.accent.onChip)
            entry.givenSeconds > 0 -> ValueChip(text = "${entry.givenSeconds.toShortDuration(roundUp = false)} of ${entry.plannedSeconds.toShortDuration()}")
            else -> ValueChip(text = "-")
        }
    }
}

@Composable
private fun MonthTotals(state: HistoryUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionHeader(text = state.month.format(MonthTitleFormat).uppercase())
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            if (state.goalTotals.isEmpty()) {
                Text(
                    text = "No focused time this month yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 20.dp)
                )
            } else {
                val max = state.goalTotals.maxOf { it.givenSeconds }.coerceAtLeast(1L)
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    state.goalTotals.forEachIndexed { index, total ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = FocusLayout.RowMinHeight)
                                .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconTile(color = total.accent.chipBackground, glowColor = total.accent.onChip) {
                                Icon(total.icon, contentDescription = null, tint = total.accent.onChip, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = total.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = total.givenSeconds.toShortDuration(roundUp = false),
                                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Bars are relative to the month's biggest goal.
                                HistoryBar(fraction = total.givenSeconds.toFloat() / max, color = total.accent.onChip)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (total.daysDone) {
                                        0 -> "Not fully done on any day yet"
                                        1 -> "Fully done on 1 day"
                                        else -> "Fully done on ${total.daysDone} days"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < state.goalTotals.lastIndex) CardRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBar(fraction: Float, color: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "historyBar")
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

private val MonthTitleFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
private val DayTitleFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM")
