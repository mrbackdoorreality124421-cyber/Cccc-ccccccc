package com.example.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun LastMoveHighlight(
    isFrom: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "last_move_anim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "last_move_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isFrom) Color(0xFF3B82F6).copy(alpha = alpha)
                else Color(0xFF10B981).copy(alpha = alpha)
            )
    )
}
