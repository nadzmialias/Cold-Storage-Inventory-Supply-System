package com.csiss.inventory.ui.temperature

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RefreshNavy = Color(0xFF132945)
private val RefreshIceBlue = Color(0xFF67D5F5)

@Composable
fun OutlineRefreshButton(
    onClick: () -> Unit,
    isRefreshing: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .height(42.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isRefreshing,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RefreshNavy,
                contentColor = Color.White,
                disabledContainerColor = RefreshNavy,
                disabledContentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            Text(
                text = "Refresh",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (isRefreshing) {
            AnimatedRefreshOutline(
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@Composable
private fun AnimatedRefreshOutline(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "RefreshOutline"
    )

    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1400,
                easing = LinearEasing
            )
        ),
        label = "OutlineProgress"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2f
        val radius = 12.dp.toPx() - inset

        val borderPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        left = inset,
                        top = inset,
                        right = size.width - inset,
                        bottom = size.height - inset
                    ),
                    cornerRadius = CornerRadius(radius)
                )
            )
        }

        val pathMeasure = PathMeasure().apply {
            setPath(borderPath, forceClosed = true)
        }

        val totalLength = pathMeasure.length
        val segmentLength = totalLength * 0.22f

        val start = totalLength * progress
        val end = start + segmentLength

        val animatedPath = Path()

        pathMeasure.getSegment(
            startDistance = start,
            stopDistance = minOf(end, totalLength),
            destination = animatedPath,
            startWithMoveTo = true
        )

        if (end > totalLength) {
            pathMeasure.getSegment(
                startDistance = 0f,
                stopDistance = end - totalLength,
                destination = animatedPath,
                startWithMoveTo = true
            )
        }

        drawPath(
            path = animatedPath,
            color = RefreshIceBlue,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            )
        )
    }
}