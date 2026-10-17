package com.meltide.app.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import com.meltide.app.presentation.components.Depth
import com.meltide.app.presentation.components.FocusBottomSheet
import com.meltide.app.presentation.components.FocusDialog
import com.meltide.app.presentation.components.FocusSurface
import com.meltide.app.presentation.components.SectionLabel
import com.meltide.app.presentation.theme.FocusDanger
import com.meltide.app.presentation.theme.GoalVisuals
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.meltide.app.presentation.components.FocusBackButton
import com.meltide.app.presentation.components.FocusPrimaryButton
import com.meltide.app.presentation.components.IconTile
import com.meltide.app.presentation.components.SectionHeader
import com.meltide.app.presentation.components.entrance
import com.meltide.app.presentation.components.insetSurface
import com.meltide.app.presentation.theme.CardShape
import com.meltide.app.presentation.theme.FocusShadow
import com.meltide.app.presentation.theme.FocusSoftAccent
import com.meltide.app.presentation.theme.TileShape
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
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
import com.meltide.app.presentation.components.CardRowDivider
import com.meltide.app.presentation.components.FocusLayout
import com.meltide.app.presentation.components.ResponsiveScreen
import com.meltide.app.presentation.components.SectionCard
import com.meltide.app.presentation.components.screenVerticalInsets
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
    isEditing: Boolean = false,
    iconKey: String = GoalVisuals.DefaultIcon,
    colorKey: String = GoalVisuals.accents.first().first,
    onIconChange: (String) -> Unit = {},
    onColorChange: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    onSave: () -> Unit = {}
) {
    var attemptedSave by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    val isNameValid = goalName.isNotBlank()
    val isDurationValid = selectedHours > 0 || selectedMinutes > 0

    ResponsiveScreen(
        // The app draws edge-to-edge (targetSdk 35+), so keep the back icon
        // and Save button clear of the status and navigation bars.
        contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())
    ) {
        Spacer(modifier = Modifier.height(FocusLayout.ScreenTopGap))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FocusBackButton(onClick = {
                println("Clicked: GoalConfig Back icon")
                onBack()
            })
            Text(
                text = if (isEditing) "Edit goal" else "New goal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            if (isEditing) {
                FocusSurface(
                    modifier = Modifier.size(44.dp),
                    shape = TileShape,
                    depth = Depth.Low,
                    onClick = { confirmDelete = true },
                    onClickLabel = "Delete goal",
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete goal",
                        tint = FocusDanger,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(FocusLayout.HeaderGap))

        GoalNameSection(
            goalName = goalName,
            onGoalNameChange = onGoalNameChange,
            iconKey = iconKey,
            colorKey = colorKey,
            onIconClick = { showAppearance = true },
            showError = attemptedSave && !isNameValid,
            modifier = Modifier.entrance(index = 1)
        )

        Spacer(modifier = Modifier.height(FocusLayout.SectionGap))

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
                    Column(modifier = Modifier.weight(1f).entrance(index = 2)) {
                        dailyTime(3, compactItemHeight, CompactCardVerticalPadding)
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().entrance(index = 3)) {
                        AppsToBlockSection(blockableApps, onToggleApp)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.entrance(index = 2)) {
                        dailyTime(5, WheelItemHeight, CardVerticalPadding)
                    }
                    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
                    Column(modifier = Modifier.weight(1f).entrance(index = 3)) {
                        AppsToBlockSection(blockableApps, onToggleApp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // A failed save shakes the button (and the errors above animate in),
        // so it's obvious the tap registered and what still needs doing.
        val shake = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        FocusPrimaryButton(
            text = if (isEditing) "Save changes" else "Save goal",
            onClick = {
                println("Clicked: GoalConfig Save button")
                if (isNameValid && isDurationValid) {
                    onSave()
                } else {
                    attemptedSave = true
                    scope.launch {
                        shake.snapTo(0f)
                        shake.animateTo(0f, keyframes {
                            durationMillis = 360
                            -10f at 50
                            10f at 110
                            -7f at 170
                            7f at 230
                            -3f at 290
                        })
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = shake.value.dp.toPx() }
                .entrance(index = 4)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (confirmDelete) {
        FocusDialog(
            title = "Delete ${goalName.ifBlank { "this goal" }}?",
            message = "It'll be removed from Today. Time you've already given it stays in your history.",
            confirmText = "Delete",
            dismissText = "Keep",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false }
        )
    }

    if (showAppearance) {
        GoalAppearanceSheet(
            iconKey = iconKey,
            colorKey = colorKey,
            onIconChange = onIconChange,
            onColorChange = onColorChange,
            onDismiss = { showAppearance = false }
        )
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
    SectionHeader(
        text = "DAILY TIME",
        trailing = formatDailyDuration(selectedHours, selectedMinutes)
    )
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
    AnimatedVisibility(visible = showError) {
        Text(
            text = "Pick a daily time above zero",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ColumnScope.AppsToBlockSection(
    blockableApps: List<BlockableAppItem>,
    onToggleApp: (BlockableAppItem) -> Unit
) {
    val blockedCount = blockableApps.count { it.isBlocked }
    SectionHeader(
        text = "APPS TO BLOCK",
        trailing = if (blockedCount > 0) "$blockedCount selected" else null
    )
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
            var query by rememberSaveable { mutableStateOf("") }
            val visibleApps = remember(blockableApps, query) {
                if (query.isBlank()) blockableApps
                else blockableApps.filter { it.appName.contains(query.trim(), ignoreCase = true) }
            }
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
                Column {
                    // Only worth the space once the list is long enough to hunt through.
                    if (blockableApps.size > SearchThreshold) {
                        AppSearchField(
                            query = query,
                            onQueryChange = { query = it },
                            modifier = Modifier.padding(
                                start = FocusLayout.CardHorizontalPadding,
                                end = FocusLayout.CardHorizontalPadding,
                                top = 12.dp,
                                bottom = 4.dp
                            )
                        )
                    }
                    if (visibleApps.isEmpty()) {
                        Text(
                            text = "No apps match \"${query.trim()}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 20.dp)
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                            AppToggleRow(app = app, onToggle = { onToggleApp(app) })
                            if (index < visibleApps.lastIndex) {
                                CardRowDivider()
                            }
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
                .insetSurface(TileShape)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Wheel(
                values = hourOptions,
                selectedValue = selectedHours,
                onSelectedValueChange = onHoursChange,
                label = { "$it" },
                unit = if (selectedHours == 1) "hour" else "hours",
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
internal fun AppToggleRow(app: BlockableAppItem, onToggle: () -> Unit) {
    // The whole row is the toggle (not just the small switch), and is
    // announced as one switch with the app's name to screen readers.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = app.isBlocked,
                role = Role.Switch,
                onValueChange = {
                    println("Clicked: GoalConfig app toggle - ${app.appName}")
                    onToggle()
                }
            )
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(
            color = if (app.icon != null) MaterialTheme.colorScheme.surfaceContainer else app.fallbackAccentColor(),
            glowColor = if (app.icon != null) FocusShadow else app.fallbackAccentColor()
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
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedBorderColor = Color.Transparent,
                uncheckedTrackColor = FocusSoftAccent,
                uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

// "1h 30m a day" - the live summary beside the DAILY TIME label.
private fun formatDailyDuration(hours: Int, minutes: Int): String = when {
    hours > 0 && minutes > 0 -> "${hours}h ${minutes}m a day"
    hours > 0 -> "${hours}h a day"
    minutes > 0 -> "${minutes}m a day"
    else -> "Not set"
}

/**
 * The goal's name as a labelled input card. The card's rim warms to the
 * primary color while it's being edited, so focus is visible at a glance.
 */
@Composable
private fun GoalNameSection(
    goalName: String,
    onGoalNameChange: (String) -> Unit,
    iconKey: String,
    colorKey: String,
    onIconClick: () -> Unit,
    showError: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val rimColor by animateColorAsState(
        targetValue = when {
            showError -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else -> Color.Transparent
        },
        label = "nameRim"
    )

    Column(modifier = modifier) {
        SectionHeader(text = "GOAL NAME")
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .border(1.5.dp, rimColor, CardShape)
                  .padding(start = 12.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
            // The goal's icon and color - tap to change. Shown live here so
            // the choice is visible right beside the name it belongs to.
            val accent = GoalVisuals.accent(colorKey)
            IconTile(
                color = accent.chipBackground,
                glowColor = accent.onChip,
                modifier = Modifier
                    .clip(TileShape)
                    .clickable(onClickLabel = "Change icon and color", onClick = onIconClick)
            ) {
                Icon(GoalVisuals.icon(iconKey), contentDescription = "Goal icon", tint = accent.onChip, modifier = Modifier.size(20.dp))
            }
            BasicTextField(
                value = goalName,
                onValueChange = onGoalNameChange,
                singleLine = true,
                interactionSource = interactionSource,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (goalName.isEmpty()) {
                            Text(
                                text = "e.g. Mathematics",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
          }
        }
        AnimatedVisibility(visible = showError) {
            Text(
                text = "Give this goal a name",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private const val SearchThreshold = 8

@Composable
internal fun AppSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .insetSurface(TileShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Clear search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onQueryChange("") }
            )
        }
    }
}

/** Icon grid and color swatches for a goal - opened from the tile beside its name. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalAppearanceSheet(
    iconKey: String,
    colorKey: String,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val accent = GoalVisuals.accent(colorKey)
    FocusBottomSheet(onDismiss = onDismiss) {
        Text(
            text = "Icon & color",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel("COLOR")
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalVisuals.accents.forEach { (key, option) ->
                val selected = key == colorKey
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (selected) option.onChip else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(option.onChip)
                        .clickable(onClickLabel = "Use $key") { onColorChange(key) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel("ICON")
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GoalVisuals.icons.forEach { (key, icon) ->
                val selected = key == iconKey
                IconTile(
                    color = if (selected) accent.chipBackground else MaterialTheme.colorScheme.surfaceContainer,
                    glowColor = if (selected) accent.onChip else FocusShadow,
                    size = 48.dp,
                    modifier = Modifier
                        .border(2.dp, if (selected) accent.onChip else Color.Transparent, TileShape)
                        .clip(TileShape)
                        .clickable { onIconChange(key) }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = key.replace('_', ' '),
                        tint = if (selected) accent.onChip else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        FocusPrimaryButton(text = "Done", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}
