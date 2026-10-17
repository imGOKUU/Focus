package com.meltide.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meltide.app.core.platform.PermissionStatus
import com.meltide.app.presentation.components.FocusLayout
import com.meltide.app.presentation.components.PermissionCard
import com.meltide.app.presentation.components.entrance
import com.meltide.app.presentation.components.rememberCheckOnResume
import com.meltide.app.presentation.components.rememberNotificationPermissionRequest
import com.meltide.app.presentation.theme.GoalAccents

/** Accessibility wording shared with Settings - it doubles as the Play prominent disclosure. */
internal const val AccessibilityDisclosure =
    "Meltide uses Android's Accessibility service only to notice which app you open, so it can " +
        "cover the apps you've chosen during a session. It can't read what's on your screen, " +
        "and nothing leaves your phone."

internal const val AccessibilitySteps = "In Settings: Accessibility, then Meltide, then turn it on."

internal const val NotificationExplanation =
    "A quiet notification shows your countdown while a session runs, with Pause and Resume, " +
        "and lets you know when it's done."

/**
 * The setup flow's permission step (spec: "Permission" screen). Both
 * permissions are explained before they're requested, and both are optional
 * - the step's Continue always works; Today keeps a gentle banner until
 * blocking is on.
 */
@Composable
fun PermissionsStep(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val blockingOn by rememberCheckOnResume(PermissionStatus::isAccessibilityServiceEnabled)
    val notificationsOn by rememberCheckOnResume(PermissionStatus::canPostNotifications)
    val requestNotifications = rememberNotificationPermissionRequest()

    Column(modifier = modifier) {
        SetupHeadline(
            title = "Two small things",
            body = "Meltide works best with these. Both are optional, and you can change them any time in Settings."
        )
        Spacer(modifier = Modifier.height(FocusLayout.SectionGap))
        PermissionCard(
            icon = Icons.Outlined.Shield,
            title = "Block distracting apps",
            description = AccessibilityDisclosure,
            footnote = AccessibilitySteps,
            isGranted = blockingOn,
            actionText = "Turn on app blocking",
            onAction = { context.startActivity(PermissionStatus.accessibilitySettingsIntent()) },
            modifier = Modifier.entrance(index = 1)
        )
        Spacer(modifier = Modifier.height(16.dp))
        PermissionCard(
            icon = Icons.Outlined.NotificationsNone,
            title = "See your timer anywhere",
            description = NotificationExplanation,
            isGranted = notificationsOn,
            actionText = "Allow notifications",
            onAction = requestNotifications,
            tileColor = GoalAccents.Blue.chipBackground,
            iconColor = GoalAccents.Blue.onChip,
            modifier = Modifier.entrance(index = 2)
        )
        AnimatedVisibility(visible = !blockingOn) {
            Text(
                text = "Without app blocking, sessions still time your focus - they just won't block apps.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}
