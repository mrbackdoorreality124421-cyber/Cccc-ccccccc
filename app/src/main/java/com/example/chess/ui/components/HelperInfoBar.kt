package com.example.chess.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessMove

@Composable
fun HelperInfoBar(
    arrowMove: ChessMove?,
    evaluation: Int,
    mateIn: Int?,
    isThinking: Boolean,
    onRequestHint: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "helper_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_progress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131B2E)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isThinking -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF76FF03),
                        strokeWidth = 2.5.dp
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Analyzing position...",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Stockfish Helper finding optimal continuation",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
                arrowMove != null -> {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color(0xFF76FF03).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = Color(0xFF76FF03),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Suggested: ${arrowMove.from.algebraic.uppercase()} → ${arrowMove.to.algebraic.uppercase()}",
                                color = Color(0xFF76FF03),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (arrowMove.isCapture) {
                                Text(
                                    text = "⚔️ Capture",
                                    color = Color(0xFFFF9800),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (mateIn != null) {
                                Text(
                                    text = "Forced Mate in $mateIn",
                                    color = Color(0xFFFF6D00),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                val evalScore = evaluation / 100.0
                                val evalFormatted = if (evalScore >= 0) "+%.2f".format(evalScore) else "%.2f".format(evalScore)
                                Text(
                                    text = "Eval: $evalFormatted",
                                    color = if (evaluation >= 0) Color(0xFF38BDF8) else Color(0xFFF87171),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "• Tap piece or target square",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = "Waiting for your turn...",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = onRequestHint,
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = Color(0xFF76FF03).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Hint",
                    tint = Color(0xFF76FF03),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
