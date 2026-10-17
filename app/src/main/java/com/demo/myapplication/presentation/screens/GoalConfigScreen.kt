package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.myapplication.presentation.components.CardRowDivider
import com.demo.myapplication.presentation.components.FocusLayout
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.components.SectionCard
import com.demo.myapplication.presentation.components.SectionLabel
import com.demo.myapplication.presentation.components.screenVerticalInsets
import com.demo.myapplication.presentation.theme.ButtonLabel
import com.demo.myapplication.presentation.theme.ButtonPillShape
import kotlin.math.abs

/**
 * Pure UI shape for one row in the "Apps to Block" list. The caller (a
 * ViewModel reading real installed apps via InstalledAppsProvider, merged
 * with this goal's saved BlockedApp rows) builds these — this screen never
 * queries anything itself. icon is nullable so a load failure degrades to
 * an accent-colored initial-letter fallback instead of crashing the row.
 */
data class BlockableAppItem(
    val appName: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val isBlocked: Boolean
)
private val knownAppAccents = mapOf(
    "com.instagram.android" to Color(0xFFC1447E),
    "com.google.android.youtube" to Color(0xFFE1483B),
    "com.reddit.frontpage" to Color(0xFFE06A2C),
    "com.twitter.android" to Color(0xFF000000),
    "com.android.chrome" to Color(0xFF4C8BF5),
    "com.whatsapp" to Color(0xFF3FA96A)
)

private val fallbackAccentPalette = listOf(
    Color(0xFFC1447E), Color(0xFFE1483B), Color(0xFFE06A2C),
    Color(0xFF4C8BF5), Color(0xFF3FA96A), Color(0xFF6B4FA0), Color(0xFF35619E)
)

private fun BlockableAppItem.fallbackAccentColor(): Color =
    knownAppAccents[packageName]
        ?: fallbackAccentPalette[abs(packageName.hashCode()) % fallbackAccentPalette.size]

private val CardVerticalPadding = 12.dp
private val CompactCardVerticalPadding = 4.dp
private val SectionLabelHeight = 16.dp
private val ErrorLineHeight = 22.dp


