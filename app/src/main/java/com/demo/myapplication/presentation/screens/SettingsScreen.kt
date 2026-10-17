package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import com.demo.myapplication.presentation.components.Depth
import com.demo.myapplication.presentation.components.FocusPrimaryButton
import com.demo.myapplication.presentation.components.FocusSecondaryButton
import com.demo.myapplication.presentation.components.FocusSurface
import com.demo.myapplication.presentation.components.insetSurface
import com.demo.myapplication.presentation.theme.TileShape
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.demo.myapplication.core.datastore.AppSettings
import com.demo.myapplication.core.platform.PermissionStatus
import com.demo.myapplication.presentation.components.CardRowDivider
import com.demo.myapplication.presentation.components.FocusBackButton
import com.demo.myapplication.presentation.components.FocusLayout
import com.demo.myapplication.presentation.components.IconTile
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.components.SectionCard
import com.demo.myapplication.presentation.components.SectionHeader
import com.demo.myapplication.presentation.components.ValueChip
import com.demo.myapplication.presentation.components.entrance
import com.demo.myapplication.presentation.components.rememberCheckOnResume
import com.demo.myapplication.presentation.components.rememberNotificationPermissionRequest
import com.demo.myapplication.presentation.components.screenVerticalInsets
import com.demo.myapplication.presentation.theme.FocusDanger
import com.demo.myapplication.presentation.theme.FocusSoftAccent
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalAccents

