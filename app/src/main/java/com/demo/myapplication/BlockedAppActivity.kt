package com.demo.myapplication

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.demo.myapplication.presentation.screens.BlockedAppScreen
import com.demo.myapplication.presentation.theme.FocusTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launched by AppBlockAccessibilityService on top of whatever blocked app
 * just came to the foreground. "Go back" (and the hardware back button) must
 * never return into that app - it would just re-trigger the block - so both
 * send the user to the home screen instead.
 */
@AndroidEntryPoint
class BlockedAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_GOAL_NAME = "extra_goal_name"
        const val EXTRA_REMAINING_SECONDS = "extra_remaining_seconds"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val appName = intent.getStringExtra(EXTRA_APP_NAME)?.takeIf { it.isNotBlank() } ?: packageName
        val goalName = intent.getStringExtra(EXTRA_GOAL_NAME).orEmpty()
        val remainingSeconds = intent.getLongExtra(EXTRA_REMAINING_SECONDS, 0L)

        val icon: Drawable? = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
        val iconBitmap = icon?.let { d -> runCatching { d.toBitmap().asImageBitmap() }.getOrNull() }

        setContent {
            FocusTheme(darkTheme = true) {
                BackHandler { goHome() }
                BlockedAppScreen(
                    appName = appName,
                    appIcon = iconBitmap,
                    goalName = goalName,
                    remainingSeconds = remainingSeconds,
                    onGoBack = { goHome() }
                )
            }
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }
}
