package com.meltide.app.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meltide.app.presentation.components.CardRowDivider
import com.meltide.app.presentation.components.Depth
import com.meltide.app.presentation.components.FocusBackButton
import com.meltide.app.presentation.components.FocusLayout
import com.meltide.app.presentation.components.FocusPrimaryButton
import com.meltide.app.presentation.components.FocusSurface
import com.meltide.app.presentation.components.IconTile
import com.meltide.app.presentation.components.ResponsiveScreen
import com.meltide.app.presentation.components.SectionCard
import com.meltide.app.presentation.components.SectionHeader
import com.meltide.app.presentation.components.entrance
import com.meltide.app.presentation.components.insetSurface
import com.meltide.app.presentation.components.screenVerticalInsets
import com.meltide.app.presentation.components.toShortDuration
import com.meltide.app.presentation.theme.ChipShape
import com.meltide.app.presentation.theme.GoalAccents
import com.meltide.app.presentation.theme.GoalVisuals
import com.meltide.app.presentation.theme.TileShape
import com.meltide.app.presentation.theme.WelcomeHeadline

/**
 * First-run setup, after the intro pages: a name for a friendlier hello,
 * the goals for the day (one tap each, with a duration), the apps that pull
 * the user away, the two permissions, then a "you're all set" moment.
 * No account, no email - it all stays on the phone (spec: "No account, no
 * sign-up, usable immediately").
 */