/**
 * Settings (spec section 4: App Blocking, Notifications, Minimum Mode,
 * About). Appearance isn't offered yet - there's no designed dark theme, and
 * a toggle that switches to an unfinished one would be worse than none.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit = {},
    onSessionNotificationChange: (Boolean) -> Unit = {},
    onCompletionAlertChange: (Boolean) -> Unit = {},
    onStartInMinimalChange: (Boolean) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onReplayIntro: () -> Unit = {},
    userName: String = "",
    onUserNameChange: (String) -> Unit = {}
) {
    var editingName by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val blockingOn by rememberCheckOnResume(PermissionStatus::isAccessibilityServiceEnabled)
    val notificationsOn by rememberCheckOnResume(PermissionStatus::canPostNotifications)
    val requestNotifications = rememberNotificationPermissionRequest()
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }

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
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp, bottom = 24.dp)
            ) {
                SettingsSection(title = "PERMISSIONS", index = 1) {
                    StatusRow(
                        icon = Icons.Outlined.Shield,
                        accent = GoalAccents.Amber,
                        title = "App blocking",
                        subtitle = if (blockingOn) "Blocked apps are covered during sessions" else "Off - sessions won't block apps",
                        isOn = blockingOn,
                        onClick = { context.startActivity(PermissionStatus.accessibilitySettingsIntent()) }
                    )
                    CardRowDivider()
                    StatusRow(
                        icon = Icons.Outlined.NotificationsNone,
                        accent = GoalAccents.Blue,
                        title = "Notifications",
                        subtitle = if (notificationsOn) "Allowed" else "Off - you won't see the countdown outside the app",
                        isOn = notificationsOn,
                        onClick = requestNotifications
                    )
                }

                SettingsSection(title = "SESSIONS", index = 2) {
                    ToggleRow(
                        icon = Icons.Outlined.Schedule,
                        accent = GoalAccents.Teal,
                        title = "Countdown notification",
                        subtitle = if (notificationsOn) "Shows time left in your notifications, with Pause and Resume" else "Allow notifications above to use this",
                        checked = settings.sessionNotification,
                        enabled = notificationsOn,
                        onCheckedChange = onSessionNotificationChange
                    )
                    CardRowDivider()
                    ToggleRow(
                        icon = Icons.Outlined.TaskAlt,
                        accent = GoalAccents.Green,
                        title = "Tell me when it's done",
                        subtitle = if (notificationsOn) "A quiet note if a session finishes while you're in another app" else "Allow notifications above to use this",
                        checked = settings.completionAlert,
                        enabled = notificationsOn,
                        onCheckedChange = onCompletionAlertChange
                    )
                    CardRowDivider()
                    ToggleRow(
                        icon = Icons.Outlined.DarkMode,
                        accent = GoalAccents.Purple,
                        title = "Start in minimal mode",
                        subtitle = "Open sessions on the dark, timer-only screen",
                        checked = settings.startInMinimalMode,
                        onCheckedChange = onStartInMinimalChange
                    )
                    CardRowDivider()
                    ToggleRow(
                        icon = Icons.Outlined.WbSunny,
                        accent = GoalAccents.Rose,
                        title = "Keep screen on",
                        subtitle = "While the session screen is open",
                        checked = settings.keepScreenOn,
                        onCheckedChange = onKeepScreenOnChange
                    )
                }

                SettingsSection(title = "ABOUT", index = 3) {
                    NavigationRow(
                        icon = Icons.Outlined.Person,
                        accent = GoalAccents.Amber,
                        title = "Your name",
                        subtitle = userName.ifBlank { "Not set - used for your greeting on Today" },
                        onClick = { editingName = true }
                    )
                    CardRowDivider()
                    NavigationRow(
                        icon = Icons.Outlined.Replay,
                        accent = GoalAccents.Teal,
                        title = "Replay the intro",
                        subtitle = "See the welcome and permission steps again",
                        onClick = onReplayIntro
                    )
                    CardRowDivider()
                    InfoRow(
                        icon = Icons.Outlined.Lock,
                        accent = GoalAccents.Green,
                        title = "Private by design",
                        subtitle = "No account, no tracking. Your goals and history stay on this phone."
                    )
                    CardRowDivider()
                    InfoRow(
                        icon = Icons.Outlined.Info,
                        accent = GoalAccents.Blue,
                        title = "Version",
                        subtitle = version
                    )
                }
            }
        }
    }

    if (editingName) {
        NameDialog(
            initial = userName,
            onSave = {
                onUserNameChange(it)
                editingName = false
            },
            onDismiss = { editingName = false }
        )
    }
}

@Composable
private fun NameDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        FocusSurface(modifier = Modifier.fillMaxWidth(), depth = Depth.High) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Your name", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.take(30) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .insetSurface(TileShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FocusSecondaryButton(text = "Cancel", onClick = onDismiss, modifier = Modifier.weight(1f))
                    FocusPrimaryButton(text = "Save", onClick = { onSave(text) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, index: Int, content: @Composable () -> Unit) {
    Column(modifier = Modifier.entrance(index = index)) {
        SectionHeader(text = title)
        Spacer(modifier = Modifier.height(FocusLayout.SectionLabelGap))
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) { content() }
        }
        Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
    }
}

@Composable
private fun RowLayout(
    icon: ImageVector,
    accent: GoalAccent,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = FocusLayout.RowMinHeight)
            .padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(color = accent.chipBackground, glowColor = accent.onChip) {
            Icon(icon, contentDescription = null, tint = accent.onChip, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.6f)
            )
        }
        trailing()
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    accent: GoalAccent,
    title: String,
    subtitle: String,
    isOn: Boolean,
    onClick: () -> Unit
) {
    RowLayout(
        icon = icon,
        accent = accent,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.clickable(onClickLabel = "Open system settings", onClick = onClick)
    ) {
        ValueChip(
            text = if (isOn) "On" else "Off",
            contentColor = if (isOn) GoalAccents.Green.onChip else FocusDanger
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp).size(20.dp)
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    accent: GoalAccent,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    // The whole row toggles, announced as one switch.
    RowLayout(
        icon = icon,
        accent = accent,
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    ) {
        Switch(
            checked = checked && enabled,
            onCheckedChange = null,
            enabled = enabled,
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

@Composable
private fun NavigationRow(
    icon: ImageVector,
    accent: GoalAccent,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    RowLayout(
        icon = icon,
        accent = accent,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, accent: GoalAccent, title: String, subtitle: String) {
    RowLayout(icon = icon, accent = accent, title = title, subtitle = subtitle)
}
