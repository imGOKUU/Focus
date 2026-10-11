package com.demo.myapplication.presentation.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demo.myapplication.presentation.components.ResponsiveScreen
import com.demo.myapplication.presentation.theme.ButtonLabel
import com.demo.myapplication.presentation.theme.ButtonPillShape
import com.demo.myapplication.presentation.theme.FocusIceAccent
import com.demo.myapplication.presentation.theme.GoalAccent
import com.demo.myapplication.presentation.theme.GoalAccents
import com.demo.myapplication.presentation.theme.TimerDisplayLarge
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
        // Back control: a real IconButton gives the standard 48dp touch
        // target + ripple for free, and windowInsetsPadding keeps it clear
        // of the status bar / display cutouts on every device instead of a
        // fixed 24dp spacer that only happened to work on some phones.
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = SpacingXS)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(SpacingM))

        Text(
            text = goal?.name.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingM)
        )

        Spacer(modifier = Modifier.height(SpacingS))

        Text(
            text = remainingSeconds.toHeroTimerDisplay(),
            style = TimerDisplayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(SpacingL))

        // Fraction + aspectRatio keeps the can's silhouette correct at any
        // width, and sizeIn clamps it so it neither shrinks to a speck on a
        // small phone nor balloons disproportionately in the wider tablet
        // column (ResponsiveScreen caps that column at 600dp, not the full
        // screen width).
        MeltingCanIllustration(
            progressRemaining = progressRemaining,
            canColor = accent.onChip,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.52f)
                .aspectRatio(CanAspectRatio)
                .sizeIn(
                    minWidth = 120.dp,
                    maxWidth = 200.dp,
                    minHeight = 160.dp,
                    maxHeight = 260.dp
                )
        )

        Spacer(modifier = Modifier.height(SpacingL))

        // Session meta: one quiet, scannable line instead of a divider rule
        // plus two stacked lines - stays subtle and doesn't compete with the
        // timer above it.
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = SpacingM),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${safeTotal.toTotalLabel()} total",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "   ·   ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = blockedAppsCount.toBlockedAppsLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = true))

        Button(
            onClick = onPause,
            shape = ButtonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = SpacingS)
                .height(56.dp)
        ) {
            Text(
                text = "Pause",
                style = ButtonLabel
            )
        }

        Spacer(modifier = Modifier.height(SpacingM))
    }
}

// A small, consistent spacing scale used throughout FocusScreen instead of
// one-off dp values, so the rhythm between title/timer/illustration/meta/
// button stays intentional rather than arbitrary.
private val SpacingXS = 4.dp
private val SpacingS = 12.dp
private val SpacingM = 16.dp
private val SpacingL = 28.dp