@Composable
fun SetupScreen(
    state: SetupState,
    onNameChange: (String) -> Unit = {},
    onToggleTemplate: (GoalTemplate) -> Unit = {},
    onAddCustomGoal: (String) -> Unit = {},
    onRemoveGoal: (String) -> Unit = {},
    onSetDuration: (String, Long) -> Unit = { _, _ -> },
    onToggleApp: (String) -> Unit = {},
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onFinish: () -> Unit = {}
) {
    ResponsiveScreen(
        contentModifier = Modifier
            .windowInsetsPadding(screenVerticalInsets())
            .imePadding()
    ) {
        Spacer(modifier = Modifier.height(FocusLayout.ScreenTopGap))

        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(16.dp))
            // Always every step, so the bar doesn't change length when the
            // app step is skipped (no goals picked) - it just fills past it.
            StepProgress(current = state.step.ordinal, total = SetupStep.entries.size, modifier = Modifier.weight(1f))
        }

        // Steps slide in from the side they're coming from.
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(320)) { if (forward) it / 3 else -it / 3 } + fadeIn(tween(320))) togetherWith
                    (slideOutHorizontally(tween(220)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(220)))
            },
            label = "setupStep",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { step ->
            when (step) {
                SetupStep.Name -> ScrollStep { NameStep(name = state.name, onNameChange = onNameChange, onDone = onNext) }
                SetupStep.Goals -> ScrollStep {
                    GoalsStep(
                        goals = state.goals,
                        onToggleTemplate = onToggleTemplate,
                        onAddCustomGoal = onAddCustomGoal,
                        onRemoveGoal = onRemoveGoal,
                        onSetDuration = onSetDuration
                    )
                }
                SetupStep.Distractions -> DistractionsStep(state = state, onToggleApp = onToggleApp)
                SetupStep.Permissions -> ScrollStep { PermissionsStep() }
                SetupStep.Done -> ScrollStep { DoneStep(state = state) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FocusPrimaryButton(
            text = when (state.step) {
                SetupStep.Name -> if (state.name.isBlank()) "Skip for now" else "Continue"
                SetupStep.Goals -> if (state.goals.isEmpty()) "Skip - I'll add goals later" else "Continue"
                SetupStep.Distractions -> if (state.blockedPackages.isEmpty()) "Continue without blocking" else "Continue"
                SetupStep.Permissions -> "Continue"
                SetupStep.Done -> if (state.isSaving) "Setting up..." else "Start my day"
            },
            onClick = if (state.step == SetupStep.Done) onFinish else onNext,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** One segment per step; the current one fills in as you arrive. */
@Composable
private fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val fill by animateFloatAsState(
                targetValue = if (index <= current) 1f else 0f,
                animationSpec = tween(400),
                label = "stepFill"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .insetSurface(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fill)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun ScrollStep(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 28.dp, bottom = 24.dp)
    ) {
        content()
    }
}

@Composable
internal fun SetupHeadline(title: String, body: String) {
    Text(
        text = title,
        style = WelcomeHeadline,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.entrance(index = 0)
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.entrance(index = 0)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Name
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun NameStep(name: String, onNameChange: (String) -> Unit, onDone: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        // A beat after the slide-in, so the keyboard doesn't fight the animation.
        kotlinx.coroutines.delay(350)
        runCatching { focusRequester.requestFocus() }
    }

    SetupHeadline(
        title = "Hi there.\nWhat should we call you?",
        body = "Just for a friendlier hello. It stays on this phone."
    )
    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
    FocusSurface(modifier = Modifier.fillMaxWidth().entrance(index = 1), depth = Depth.Medium) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(color = GoalAccents.Amber.chipBackground, glowColor = GoalAccents.Amber.onChip) {
                Icon(Icons.Outlined.WavingHand, contentDescription = null, tint = GoalAccents.Amber.onChip, modifier = Modifier.size(20.dp))
            }
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = {
                    focusManager.clearFocus()
                    onDone()
                }),
                textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) {
                            Text(
                                text = "Your first name",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
    AnimatedVisibility(visible = name.isNotBlank()) {
        Text(
            text = "Nice to meet you, ${name.trim()}.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Goals
// ═══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalsStep(
    goals: List<SetupGoal>,
    onToggleTemplate: (GoalTemplate) -> Unit,
    onAddCustomGoal: (String) -> Unit,
    onRemoveGoal: (String) -> Unit,
    onSetDuration: (String, Long) -> Unit
) {
    SetupHeadline(
        title = "What do you want\nto give time to?",
        body = "Pick a few to start. Not tasks - things you want to give time to, every day."
    )
    Spacer(modifier = Modifier.height(FocusLayout.SectionGap))

    FlowRow(
        modifier = Modifier.entrance(index = 1),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GoalTemplates.forEach { template ->
            TemplateChip(
                template = template,
                selected = goals.any { it.key == template.key },
                onClick = { onToggleTemplate(template) }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    CustomGoalField(onAdd = onAddCustomGoal, modifier = Modifier.entrance(index = 2))

    AnimatedVisibility(visible = goals.isNotEmpty()) {
        Column {
            Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
            SectionHeader(text = "YOUR DAY", trailing = "${goals.sumOf { it.seconds }.toShortDuration()} a day")
            Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.animateContentSize().padding(vertical = 4.dp)) {
                    goals.forEachIndexed { index, goal ->
                        ChosenGoalRow(goal = goal, onRemove = { onRemoveGoal(goal.key) }, onSetDuration = { onSetDuration(goal.key, it) })
                        if (index < goals.lastIndex) CardRowDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateChip(template: GoalTemplate, selected: Boolean, onClick: () -> Unit) {
    val accent = GoalVisuals.accent(template.colorKey)
    val rim by animateColorAsState(if (selected) accent.onChip else Color.Transparent, label = "chipRim")
    FocusSurface(
        shape = TileShape,
        depth = if (selected) Depth.Medium else Depth.Low,
        onClick = onClick,
        onClickLabel = if (selected) "Remove ${template.name}" else "Add ${template.name}",
        modifier = Modifier.border(1.5.dp, rim, TileShape)
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(color = accent.chipBackground, glowColor = accent.onChip, size = 32.dp) {
                AnimatedContent(targetState = selected, label = "chipIcon") { isSelected ->
                    Icon(
                        imageVector = if (isSelected) Icons.Outlined.Check else GoalVisuals.icon(template.iconKey),
                        contentDescription = null,
                        tint = accent.onChip,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CustomGoalField(onAdd: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by rememberSaveable { mutableStateOf("") }
    val submit = {
        if (text.isNotBlank()) {
            onAdd(text)
            text = ""
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .insetSurface(TileShape)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it.take(40) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = "Something else...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    inner()
                }
            }
        )
        FocusSurface(
            modifier = Modifier.size(36.dp),
            shape = TileShape,
            depth = Depth.Low,
            onClick = submit,
            onClickLabel = "Add goal",
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add goal", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChosenGoalRow(goal: SetupGoal, onRemove: () -> Unit, onSetDuration: (Long) -> Unit) {
    val accent = GoalVisuals.accent(goal.colorKey)
    Column(modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(color = accent.chipBackground, glowColor = accent.onChip, size = 34.dp) {
                Icon(GoalVisuals.icon(goal.iconKey), contentDescription = null, tint = accent.onChip, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${goal.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(6.dp)
                    .size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        // How long each day - the spec's presets, one tap each.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DurationPresets.forEach { seconds ->
                DurationPill(
                    label = seconds.toShortDuration(),
                    selected = goal.seconds == seconds,
                    onClick = { onSetDuration(seconds) }
                )
            }
        }
    }
}

@Composable
private fun DurationPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
        label = "pillBg"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillText"
    )
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
        color = content,
        modifier = Modifier
            .clip(ChipShape)
            .background(background)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════
// Distractions
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DistractionsStep(state: SetupState, onToggleApp: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleApps = remember(state.apps, query) {
        if (query.isBlank()) state.apps else state.apps.filter { it.appName.contains(query.trim(), ignoreCase = true) }
    }
    Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 24.dp)) {
        SetupHeadline(
            title = "What pulls you away?",
            body = "These stay blocked while any of your goals is running. You can fine-tune each goal later."
        )
        Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
        SectionHeader(
            text = "APPS TO BLOCK",
            trailing = if (state.blockedPackages.isEmpty()) null else "${state.blockedPackages.size} selected"
        )
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        SectionCard(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).entrance(index = 1)) {
            Column {
                if (state.apps.size > 8) {
                    AppSearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.padding(start = FocusLayout.CardHorizontalPadding, end = FocusLayout.CardHorizontalPadding, top = 12.dp, bottom = 4.dp)
                    )
                }
                if (!state.appsLoaded) {
                    Text(
                        text = "Finding your apps...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(FocusLayout.CardHorizontalPadding)
                    )
                }
                LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                    itemsIndexed(visibleApps, key = { _, app -> app.packageName }) { index, app ->
                        AppToggleRow(
                            app = BlockableAppItem(
                                appName = app.appName,
                                packageName = app.packageName,
                                icon = app.icon,
                                isBlocked = app.packageName in state.blockedPackages
                            ),
                            onToggle = { onToggleApp(app.packageName) }
                        )
                        if (index < visibleApps.lastIndex) CardRowDivider()
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Done
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun DoneStep(state: SetupState) {
    val badgeScale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) { badgeScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f)) }
    val name = state.name.trim()

    IconTile(
        color = GoalAccents.Green.chipBackground,
        glowColor = GoalAccents.Green.onChip,
        size = 76.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = badgeScale.value
            scaleY = badgeScale.value
        }
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = GoalAccents.Green.onChip, modifier = Modifier.size(36.dp))
    }
    Spacer(modifier = Modifier.height(24.dp))
    SetupHeadline(
        title = if (name.isEmpty()) "You're all set." else "You're all set,\n$name.",
        body = if (state.goals.isEmpty()) {
            "Add your first goal whenever you're ready - it's one tap from Today."
        } else {
            "Here's your day. Start any goal from Today, and Meltide will keep the distractions away until it's done."
        }
    )
    if (state.goals.isNotEmpty()) {
        Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
        SectionCard(modifier = Modifier.fillMaxWidth().entrance(index = 1)) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                state.goals.forEachIndexed { index, goal ->
                    val accent = GoalVisuals.accent(goal.colorKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(color = accent.chipBackground, glowColor = accent.onChip, size = 34.dp) {
                            Icon(GoalVisuals.icon(goal.iconKey), contentDescription = null, tint = accent.onChip, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = goal.seconds.toShortDuration(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < state.goals.lastIndex) CardRowDivider()
                }
            }
        }
        if (state.blockedPackages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.entrance(index = 2)) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = GoalAccents.Amber.onChip, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.blockedPackages.size == 1) "1 app will be blocked during sessions" else "${state.blockedPackages.size} apps will be blocked during sessions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
