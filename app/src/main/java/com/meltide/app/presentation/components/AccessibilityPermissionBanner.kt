package com.meltide.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.meltide.app.core.platform.PermissionStatus
import com.meltide.app.presentation.theme.FocusShadow
import com.meltide.app.presentation.theme.FocusSunAccent

/**
 * Nudges the user to turn on the accessibility service app-blocking depends
 * on. The rest of the app stays fully usable without it - blocking just
 * silently doesn't enforce - so this only informs, never gates anything.
 * Re-checks on resume since granting it happens in system Settings, outside
 * this screen's own lifecycle.
 */
@Composable
fun AccessibilityPermissionBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isEnabled by rememberCheckOnResume(PermissionStatus::isAccessibilityServiceEnabled)

    if (isEnabled) return

    FocusSurface(
        modifier = modifier.fillMaxWidth(),
        depth = Depth.Low,
        onClickLabel = "Open accessibility settings",
        onClick = {
            println("Clicked: Accessibility permission banner")
            context.startActivity(PermissionStatus.accessibilitySettingsIntent())
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = FocusLayout.CardHorizontalPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(color = FocusSunAccent, glowColor = FocusSunAccent) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = FocusShadow,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(FocusLayout.RowIconLabelGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "App blocking isn't on yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Turn on Meltide in Accessibility settings to block the apps you've chosen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
