package com.meltide.app.presentation.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meltide.app.core.platform.PermissionStatus
import com.meltide.app.presentation.theme.GoalAccents

/**
 * Explains one permission *before* asking for it (spec: "explain why
 * before asking"; also what Play expects as a prominent disclosure for the
 * Accessibility API), then offers the one button that grants it.
 */
@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    tileColor: Color = GoalAccents.Amber.chipBackground,
    iconColor: Color = GoalAccents.Amber.onChip,
    footnote: String? = null
) {
    FocusSurface(modifier = modifier.fillMaxWidth(), depth = Depth.Medium) {
        Column(modifier = Modifier.padding(FocusLayout.CardHorizontalPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(color = tileColor, glowColor = iconColor) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (footnote != null && !isGranted) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            AnimatedContent(
                targetState = isGranted,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "permissionState"
            ) { granted ->
                if (granted) {
                    ValueChip(text = "On", icon = Icons.Outlined.Check, contentColor = GoalAccents.Green.onChip)
                } else {
                    FocusSecondaryButton(
                        text = actionText,
                        onClick = onAction,
                        height = 44.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Returns a function that asks for notifications the best way available:
 * the runtime prompt on Android 13+ the first time, and the app's
 * notification settings once the prompt can't be shown again (Android
 * silently ignores repeat requests after the user declines) or when
 * notifications are switched off at the app level.
 */
@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    var requests by rememberSaveable { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    return {
        if (requests == 0 && !PermissionStatus.hasNotificationRuntimePermission(context)) {
            requests++
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(PermissionStatus.notificationSettingsIntent(context))
        }
    }
}
