package com.demo.myapplication.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.demo.myapplication.presentation.theme.ButtonLabel
import com.demo.myapplication.presentation.theme.ButtonShape
import com.demo.myapplication.presentation.theme.CardShape
import com.demo.myapplication.presentation.theme.ChipShape
import com.demo.myapplication.presentation.theme.FocusShadow
import com.demo.myapplication.presentation.theme.TileShape

/**
 * The base raised surface every card, banner and small control in the app is
 * built from. With [onClick] it becomes pressable: it sinks (scale + flatter
 * shadow) under the finger and shows a ripple, so every tappable card gives
 * the same physical feedback.
 */
@Composable
fun FocusSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    depth: Depth = Depth.Medium,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val press by rememberPressFraction(interactionSource)
    Box(
        modifier = modifier
            .pressScale({ press }, amount = 0.02f)
            .depth(shape, depth, lift = 1f - 0.6f * press)
            .cardSurface(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClickLabel = onClickLabel,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = contentAlignment,
        content = content
    )
}

/**
 * The one primary call-to-action style (Get Started / Continue / Save /
 * Pause). Lit from above with a soft glow in its own color underneath, and a
 * text crossfade so label changes (Next -> Get Started, Pause -> Resume)
 * animate instead of jumping.
 */
@Composable
fun FocusPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    height: Dp = 52.dp,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val press by rememberPressFraction(interactionSource)
    val shape = ButtonShape
    val topColor = lerp(containerColor, Color.White, 0.12f)

    Box(
        modifier = modifier
            .height(height)
            .pressScale({ press })
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 18.dp - 10.dp * press,
                    color = containerColor,
                    spread = (-4).dp,
                    offset = DpOffset(0.dp, 8.dp - 5.dp * press),
                    alpha = 0.38f
                )
            )
            .depth(shape, Depth.Low, lift = 1f - 0.6f * press)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, containerColor)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent)
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = contentColor),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = text to leadingIcon,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 }) togetherWith
                    (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
            },
            label = "buttonLabel"
        ) { (label, icon) ->
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = label,
                    style = ButtonLabel,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Back navigation as a small raised tile instead of a bare 24dp icon - a
 * full 44dp touch target with a label for screen readers.
 */
@Composable
fun FocusBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusSurface(
        modifier = modifier.size(44.dp),
        shape = TileShape,
        depth = Depth.Low,
        onClick = onClick,
        onClickLabel = "Go back",
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * The leading square on list rows (goal icons, app icons, Add Goal). Shaded
 * from a lighter top to [color] with a small glow in [glowColor], so it reads
 * as a raised button-like tile rather than a flat colored square.
 */
@Composable
fun IconTile(
    color: Color,
    modifier: Modifier = Modifier,
    glowColor: Color = FocusShadow,
    size: Dp = FocusLayout.RowIconSize,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .dropShadow(
                shape = TileShape,
                shadow = Shadow(
                    radius = 6.dp,
                    color = glowColor,
                    spread = (-1).dp,
                    offset = DpOffset(0.dp, 3.dp),
                    alpha = 0.22f
                )
            )
            .clip(TileShape)
            .background(Brush.verticalGradient(listOf(lerp(color, Color.White, 0.35f), color)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.7f), Color.Transparent)
                ),
                shape = TileShape
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * A small pressed-in value pill (durations, counts, session meta). Inset
 * rather than raised, so it reads as information, not as a button.
 */
@Composable
fun ValueChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier
            .insetSurface(ChipShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(14.dp)
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}
