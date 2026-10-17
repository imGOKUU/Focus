package com.meltide.app.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meltide.app.presentation.components.Depth
import com.meltide.app.presentation.components.FocusPrimaryButton
import com.meltide.app.presentation.components.FocusSurface
import com.meltide.app.presentation.components.IconTile
import com.meltide.app.presentation.components.ResponsiveScreen
import com.meltide.app.presentation.components.ValueChip
import com.meltide.app.presentation.components.cardSurface
import com.meltide.app.presentation.components.depth
import com.meltide.app.presentation.components.entrance
import com.meltide.app.presentation.components.insetSurface
import com.meltide.app.presentation.components.screenVerticalInsets
import com.meltide.app.presentation.theme.FocusMountainFar
import com.meltide.app.presentation.theme.FocusMountainMid
import com.meltide.app.presentation.theme.FocusMountainNear
import com.meltide.app.presentation.theme.FocusSoftAccent
import com.meltide.app.presentation.theme.FocusSunAccent
import com.meltide.app.presentation.theme.GoalAccents
import com.meltide.app.presentation.theme.TileShape
import com.meltide.app.presentation.theme.WelcomeHeadline
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private data class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: @Composable (pageOffset: () -> Float) -> Unit
)

private val OnboardingPages = listOf(
    OnboardingPage(
        title = "Your time.\nYour rules.",
        body = "Give time to what matters. We'll keep the distractions away.",
        illustration = { SunriseScene(it) }
    ),
    OnboardingPage(
        title = "Set a daily\ngoal.",
        body = "Choose what deserves your time, and how much of it, every day.",
        illustration = { GoalStackScene(it) }
    ),
    OnboardingPage(
        title = "Distractions,\non hold.",
        body = "Pick the apps to pause while you focus. We'll gently keep them away.",
        illustration = { FocusShieldScene(it) }
    )
)

// The page gutter (ResponsiveScreen's 24dp horizontal padding). The pager
// bleeds out over it so swiped pages and the hero card's shadow aren't cut
// off at the gutter edge; each page re-applies it on the inside.
private val PageGutter = 24.dp
private val MinIllustrationHeight = 120.dp

// Below this pager height (phone landscape, split screen), each page puts
// its text beside the illustration instead of above it.
private val SideBySidePageHeight = 300.dp

