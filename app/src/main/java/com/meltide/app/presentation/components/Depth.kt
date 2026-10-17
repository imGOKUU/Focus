package com.meltide.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.meltide.app.presentation.theme.FocusShadow
import kotlinx.coroutines.delay

/**
 * The app's depth system. Every raised surface uses one of these levels, so
 * the "light source" is the same everywhere: soft, warm-tinted shadows that
 * fall slightly downward, stacked in layers (a tight contact shadow under the
 * edge, a key shadow, and a wide ambient one) - a single Material elevation
 * shadow looks flat and grey on the cream background by comparison.
 */
enum class Depth(internal val layers: List<ShadowLayer>) {
    Flat(emptyList()),
    Low(
        listOf(
            ShadowLayer(radius = 2.dp, y = 1.dp, spread = 0.dp, alpha = 0.08f),
            ShadowLayer(radius = 10.dp, y = 4.dp, spread = (-2).dp, alpha = 0.09f)
        )
    ),
    Medium(
        listOf(
            ShadowLayer(radius = 2.dp, y = 1.dp, spread = 0.dp, alpha = 0.06f),
            ShadowLayer(radius = 12.dp, y = 6.dp, spread = (-2).dp, alpha = 0.09f),
            ShadowLayer(radius = 32.dp, y = 16.dp, spread = (-8).dp, alpha = 0.10f)
        )
    ),
    High(
        listOf(
            ShadowLayer(radius = 3.dp, y = 2.dp, spread = 0.dp, alpha = 0.07f),
            ShadowLayer(radius = 16.dp, y = 8.dp, spread = (-2).dp, alpha = 0.11f),
            ShadowLayer(radius = 44.dp, y = 22.dp, spread = (-10).dp, alpha = 0.14f)
        )
    )
}

internal data class ShadowLayer(val radius: Dp, val y: Dp, val spread: Dp, val alpha: Float)

/**
 * Drop shadows for [depth]. [lift] (0..1) flattens them toward the surface -
 * pressed controls pass a lower value so they visibly sink as they shrink.
 */
fun Modifier.depth(shape: Shape, depth: Depth = Depth.Medium, lift: Float = 1f): Modifier =
    depth.layers.fold(this) { acc, layer ->
        acc.dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = layer.radius * (0.4f + 0.6f * lift),
                color = FocusShadow,
                spread = layer.spread,
                offset = DpOffset(0.dp, layer.y * lift),
                alpha = layer.alpha * (0.6f + 0.4f * lift)
            )
        )
    }

/**
 * Raised card face: a barely-there top-to-bottom gradient plus a hairline rim
 * that's light on top and warm on the bottom, which reads as an edge catching
 * light - this is what makes cards look like objects rather than flat panels.
 * Apply after [depth] (shadows are drawn outside the clip).
 */
@Composable
fun Modifier.cardSurface(shape: Shape): Modifier {
    val top = MaterialTheme.colorScheme.surfaceContainerLowest
    val bottom = MaterialTheme.colorScheme.surfaceContainerLow
    return this
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.9f), FocusShadow.copy(alpha = 0.07f))
            ),
            shape = shape
        )
}

/**
 * The opposite of [cardSurface]: a well pressed into the surface around it
 * (shadow along the top inner edge, highlight along the bottom). Used for
 * selection bands and small value chips inside cards.
 */
@Composable
fun Modifier.insetSurface(shape: Shape): Modifier = this
    .clip(shape)
    .background(MaterialTheme.colorScheme.surfaceContainer)
    .innerShadow(
        shape = shape,
        shadow = Shadow(radius = 6.dp, color = FocusShadow, offset = DpOffset(0.dp, 2.dp), alpha = 0.12f)
    )
    .innerShadow(
        shape = shape,
        shadow = Shadow(radius = 2.dp, color = Color.White, offset = DpOffset(0.dp, (-1).dp), alpha = 0.7f)
    )

/** 0 at rest, springing to 1 while [interactionSource] is pressed. */
@Composable
fun rememberPressFraction(interactionSource: InteractionSource): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "press"
    )
}

/** Shrinks slightly as [pressFraction] goes to 1 - the tactile half of a press. */
fun Modifier.pressScale(pressFraction: () -> Float, amount: Float = 0.03f): Modifier =
    graphicsLayer {
        val scale = 1f - amount * pressFraction()
        scaleX = scale
        scaleY = scale
    }

/**
 * Fades and rises into place the first time it's composed, delayed by
 * [index] steps so a screen's sections arrive one after another instead of
 * popping in all at once. Driven from the draw phase, so it never recomposes.
 */
@Composable
fun Modifier.entrance(index: Int, stepMillis: Long = 70L): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis)
        progress.animateTo(1f, tween(durationMillis = 520, easing = FastOutSlowInEasing))
    }
    return graphicsLayer {
        // ModulateAlpha fades without an offscreen buffer, which would clip
        // the faded content's drop shadows to its bounds mid-animation.
        compositingStrategy = CompositingStrategy.ModulateAlpha
        alpha = progress.value
        translationY = (1f - progress.value) * 20.dp.toPx()
    }
}
