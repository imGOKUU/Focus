package com.demo.myapplication.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.demo.myapplication.data.local.entity.Goal
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.theme.ButtonLabel
import com.demo.myapplication.presentation.theme.ButtonPillShape
import com.demo.myapplication.presentation.theme.FocusIceAccent
import com.demo.myapplication.presentation.theme.FocusSoftAccent
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalAccents
import com.demo.myapplication.presentation.theme.TimerDisplayLarge
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random


@Composable
fun FocusScreen(
    goal: GoalListItem?,
    remainingSeconds: Long = 0L,
    totalSeconds: Long = 1L,
    blockedAppsCount: Int = 0,
    accent: GoalAccent = GoalAccents.Teal,
    onPause: () -> Unit = {},
    onBack:()->Unit,
) {
    val safeTotal = totalSeconds.coerceAtLeast(1L)
    val progressRemaining = (remainingSeconds.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)

    ResponsiveScreen {
        Spacer(modifier = Modifier.height(24.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable(onClick = onBack)
        )

        Spacer(modifier = Modifier.height(20.dp))


        Text(
            text = goal?.name?:"",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = remainingSeconds.toHeroTimerDisplay(),
            style = TimerDisplayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        MeltingCanIllustration(
            progressRemaining = progressRemaining,
            canColor = accent.onChip,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 160.dp, height = 220.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(1.dp)
                .background(FocusSoftAccent)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${safeTotal.toTotalLabel()} total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = blockedAppsCount.toBlockedAppsLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onPause,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = ButtonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Pause",
                style = ButtonLabel
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// "00:42:17" - always zero-padded HH:MM:SS, per the spec's own hero-timer
// example, so width never shifts as digits change (see also the "tnum"
// tabular-figure setting on TimerDisplayLarge).
private fun Long.toHeroTimerDisplay(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

// "1:00:00 total" - hour NOT zero-padded, per the spec's own total-label
// example. Deliberately a different format from the hero timer above.
private fun Long.toTotalLabel(): String {
    val h = this / 3600
    val m = (this % 3600) / 60
    val s = this % 60
    return "%d:%02d:%02d".format(h, m, s)
}

private fun Int.toBlockedAppsLabel(): String =
    if (this == 1) "1 app blocked" else "$this apps blocked"

/**
 * The beer-can/ice metaphor (spec sections 13-15): time melting the ice
 * around a can, smooth continuous melt from fully ice-covered to fully
 * free. Hand-drawn via Canvas/Path, same technique as WelcomeScreen's
 * MountainSunIllustration - no image/Lottie assets anywhere in this app.
 */
@Composable
private fun MeltingCanIllustration(
    progressRemaining: Float,
    canColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressRemaining,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "iceMeltProgress"
    )

    // Fixed once per screen instance - only the ice band's height should
    // animate. Re-deriving this per-frame would make the jagged edge crawl
    // every time the timer ticks, reading as "boiling" instead of "melting".
    val jagPhaseSeed = remember { Random.nextFloat() * 2f * PI.toFloat() }

    // Called unconditionally so the animation never restarts/flashes as
    // progress crosses the 10% line; only its *value* is gated below.
    val infiniteTransition = rememberInfiniteTransition(label = "iceLowPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iceLowPulseAlpha"
    )
    val iceAlpha = if (animatedProgress <= 0.1f) pulseAlpha else 0.85f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val canWidth = w * 0.46f
        val canLeft = (w - canWidth) / 2f
        val canRight = canLeft + canWidth
        val canTop = h * 0.10f
        val canBottom = h * 0.90f
        val canHeight = canBottom - canTop
        val canCornerRadius = canWidth * 0.16f

        // Can body.
        drawRoundRect(
            color = canColor,
            topLeft = Offset(canLeft, canTop),
            size = Size(canWidth, canHeight),
            cornerRadius = CornerRadius(canCornerRadius, canCornerRadius)
        )

        // Rim - slightly darker, not a new hardcoded color.
        drawRoundRect(
            color = lerp(canColor, Color.Black, 0.15f),
            topLeft = Offset(canLeft, canTop),
            size = Size(canWidth, canHeight * 0.10f),
            cornerRadius = CornerRadius(canCornerRadius, canCornerRadius)
        )

        // Seam line.
        drawLine(
            color = lerp(canColor, Color.Black, 0.12f),
            start = Offset(w / 2f, canTop + canHeight * 0.14f),
            end = Offset(w / 2f, canBottom - canHeight * 0.08f),
            strokeWidth = 1.5.dp.toPx()
        )

        // Ice band - anchored at the can's top, height shrinks as progress
        // drops, jagged bottom edge so it reads as melting rather than a
        // hard wipe. Revealed can therefore emerges from the bottom up.
        val overhang = canWidth * 0.05f
        val iceLeft = canLeft - overhang
        val iceRight = canRight + overhang
        val iceTopY = canTop - canHeight * 0.03f
        val topCornerRadius = canWidth * 0.12f
        val maxIceBottomY = canBottom + canHeight * 0.02f
        val iceBaseBottomY = iceTopY + (maxIceBottomY - iceTopY) * animatedProgress

        if (iceBaseBottomY > iceTopY + 1f) {
            val segments = 7
            val segWidth = (iceRight - iceLeft) / segments
            val jagAmplitude = canHeight * 0.02f
            val jagFrequency = (2f * PI / (segWidth * 2.4f)).toFloat()

            val icePath = Path().apply {
                moveTo(iceLeft, iceTopY + topCornerRadius)
                quadraticBezierTo(iceLeft, iceTopY, iceLeft + topCornerRadius, iceTopY)
                lineTo(iceRight - topCornerRadius, iceTopY)
                quadraticBezierTo(iceRight, iceTopY, iceRight, iceTopY + topCornerRadius)
                lineTo(iceRight, iceBaseBottomY)
                for (i in 1..segments) {
                    val x = iceRight - segWidth * i
                    val jagY = iceBaseBottomY + jagAmplitude * sin(x * jagFrequency + jagPhaseSeed)
                    lineTo(x, jagY)
                }
                lineTo(iceLeft, iceTopY + topCornerRadius)
                close()
            }

            val margin = 4.dp.toPx()
            clipRect(
                left = iceLeft - margin,
                top = iceTopY - margin,
                right = iceRight + margin,
                bottom = maxIceBottomY + margin
            ) {
                drawPath(icePath, color = FocusIceAccent.copy(alpha = iceAlpha))
            }
        }
    }
}