@Composable
fun WelcomeScreen(
    onNavigate: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == OnboardingPages.lastIndex

    // System back steps back through the intro before leaving it.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    ResponsiveScreen(
        contentModifier = Modifier.windowInsetsPadding(screenVerticalInsets())
    ) {
        // Fixed-height row so the pager doesn't jump when Skip disappears.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = !isLastPage, enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = {
                    println("Clicked: Welcome Skip")
                    scope.launch { pagerState.animateScrollToPage(OnboardingPages.lastIndex) }
                }) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .weight(1f)
                .bleedHorizontally(PageGutter)
        ) { page ->
            // 0 when this page is settled, +/-1 as it slides fully off.
            val pageOffset = {
                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            }
            OnboardingPageContent(page = OnboardingPages[page], pageOffset = pageOffset)
        }

        Spacer(modifier = Modifier.height(20.dp))

        PageIndicator(
            pagerState = pagerState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FocusPrimaryButton(
            text = if (isLastPage) "Get Started" else "Next",
            leadingIcon = if (isLastPage) null else Icons.AutoMirrored.Outlined.ArrowForward,
            onClick = {
                if (isLastPage) {
                    println("Clicked: Welcome Get Started button")
                    onNavigate()
                } else {
                    println("Clicked: Welcome Next button")
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .entrance(index = 3)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Widens the node by [amount] on each side, past its parent's padding. */
private fun Modifier.bleedHorizontally(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = (amount * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = constraints.maxWidth + extra
        )
    )
    layout(constraints.maxWidth, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, pageOffset: () -> Float) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PageGutter)
    ) {
        // Text drifts a little slower than the page and fades as it leaves -
        // a light parallax so the swipe feels layered rather than flat.
        val textParallax = Modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.ModulateAlpha
            val offset = pageOffset()
            translationX = offset * size.width * 0.35f
            alpha = 1f - (abs(offset) * 1.4f).coerceAtMost(1f)
        }
        val text: @Composable () -> Unit = {
            Column(modifier = textParallax) {
                Text(
                    text = page.title,
                    style = WelcomeHeadline,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.entrance(index = 0)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.entrance(index = 1)
                )
            }
        }
        // Bottom padding leaves room for the hero card's shadow, which the
        // pager would otherwise clip.
        val hero: @Composable (Modifier) -> Unit = { modifier ->
            BoxWithConstraints(
                modifier = modifier.padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (maxHeight >= MinIllustrationHeight) {
                    FocusSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .fillMaxHeight()
                            .entrance(index = 2),
                        depth = Depth.Medium
                    ) {
                        page.illustration(pageOffset)
                    }
                }
            }
        }

        if (maxHeight < SideBySidePageHeight) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { text() }
                hero(Modifier.weight(1f).fillMaxHeight().padding(top = 12.dp))
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(12.dp))
                text()
                Spacer(modifier = Modifier.height(28.dp))
                hero(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 1: sunrise over the mountains. The sun rises on first appearance
// and keeps breathing, clouds drift across, and the mountain layers shift
// at different speeds while the page is swiped.
// ═══════════════════════════════════════════════════════════════════════

private val SkyTop = Color(0xFFFFF7EE)
private val SkyBottom = Color(0xFFFBE4D3)

@Composable
private fun SunriseScene(pageOffset: () -> Float) {
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        rise.animateTo(1f, tween(durationMillis = 1600, delayMillis = 250, easing = FastOutSlowInEasing))
    }
    val transition = rememberInfiniteTransition(label = "sunrise")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing)),
        label = "drift"
    )

    // Its own layer, so a fading parent (Modifier.entrance) fades the scene
    // as one picture instead of letting the sun show through the ridges.
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val w = size.width
        val h = size.height
        val offset = pageOffset()

        drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))

        // Sun with a soft pulsing halo.
        val sunCenter = Offset(w * 0.56f, h * (0.95f - 0.62f * rise.value))
        val sunRadius = minOf(w, h) * 0.16f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(FocusSunAccent.copy(alpha = 0.55f), Color.Transparent),
                center = sunCenter,
                radius = sunRadius * (2.1f + 0.25f * breath)
            ),
            radius = sunRadius * (2.1f + 0.25f * breath),
            center = sunCenter
        )
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(lerp(FocusSunAccent, Color.White, 0.35f), FocusSunAccent),
                startY = sunCenter.y - sunRadius,
                endY = sunCenter.y + sunRadius
            ),
            radius = sunRadius,
            center = sunCenter
        )

        drawCloud(x = wrap(drift, w, cloudWidth = w * 0.30f), y = h * 0.20f, width = w * 0.30f)
        drawCloud(x = wrap(drift * 0.6f + 0.55f, w, cloudWidth = w * 0.22f), y = h * 0.34f, width = w * 0.22f)

        // Back to front: each layer is wider than the card and shifts a little
        // further during a swipe, so nearer ridges appear to move faster.
        drawRidge(
            parallax = offset * w * 0.05f,
            color = FocusMountainFar,
            points = listOf(-0.3f to 0.58f, 0.05f to 0.40f, 0.30f to 0.24f, 0.62f to 0.52f, 0.85f to 0.30f, 1.3f to 0.50f)
        )
        drawRidge(
            parallax = offset * w * 0.12f,
            color = FocusMountainMid,
            points = listOf(-0.3f to 0.80f, 0.0f to 0.62f, 0.20f to 0.46f, 0.42f to 0.72f, 0.60f to 0.42f, 0.80f to 0.70f, 1.0f to 0.54f, 1.3f to 0.66f)
        )
        drawRidge(
            parallax = offset * w * 0.22f,
            color = FocusMountainNear,
            points = listOf(-0.3f to 0.86f, 0.0f to 0.88f, 0.16f to 0.66f, 0.34f to 0.90f, 0.52f to 0.62f, 0.70f to 0.88f, 0.88f to 0.64f, 1.0f to 0.80f, 1.3f to 0.90f)
        )
    }
}

/** Horizontal position for something looping left-to-right across [width]. */
private fun wrap(progress: Float, width: Float, cloudWidth: Float): Float =
    (progress % 1f) * (width + cloudWidth * 2) - cloudWidth * 1.5f

private fun DrawScope.drawCloud(x: Float, y: Float, width: Float) {
    val color = Color.White.copy(alpha = 0.85f)
    val r = width * 0.2f
    drawCircle(color, radius = r, center = Offset(x + width * 0.30f, y))
    drawCircle(color, radius = r * 1.3f, center = Offset(x + width * 0.55f, y - r * 0.35f))
    drawRoundRect(
        color = color,
        topLeft = Offset(x + width * 0.1f, y - r * 0.2f),
        size = Size(width * 0.8f, r * 1.2f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.6f)
    )
}

