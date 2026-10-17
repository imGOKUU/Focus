package com.demo.myapplication.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.demo.myapplication.presentation.components.FocusPrimaryButton
import com.demo.myapplication.presentation.components.FocusSurface
import com.demo.myapplication.presentation.components.IconTile
import com.demo.myapplication.presentation.components.ValueChip
import com.demo.myapplication.presentation.components.entrance

/**
 * Shown by BlockedAppActivity, launched from AppBlockAccessibilityService the
 * moment a blocked app comes to the foreground during an active (non-paused)
 * session - mirrors the reference "Blocked app screen" design.
 */
@Composable
fun BlockedAppScreen(
    appName: String,
    appIcon: ImageBitmap?,
    goalName: String,
    remainingSeconds: Long,
    onGoBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconTile(
            color = MaterialTheme.colorScheme.surfaceContainer,
            size = 88.dp,
            modifier = Modifier.entrance(index = 0)
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Text(
                    text = appName.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        FocusSurface(
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 1)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Not right now",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "You're in a $goalName session.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                ValueChip(text = remainingSeconds.toRemainingLabel())
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        FocusPrimaryButton(
            text = "Go back",
            onClick = {
                println("Clicked: BlockedApp Go Back button")
                onGoBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 2)
        )
    }
}

private fun Long.toRemainingLabel(): String {
    val minutes = this / 60
    return when {
        this <= 0L -> "This session is wrapping up."
        minutes < 1L -> "Under a minute left."
        minutes == 1L -> "1 minute left."
        else -> "$minutes minutes left."
    }
}
