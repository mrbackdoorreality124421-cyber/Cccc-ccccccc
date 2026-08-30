package com.example.chess.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun EvaluationBar(
    evaluation: Int,
    mateIn: Int?,
    modifier: Modifier = Modifier
) {
    val normalizedEval = (evaluation / 100.0).coerceIn(-10.0, 10.0)
    val targetWhiteFraction = ((normalizedEval + 10.0) / 20.0).toFloat().coerceIn(0.05f, 0.95f)

    val animatedFraction by animateFloatAsState(
        targetValue = targetWhiteFraction,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "eval_bar_anim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E293B))
    ) {
        // Black background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E293B))
        )

        // White advantage fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF34D399))
                    )
                )
        )

        // Center equilibrium indicator
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(2.dp)
                .background(Color.White.copy(alpha = 0.6f))
                .align(Alignment.Center)
        )
    }
}