/** A mountain ridge from (x, y) fractions of the canvas, shaded lighter at the peaks. */
private fun DrawScope.drawRidge(parallax: Float, color: Color, points: List<Pair<Float, Float>>) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(points.first().first * w, h)
        points.forEach { (x, y) -> lineTo(x * w, y * h) }
        lineTo(points.last().first * w, h)
        close()
    }
    val peak = points.minOf { it.second } * h
    translate(left = parallax) {
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(lerp(color, Color.White, 0.22f), color),
                startY = peak,
                endY = h
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 2: a floating stack of goal cards - the same rows as the Home
// screen, in miniature - each gently bobbing, with progress filling in.
// ═══════════════════════════════════════════════════════════════════════

private data class SampleGoal(val name: String, val duration: String, val icon: ImageVector, val progress: Float)

private val SampleGoals = listOf(
    SampleGoal("Mathematics", "1h", Icons.Outlined.Calculate, 1f),
    SampleGoal("DSA", "45m", Icons.Outlined.Code, 0.62f),
    SampleGoal("Reading", "30m", Icons.AutoMirrored.Outlined.MenuBook, 0.28f)
)

@Composable
private fun GoalStackScene(pageOffset: () -> Float) {
    val transition = rememberInfiniteTransition(label = "goalStack")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "bob"
    )
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fill.animateTo(1f, tween(durationMillis = 1400, delayMillis = 400, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, MaterialTheme.colorScheme.surfaceContainerLow))),
        contentAlignment = Alignment.Center
    ) {
        val rowsThatFit = when {
            maxHeight < 150.dp -> 1
            maxHeight < 220.dp -> 2
            else -> 3
        }
        Column(
            modifier = Modifier.fillMaxWidth(0.86f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SampleGoals.take(rowsThatFit).forEachIndexed { index, goal ->
                val accent = GoalAccents.forIndex(index)
                FocusSurface(
                    depth = Depth.Low,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val phase = (bob + index * 0.22f) * 2f * PI.toFloat()
                            translationY = sin(phase) * 4.dp.toPx()
                            translationX = pageOffset() * size.width * (0.12f + index * 0.1f)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconTile(color = accent.chipBackground, glowColor = accent.onChip, size = 34.dp) {
                            Icon(goal.icon, contentDescription = null, tint = accent.onChip, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .insetSurface(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(goal.progress * fill.value)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(Brush.horizontalGradient(listOf(accent.chipBackground, accent.onChip)))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        if (goal.progress >= 1f && fill.value >= 1f) {
                            ValueChip(text = "Done", icon = Icons.Outlined.Check, contentColor = accent.onChip)
                        } else {
                            ValueChip(text = goal.duration)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Page 3: a focus timer ticking inside a raised dial, with distracting
// apps floating around it - each dimmed and locked.
// ═══════════════════════════════════════════════════════════════════════

private data class SampleApp(val letter: String, val color: Color, val x: Float, val y: Float)

private val SampleApps = listOf(
    SampleApp("I", Color(0xFFC1447E), x = -1f, y = -0.55f),
    SampleApp("Y", Color(0xFFE1483B), x = 1f, y = -0.45f),
    SampleApp("R", Color(0xFFE06A2C), x = -0.9f, y = 0.6f),
    SampleApp("X", Color(0xFF35619E), x = 0.95f, y = 0.65f)
)

private const val SampleSessionSeconds = 25 * 60

@Composable
private fun FocusShieldScene(pageOffset: () -> Float) {
    val transition = rememberInfiniteTransition(label = "shield")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "progress"
    )
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing)),
        label = "bob"
    )
    val lockPulse by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lockPulse"
    )
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainer

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SkyTop, MaterialTheme.colorScheme.surfaceContainerLow))),
        contentAlignment = Alignment.Center
    ) {
        val dial = minOf(maxHeight * 0.62f, maxWidth * 0.42f, 150.dp)
        val tile = (dial * 0.32f).coerceIn(28.dp, 44.dp)

        // Apps orbit at the dial's sides, behind it.
        SampleApps.forEachIndexed { index, app ->
            Box(
                modifier = Modifier
                    .offset(x = (dial * 0.5f + tile) * app.x, y = dial * 0.5f * app.y)
                    .graphicsLayer {
                        val phase = (bob + index * 0.25f) * 2f * PI.toFloat()
                        translationY = sin(phase) * 5.dp.toPx()
                        translationX = pageOffset() * size.width * (1.5f + index * 0.4f)
                    }
            ) {
                IconTile(color = lerp(app.color, Color(0xFFB9B4AC), 0.45f), glowColor = app.color, size = tile) {
                    Text(
                        text = app.letter,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 5.dp, y = 5.dp)
                        .size(18.dp)
                        .graphicsLayer {
                            scaleX = lockPulse
                            scaleY = lockPulse
                        }
                        .depth(CircleShape, Depth.Low)
                        .clip(CircleShape)
                        .background(primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(10.dp))
                }
            }
        }

        // The raised dial with the ticking ring.
        Box(
            modifier = Modifier
                .size(dial)
                .depth(CircleShape, Depth.Medium)
                .cardSurface(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(dial * 0.1f)) {
                val stroke = size.minDimension * 0.09f
                drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(stroke))
                drawArc(
                    brush = Brush.sweepGradient(listOf(lerp(primary, Color.White, 0.45f), primary, lerp(primary, Color.White, 0.45f))),
                    startAngle = -90f,
                    sweepAngle = 360f * (1f - progress),
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            val remaining = (SampleSessionSeconds * (1f - progress)).toInt()
            Text(
                text = "%02d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (dial.value * 0.15f).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════

/** Dots that stretch into a pill for the current page, following the swipe. */
@Composable
private fun PageIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { index ->
            val isActive = index == pagerState.currentPage
            val width by animateDpAsState(
                targetValue = if (isActive) 22.dp else 6.dp,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                label = "dotWidth"
            )
            val color by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primary else FocusSoftAccent,
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
