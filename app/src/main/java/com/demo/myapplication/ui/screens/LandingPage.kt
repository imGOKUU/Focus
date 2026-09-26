package com.demo.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.demo.myapplication.ui.theme.ButtonPillShape
import com.demo.myapplication.ui.theme.FocusMountainFar
import com.demo.myapplication.ui.theme.FocusMountainMid
import com.demo.myapplication.ui.theme.FocusMountainNear
import com.demo.myapplication.ui.theme.FocusSoftAccent
import com.demo.myapplication.ui.theme.FocusSunAccent

@Composable
fun LandingPage(
    onNavigate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Your time.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Your rules.",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Give time to what matters. We'll keep the distractions away.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        MountainSunIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        PageIndicator(
            pageCount = 3,
            activeIndex = 0,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = ButtonPillShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun MountainSunIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Sun — sits low and slightly left, tucked behind the back mountain layer.
        drawCircle(
            color = FocusSunAccent,
            radius = w * 0.11f,
            center = Offset(w * 0.28f, h * 0.62f)
        )

        // Back layer — furthest, lightest.
        val far = Path().apply {
            moveTo(0f, h * 0.55f)
            lineTo(w * 0.30f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.85f, h * 0.28f)
            lineTo(w, h * 0.45f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(far, color = FocusMountainFar)

        // Mid layer.
        val mid = Path().apply {
            moveTo(0f, h * 0.78f)
            lineTo(w * 0.20f, h * 0.42f)
            lineTo(w * 0.42f, h * 0.70f)
            lineTo(w * 0.60f, h * 0.38f)
            lineTo(w * 0.80f, h * 0.68f)
            lineTo(w, h * 0.50f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(mid, color = FocusMountainMid)

        // Front layer — closest, darkest, its base sits flush with the bottom edge.
        val near = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.88f)
            lineTo(w * 0.16f, h * 0.62f)
            lineTo(w * 0.34f, h * 0.90f)
            lineTo(w * 0.52f, h * 0.58f)
            lineTo(w * 0.70f, h * 0.86f)
            lineTo(w * 0.88f, h * 0.60f)
            lineTo(w, h * 0.82f)
            lineTo(w, h)
            close()
        }
        drawPath(near, color = FocusMountainNear)
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val dotColor = if (index == activeIndex) {
                MaterialTheme.colorScheme.primary
            } else {
                FocusSoftAccent
            }
            Dot(dotColor)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color)
    )
}