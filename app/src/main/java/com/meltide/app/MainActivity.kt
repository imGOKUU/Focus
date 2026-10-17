package com.meltide.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.meltide.app.presentation.theme.FocusTheme
import com.meltide.app.core.notifications.SessionNotifier
import com.meltide.app.presentation.screens.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionNotifier: SessionNotifier

    // Same instance NavScreen gets from hiltViewModel() (both are activity-scoped).
    private val appViewModel: AppViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        // Notification permission is granted outside the app; re-sync on return.
        sessionNotifier.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Dark status/navigation icons over the cream, edge to edge (after
        // super, once the splash has swapped in the app theme).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // Hold the splash until we know whether to open the intro or Today,
        // so the first thing after it is the right screen - never a blank frame.
        splash.setKeepOnScreenCondition { appViewModel.startDestination.value == null }

        // Icon swells and fades while the splash dissolves into the app.
        splash.setOnExitAnimationListener { provider ->
            val icon: View = provider.iconView
            val fade = ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f)
            val growX = ObjectAnimator.ofFloat(icon, View.SCALE_X, 1f, 1.25f)
            val growY = ObjectAnimator.ofFloat(icon, View.SCALE_Y, 1f, 1.25f)
            AnimatorSet().apply {
                playTogether(fade, growX, growY)
                duration = 380L
                interpolator = AnticipateInterpolator(0.6f)
                doOnEnd { provider.remove() }
                start()
            }
        }

        setContent {
            FocusTheme {
                    NavScreen()
            }
        }
    }
}
