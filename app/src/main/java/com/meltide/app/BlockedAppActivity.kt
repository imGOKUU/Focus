package com.meltide.app

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.meltide.app.presentation.screens.BlockedAppScreen
import com.meltide.app.presentation.theme.FocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

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
        // Wall-clock end time rather than a remaining-seconds snapshot, so the
        // countdown stays correct while the screen is open and across rotation.
        const val EXTRA_SESSION_ENDS_AT_MILLIS = "extra_session_ends_at_millis"
    }

    private data class BlockInfo(
        val appName: String,
        val appIcon: ImageBitmap?,
        val goalName: String,
        val sessionEndsAtMillis: Long
    )

    // singleTask: if this screen is still alive (user pressed Home on it) and
    // another blocked app opens, the new intent arrives via onNewIntent, so
    // the content has to be swappable rather than read once in onCreate.
    private var blockInfo by mutableStateOf<BlockInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Same light system bars as the main app (dark icons on cream).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        blockInfo = intent.toBlockInfo()

        setContent {
            FocusTheme(darkTheme = true) {
                BackHandler { goHome() }
                val info = blockInfo ?: return@FocusTheme
                var remainingSeconds by remember(info) { mutableLongStateOf(info.remainingSeconds()) }
                LaunchedEffect(info) {
                    while (true) {
                        remainingSeconds = info.remainingSeconds()
                        if (remainingSeconds == 0L) break
                        delay(1_000)
                    }
                }
                BlockedAppScreen(
                    appName = info.appName,
                    appIcon = info.appIcon,
                    goalName = info.goalName,
                    remainingSeconds = remainingSeconds,
                    onGoBack = { goHome() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockInfo = intent.toBlockInfo()
    }

    private fun Intent.toBlockInfo(): BlockInfo {
        val packageName = getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val icon: Drawable? = runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
        return BlockInfo(
            appName = getStringExtra(EXTRA_APP_NAME)?.takeIf { it.isNotBlank() } ?: packageName,
            appIcon = icon?.let { d -> runCatching { d.toBitmap().asImageBitmap() }.getOrNull() },
            goalName = getStringExtra(EXTRA_GOAL_NAME).orEmpty(),
            sessionEndsAtMillis = getLongExtra(EXTRA_SESSION_ENDS_AT_MILLIS, 0L)
        )
    }

    private fun BlockInfo.remainingSeconds(): Long =
        ((sessionEndsAtMillis - System.currentTimeMillis()) / 1000).coerceAtLeast(0L)

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }
}
