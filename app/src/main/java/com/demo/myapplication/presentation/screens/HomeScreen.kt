package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demo.myapplication.presentation.components.AccessibilityPermissionBanner
import com.demo.myapplication.presentation.components.CardRowDivider
import com.demo.myapplication.presentation.components.FocusLayout
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.components.SectionCard
import com.demo.myapplication.presentation.components.SectionLabel
import com.demo.myapplication.presentation.components.screenVerticalInsets
import com.demo.myapplication.presentation.theme.AddGoalsHeadline
import com.demo.myapplication.presentation.theme.ButtonLabel
import com.demo.myapplication.presentation.theme.ButtonPillShape
import com.demo.myapplication.presentation.theme.GoalAccent

data class GoalListItem(
    val id:Long,
    val name: String,
    val durationLabel: String,
    val icon: ImageVector,
    val accent: GoalAccent,
    val isCompletedToday: Boolean = false
)

@Composable
fun HomeScreen(
    goals: List<GoalListItem> = emptyList(),
    onBack: () -> Unit = {},
    onGoalClick: (GoalListItem) -> Unit = {},
    onAddGoal: () -> Unit = {},
    onContinue: () -> Unit = {}
) {
    ResponsiveScreen(
        contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable {
                println("Clicked: Home Back icon")
                onBack()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Everything between the back icon and Continue. Stacked normally; on
        // short screens (phone landscape) the intro sits beside the goals card
        // so the card still gets usable height - same rule as GoalConfigScreen.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val goalsSection: @Composable ColumnScope.() -> Unit = {
                GoalsSection(goals = goals, onGoalClick = onGoalClick, onAddGoal = onAddGoal)
            }
            if (maxHeight < FocusLayout.SideBySideHeightThreshold) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        HomeIntro(showBanner = goals.isNotEmpty())
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        content = goalsSection
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    HomeIntro(showBanner = goals.isNotEmpty())
                    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
                    goalsSection()
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                println("Clicked: Home Continue button")
                onContinue()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = ButtonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Continue",
                style = ButtonLabel
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HomeIntro(showBanner: Boolean) {
    Text(
        text = "What do you want\nto give time to?",
        style = AddGoalsHeadline,
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Add a few goals. Keep it simple.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (showBanner) {
        Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
        AccessibilityPermissionBanner()
    }
}

@Composable
private fun ColumnScope.GoalsSection(
    goals: List<GoalListItem>,
    onGoalClick: (GoalListItem) -> Unit,
    onAddGoal: () -> Unit
) {
    SectionLabel("YOUR GOALS")
    Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))

    // Same pattern as GoalConfigScreen's app list: the weight lives on this
    // Box, so it claims the remaining height (keeping Continue anchored),
    // while the card inside only wraps its content - a short list gets a
    // short card, a long list is capped and scrolls inside the card.
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                // fill = false: the list takes only what it needs, leaving
                // the Add Goal row pinned under it at the card's bottom.
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    itemsIndexed(goals, key = { _, goal -> goal.id }) { _, goal ->
                        GoalRow(goal = goal, onClick = {
                            println("Clicked: Home goal row - ${goal.name}")
                            onGoalClick(goal)
                        })
                        CardRowDivider()
                    }
                }
                AddGoalRow(
                    onClick = {
                        println("Clicked: Home Add Goal")
                        onAddGoal()
                    },
                    modifier = Modifier.padding(
                        top = if (goals.isEmpty()) 4.dp else 0.dp,
                        bottom = 4.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun GoalRow(goal: GoalListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(FocusLayout.RowIconSize)
                .background(goal.accent.chipBackground, shape = MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = goal.icon,
                contentDescription = null,
                tint = goal.accent.onChip,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))

        Text(
            text = goal.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )

        if (goal.isCompletedToday) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = goal.durationLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Last row of the goals card, laid out like a goal row (tile + label) so it
// lines up with the list above it.
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
        Box(
            modifier = Modifier
                .size(FocusLayout.RowIconSize)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))

        Text(
            text = "Add Goal",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