// Matches the can+ice illustration's original 160x220 design proportions;
// kept as a named constant so the responsive sizing modifier stays readable.
private const val CanAspectRatio = 160f / 220f

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
    // FastOutSlowIn (standard Material easing) instead of linear: the melt
    // eases in and out of each retarget instead of moving at a constant,
    // slightly mechanical rate, while the 900ms duration - short relative to
    // the 1s tick that drives progressRemaining - keeps the animated value
    // synchronized with actual session progress rather than visibly lagging.
    val animatedProgress by animateFloatAsState(
        targetValue = progressRemaining,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "iceMeltProgress"
    )

    // Seeded once per screen instance so the drip shape and droplet positions
    // stay put while the ice height animates - re-rolling them per frame would
    // make the melt edge and droplets jitter instead of reading as melting.
    val drips = remember {
        val rng = Random(7)
        List(11) { i ->
            IceDrip(
                xFraction = (i + 0.5f) / 11f,
                depthFraction = if (rng.nextFloat() < 0.35f) {
                    0.7f + rng.nextFloat() * 0.3f
                } else {
                    0.1f + rng.nextFloat() * 0.3f
                }
            )
        }
    }
    val droplets = remember {
        val rng = Random(42)
        List(16) {
            WaterDroplet(
                xFraction = 0.1f + rng.nextFloat() * 0.8f,
                yFraction = 0.2f + rng.nextFloat() * 0.7f,
                radiusFraction = 0.015f + rng.nextFloat() * 0.015f
            )
        }
    }

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
        val canCornerRadius = canWidth * 0.18f
        val lidHeight = canWidth * 0.16f
        val edgeShade = lerp(canColor, Color.Black, 0.38f)
        val specular = lerp(canColor, Color.White, 0.5f)
        val ringColor = lerp(canColor, Color.Black, 0.22f)

        // Asymmetric stops (highlight nearer the left third, not dead
        // center) read as a light source from the upper-left - the same
        // direction the ice streaks and droplet highlights below use - which
        // is what sells the cylindrical, 3D feel rather than a flat tint.
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0.0f to edgeShade,
                0.22f to canColor,
                0.38f to specular,
                0.58f to canColor,
                1.0f to edgeShade,
                startX = canLeft,
                endX = canRight
            ),
            topLeft = Offset(canLeft, canTop),
            size = Size(canWidth, canHeight),
            cornerRadius = CornerRadius(canCornerRadius, canCornerRadius)
        )

        // A soft vertical sheen aligned with the gradient's highlight band,
        // echoing the ice streaks lower down so the whole illustration reads
        // as lit from one consistent direction.
        val sheenX = canLeft + canWidth * 0.3f
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(sheenX, canTop + lidHeight * 0.6f),
            end = Offset(sheenX, canBottom - canHeight * 0.06f),
            strokeWidth = canWidth * 0.1f
        )

        val neckY = canTop + canHeight * 0.12f
        val baseRingY = canBottom - canHeight * 0.06f
        drawLine(ringColor, Offset(canLeft, neckY), Offset(canRight, neckY), strokeWidth = 1.5.dp.toPx())

        // Bottom rim: a thin shaded ellipse mirroring the top lid treatment,
        // plus the ring line - so the can reads as a closed cylinder with a
        // visible base rather than a flat rounded rect with a stray line.
        drawOval(
            color = lerp(canColor, Color.Black, 0.22f),
            topLeft = Offset(canLeft + canWidth * 0.1f, canBottom - lidHeight * 0.3f),
            size = Size(canWidth * 0.8f, lidHeight * 0.45f),
            alpha = 0.6f
        )
        drawLine(ringColor, Offset(canLeft, baseRingY), Offset(canRight, baseRingY), strokeWidth = 1.5.dp.toPx())

        drawOval(
            color = lerp(canColor, Color.White, 0.25f),
            topLeft = Offset(canLeft, canTop - lidHeight / 2f),
            size = Size(canWidth, lidHeight)
        )
        drawOval(
            color = lerp(canColor, Color.Black, 0.3f),
            topLeft = Offset(canLeft + canWidth * 0.18f, canTop - lidHeight * 0.3f),
            size = Size(canWidth * 0.64f, lidHeight * 0.6f)
        )
        // Thin rim stroke on the lid edge - the detail that keeps the top
        // from looking like a flat painted ellipse.
        drawOval(
            color = ringColor.copy(alpha = 0.6f),
            topLeft = Offset(canLeft, canTop - lidHeight / 2f),
            size = Size(canWidth, lidHeight),
            style = Stroke(width = 1.dp.toPx())
        )

        val overhang = canWidth * 0.05f
        val iceLeft = canLeft - overhang
        val iceRight = canRight + overhang
        val iceTopY = canTop - canHeight * 0.03f
        val topCornerRadius = canWidth * 0.12f
        val maxIceBottomY = canBottom + canHeight * 0.02f
        val iceBaseBottomY = iceTopY + (maxIceBottomY - iceTopY) * animatedProgress

        if (iceBaseBottomY > iceTopY + 1f) {
            val dripScale = canHeight * 0.07f * animatedProgress
            val iceWidth = iceRight - iceLeft

            val icePath = Path().apply {
                moveTo(iceLeft, iceTopY + topCornerRadius)
                quadraticBezierTo(iceLeft, iceTopY, iceLeft + topCornerRadius, iceTopY)
                lineTo(iceRight - topCornerRadius, iceTopY)
                quadraticBezierTo(iceRight, iceTopY, iceRight, iceTopY + topCornerRadius)
                lineTo(iceRight, iceBaseBottomY)
                var prevX = iceRight
                var prevY = iceBaseBottomY
                for (k in drips.indices) {
                    val drip = drips[drips.lastIndex - k]
                    val x = iceLeft + iceWidth * drip.xFraction
                    val y = iceBaseBottomY + dripScale * drip.depthFraction
                    quadraticBezierTo(prevX, prevY, (prevX + x) / 2f, (prevY + y) / 2f)
                    prevX = x
                    prevY = y
                }
                quadraticBezierTo(prevX, prevY, iceLeft, iceBaseBottomY)
                lineTo(iceLeft, iceTopY + topCornerRadius)
                close()
            }

            val streakX1 = canLeft + canWidth * 0.2f
            val streakX2 = canLeft + canWidth * 0.78f
            val streakTop = iceTopY + topCornerRadius
            val streakBottom = iceBaseBottomY - canHeight * 0.04f
            val streakWidth = 2.dp.toPx()

            val margin = 4.dp.toPx()
            clipRect(
                left = iceLeft - margin,
                top = iceTopY - margin,
                right = iceRight + margin,
                bottom = maxIceBottomY + margin
            ) {
                drawPath(
                    icePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lerp(FocusIceAccent, Color.White, 0.55f),
                            FocusIceAccent,
                            lerp(FocusIceAccent, canColor, 0.12f)
                        ),
                        startY = iceTopY,
                        endY = maxIceBottomY
                    ),
                    alpha = iceAlpha
                )

                // Soft glass-glare blob, clipped to the ice's own silhouette
                // so it reads as light reflecting off a translucent, glossy
                // surface instead of a flat tinted rectangle. Same upper-left
                // lighting direction as the can's sheen and the droplet
                // highlights below.
                clipPath(icePath) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.4f), Color.Transparent),
                            center = Offset(iceLeft + iceWidth * 0.3f, iceTopY + (iceBaseBottomY - iceTopY) * 0.2f),
                            radius = iceWidth * 0.5f
                        ),
                        radius = iceWidth * 0.5f,
                        center = Offset(iceLeft + iceWidth * 0.3f, iceTopY + (iceBaseBottomY - iceTopY) * 0.2f),
                        alpha = iceAlpha
                    )
                }

                drawLine(Color.White.copy(alpha = 0.5f), Offset(streakX1, streakTop), Offset(streakX1, streakBottom), strokeWidth = streakWidth)
                drawLine(Color.White.copy(alpha = 0.3f), Offset(streakX2, streakTop), Offset(streakX2, streakBottom), strokeWidth = streakWidth)

                // Crisp translucent rim on the melt edge/silhouette - this is
                // what keeps the ice from reading as a flat filled shape,
                // especially along the wavy drip edge.
                drawPath(
                    icePath,
                    color = Color.White.copy(alpha = 0.45f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        val exposure = (1f - animatedProgress).coerceIn(0f, 1f)
        val dropletAlpha = 0.2f + 0.6f * exposure
        for (droplet in droplets) {
            val cx = canLeft + canWidth * droplet.xFraction
            val cy = canTop + canHeight * droplet.yFraction
            val r = canWidth * droplet.radiusFraction
            if (cy - r < iceBaseBottomY) continue
            // Slightly elongated vertically (a teardrop silhouette rather
            // than a perfect circle) so droplets read as water clinging to
            // the can instead of flat decorative dots.
            drawOval(
                color = Color.White.copy(alpha = dropletAlpha * 0.4f),
                topLeft = Offset(cx - r, cy - r * 1.2f),
                size = Size(r * 2f, r * 2.4f)
            )
            drawCircle(
                Color.White.copy(alpha = dropletAlpha),
                radius = r * 0.3f,
                center = Offset(cx - r * 0.35f, cy - r * 0.45f)
            )
        }
    }
}

private class IceDrip(val xFraction: Float, val depthFraction: Float)

private class WaterDroplet(val xFraction: Float, val yFraction: Float, val radiusFraction: Float)
