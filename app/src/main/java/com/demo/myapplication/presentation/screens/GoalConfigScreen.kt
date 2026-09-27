package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.myapplication.presentation.theme.ButtonPillShape
import com.demo.myapplication.presentation.theme.FocusSoftAccent
import kotlin.math.abs

/**
 * Pure UI shape for one row in the "Apps to Block" list. The caller (a
 * ViewModel reading real installed apps via InstalledAppsProvider, merged
 * with this goal's saved BlockedApp rows) builds these — this screen never
 * queries anything itself. icon is nullable so a load failure degrades to
 * an initial-letter fallback instead of crashing the row.
 */
data class BlockableAppItem(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val isBlocked: Boolean
)

@Composable
fun GoalConfigScreen(
    goalName: String,
    onGoalNameChange: (String) -> Unit = {},
    hourOptions: List<Int> = (0..4).toList(),
    minuteOptions: List<Int> = (0..55 step 5).toList(),
    selectedHours: Int = 1,
    selectedMinutes: Int = 0,
    onHoursChange: (Int) -> Unit = {},
    onMinutesChange: (Int) -> Unit = {},
    blockableApps: List<BlockableAppItem> = emptyList(),
    onToggleApp: (BlockableAppItem) -> Unit = {},
    onSave: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        BasicTextField(
            value = goalName,
            onValueChange = onGoalNameChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (goalName.isEmpty()) {
                    Text(
                        text = "Goal name",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        SectionLabel("DAILY TIME")

        Spacer(modifier = Modifier.height(8.dp))

        DailyTimeWheelPicker(
            hourOptions = hourOptions,
            minuteOptions = minuteOptions,
            selectedHours = selectedHours,
            selectedMinutes = selectedMinutes,
            onHoursChange = onHoursChange,
            onMinutesChange = onMinutesChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(FocusSoftAccent.copy(alpha = 0.4f), shape = RoundedCornerShape(16.dp))
                .padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        SectionLabel("APPS TO BLOCK")

        Spacer(modifier = Modifier.height(12.dp))

        Column {
            blockableApps.forEach { app ->
                AppToggleRow(app = app, onToggle = { onToggleApp(app) })
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = ButtonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private val WheelItemHeight = 48.dp

@Composable
private fun DailyTimeWheelPicker(
    hourOptions: List<Int>,
    minuteOptions: List<Int>,
    selectedHours: Int,
    selectedMinutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(WheelItemHeight * 3), contentAlignment = Alignment.Center) {
        // Drawn once behind both wheels so it reads as a single selection
        // band across the row, rather than two separate highlights.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Wheel(
                values = hourOptions,
                selectedValue = selectedHours,
                onSelectedValueChange = onHoursChange,
                label = { "$it h" },
                modifier = Modifier.weight(1f)
            )
            Wheel(
                values = minuteOptions,
                selectedValue = selectedMinutes,
                onSelectedValueChange = onMinutesChange,
                label = { "%02d m".format(it) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One scrollable reel. The centered value is derived from which item's
 * midpoint sits closest to the viewport's midpoint — robust across
 * contentPadding and mid-fling states, unlike reading
 * firstVisibleItemIndex/scrollOffset directly.
 */
@Composable
private fun Wheel(
    values: List<Int>,
    selectedValue: Int,
    onSelectedValueChange: (Int) -> Unit,
    label: (Int) -> String,
    modifier: Modifier = Modifier
) {
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialIndex - 1).coerceAtLeast(0)
    )

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf initialIndex
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index
                ?: initialIndex
        }
    }

    LaunchedEffect(centeredIndex) {
        onSelectedValueChange(values[centeredIndex.coerceIn(values.indices)])
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = WheelItemHeight),
        modifier = modifier.height(WheelItemHeight * 3)
    ) {
        itemsIndexed(values) { index, value ->
            val isSelected = index == centeredIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(value),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 16.sp
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun AppToggleRow(app: BlockableAppItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = app.appName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = app.appName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        Switch(
            checked = app.isBlocked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}