@Composable
fun GoalConfigScreen(
    goalName: String,
    onGoalNameChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    hourOptions: List<Int> = (0..12).toList(),
    minuteOptions: List<Int> = (0..55 step 1).toList(),
    selectedHours: Int = 1,
    selectedMinutes: Int = 0,
    onHoursChange: (Int) -> Unit = {},
    onMinutesChange: (Int) -> Unit = {},
    blockableApps: List<BlockableAppItem> = emptyList(),
    onToggleApp: (BlockableAppItem) -> Unit = {},
    onSave: () -> Unit = {}
) {
    var attemptedSave by remember { mutableStateOf(false) }
    val isNameValid = goalName.isNotBlank()
    val isDurationValid = selectedHours > 0 || selectedMinutes > 0

    ResponsiveScreen(
        // The app draws edge-to-edge (targetSdk 35+), so keep the back icon
        // and Save button clear of the status and navigation bars.
        contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable {
                println("Clicked: GoalConfig Back icon")
                onBack()
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        BasicTextField(
            value = goalName,
            onValueChange = onGoalNameChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (goalName.isEmpty()) {
                    Text(
                        text = "Goal name",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )

        if (attemptedSave && !isNameValid) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Give this goal a name",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // The picker + app list region takes all height between the name
        // field and Save. Only the app list inside it scrolls; the name
        // field, picker, and Save button stay put.
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val sideBySide = maxHeight < FocusLayout.SideBySideHeightThreshold
            val showDurationError = attemptedSave && !isDurationValid

            val dailyTime: @Composable ColumnScope.(Int, Dp, Dp) -> Unit = { visibleRows, itemHeight, verticalPadding ->
                DailyTimeSection(
                    hourOptions = hourOptions,
                    minuteOptions = minuteOptions,
                    selectedHours = selectedHours,
                    selectedMinutes = selectedMinutes,
                    onHoursChange = onHoursChange,
                    onMinutesChange = onMinutesChange,
                    visibleRows = visibleRows,
                    itemHeight = itemHeight,
                    verticalPadding = verticalPadding,
                    showError = showDurationError
                )
            }

            if (sideBySide) {
                // Fit a 3-row wheel into whatever height is left so the card
                // never gets squeezed below its content (label + gap + card
                // padding + optional error line are subtracted first).
                val reserved = SectionLabelHeight + FocusLayout.SectionLabelGap + CompactCardVerticalPadding * 2 +
                    (if (showDurationError) ErrorLineHeight else 0.dp)
                val compactItemHeight = ((maxHeight - reserved) / 3).coerceIn(MinWheelItemHeight, WheelItemHeight)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        dailyTime(3, compactItemHeight, CompactCardVerticalPadding)
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AppsToBlockSection(blockableApps, onToggleApp)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    dailyTime(5, WheelItemHeight, CardVerticalPadding)
                    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
                    AppsToBlockSection(blockableApps, onToggleApp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                println("Clicked: GoalConfig Save button")
                if (isNameValid && isDurationValid) {
                    onSave()
                } else {
                    attemptedSave = true
                }
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
                text = "Save",
                style = ButtonLabel
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnScope.DailyTimeSection(
    hourOptions: List<Int>,
    minuteOptions: List<Int>,
    selectedHours: Int,
    selectedMinutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    visibleRows: Int,
    itemHeight: Dp,
    verticalPadding: Dp,
    showError: Boolean
) {
    SectionLabel("DAILY TIME")
    Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        DailyTimeWheelPicker(
            hourOptions = hourOptions,
            minuteOptions = minuteOptions,
            selectedHours = selectedHours,
            selectedMinutes = selectedMinutes,
            onHoursChange = onHoursChange,
            onMinutesChange = onMinutesChange,
            visibleRows = visibleRows,
            itemHeight = itemHeight,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = verticalPadding)
        )
    }
    if (showError) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Pick a daily time above zero",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ColumnScope.AppsToBlockSection(
    blockableApps: List<BlockableAppItem>,
    onToggleApp: (BlockableAppItem) -> Unit
) {
    SectionLabel("APPS TO BLOCK")
    Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
    // The weight lives on this Box, not the card: the Box claims all the
    // remaining height (keeping Save anchored), while the card inside only
    // wraps its content - a short list gets a short card, a long list is
    // capped at the available height and scrolls inside the card's clip.
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            if (blockableApps.isEmpty()) {
                Text(
                    text = "No apps to show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = FocusLayout.CardHorizontalPadding,
                        vertical = 20.dp
                    )
                )
            } else {
                // Every real launchable app on the device (potentially 100+,
                // see DECISIONS.md) - bounded and independently scrolling.
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    itemsIndexed(blockableApps, key = { _, app -> app.packageName }) { index, app ->
                        AppToggleRow(app = app, onToggle = { onToggleApp(app) })
                        if (index < blockableApps.lastIndex) {
                            CardRowDivider()
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Daily-time picker: two independent scroll-snap wheels (hours x minutes)
// under one shared selection band, with fixed unit labels beside the
// centered values - one duration selector rather than two lists.
// ═══════════════════════════════════════════════════════════════════════

private val WheelItemHeight = 44.dp
private val MinWheelItemHeight = 32.dp
private val WheelNumberSlotWidth = 44.dp
private val WheelUnitSlotWidth = 52.dp

@Composable
private fun DailyTimeWheelPicker(
    hourOptions: List<Int>,
    minuteOptions: List<Int>,
    selectedHours: Int,
    selectedMinutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    visibleRows: Int,
    itemHeight: Dp,
    modifier: Modifier = Modifier
) {
    // Sized by the wheels; the band is centered on their middle row.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Drawn once behind both wheels so it reads as a single selection
        // band across the row, rather than two separate highlights.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.medium
                )
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Wheel(
                values = hourOptions,
                selectedValue = selectedHours,
                onSelectedValueChange = onHoursChange,
                label = { "$it" },
                unit = "hours",
                visibleRows = visibleRows,
                itemHeight = itemHeight,
                modifier = Modifier.weight(1f)
            )
            Wheel(
                values = minuteOptions,
                selectedValue = selectedMinutes,
                onSelectedValueChange = onMinutesChange,
                label = { "%02d".format(it) },
                unit = "min",
                visibleRows = visibleRows,
                itemHeight = itemHeight,
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
    unit: String,
    visibleRows: Int,
    itemHeight: Dp,
    modifier: Modifier = Modifier
) {
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex
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

    val selectedStyle = MaterialTheme.typography.titleLarge.copy(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum"
    )
    val unselectedStyle = selectedStyle.copy(fontWeight = FontWeight.Normal)
    val selectedColor = MaterialTheme.colorScheme.onSurface
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = modifier.height(itemHeight * visibleRows), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleRows / 2)),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(values) { index, value ->
                val distance = abs(index - centeredIndex)
                val isSelected = distance == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .graphicsLayer {
                            // Rows recede as they move away from the band.
                            val scale = if (isSelected) 1f else 0.9f
                            scaleX = scale
                            scaleY = scale
                            alpha = when (distance) {
                                0 -> 1f
                                1 -> 0.8f
                                else -> 0.4f
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.width(WheelNumberSlotWidth),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = label(value),
                            style = if (isSelected) selectedStyle else unselectedStyle,
                            color = if (isSelected) selectedColor else mutedColor,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.width(WheelUnitSlotWidth))
                }
            }
        }

        // Fixed unit label beside the centered value; it doesn't consume
        // touches, so drags over it still scroll the wheel underneath.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(WheelNumberSlotWidth))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = selectedColor,
                maxLines = 1,
                modifier = Modifier
                    .width(WheelUnitSlotWidth)
                    .padding(start = 8.dp)
            )
        }
    }
}


@Composable
private fun AppToggleRow(app: BlockableAppItem, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(FocusLayout.RowIconSize)
                .background(
                    if (app.icon != null) MaterialTheme.colorScheme.surfaceVariant else app.fallbackAccentColor(),
                    shape = RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Text(
                    text = app.appName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))

        Text(
            text = app.appName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )

        Switch(
            checked = app.isBlocked,
            onCheckedChange = {
                println("Clicked: GoalConfig app toggle - ${app.appName}")
                onToggle()
            },
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = Color(0xFFDDE5EC),
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
