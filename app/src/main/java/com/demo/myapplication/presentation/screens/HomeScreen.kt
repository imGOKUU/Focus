package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.myapplication.presentation.components.AccessibilityPermissionBanner
import com.demo.myapplication.presentation.components.ResponsiveScreen
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
    ResponsiveScreen {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable {
                println("Clicked: Home Back icon")
                onBack()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(28.dp))

        if (goals.isNotEmpty()) {
            AccessibilityPermissionBanner()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // fill = false: the list only claims as much of the flexible space as
        // its content needs (so a short list stays compact and the trailing
        // spacer still pushes Continue to the bottom); a long list (6+ goals)
        // grows to fill what's available and scrolls internally instead of
        // overflowing the screen.
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(goals) { goal ->
                GoalRow(goal = goal, onClick = {
                    println("Clicked: Home goal row - ${goal.name}")
                    onGoalClick(goal)
                })
                HorizontalDivider(color = Color(0xFFEDEAE1), thickness = 1.dp)
            }
        }

        Row(
            modifier = Modifier
                .padding(vertical = 13.dp)
                .clickable {
                    println("Clicked: Home Add Goal")
                    onAddGoal()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = "Add Goal",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GoalRow(goal: GoalListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(goal.accent.chipBackground, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = goal.icon,
                contentDescription = null,
                tint = goal.accent.onChip,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = goal.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
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
