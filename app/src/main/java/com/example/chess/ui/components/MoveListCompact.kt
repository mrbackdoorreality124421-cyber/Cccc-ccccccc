package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessMove

@Composable
fun MoveListCompact(
    moves: List<ChessMove>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF161E2B), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (moves.isEmpty()) {
            Text(
                text = "Moves will appear here as you play",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pairsCount = (moves.size + 1) / 2
                for (i in 0 until pairsCount) {
                    val moveNum = i + 1
                    val whiteIndex = i * 2
                    val blackIndex = i * 2 + 1

                    val whiteMove = moves.getOrNull(whiteIndex)
                    val blackMove = moves.getOrNull(blackIndex)

                    val isLastPair = i == pairsCount - 1

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(
                                if (isLastPair) Color(0xFF1D2736) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$moveNum.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )

                        if (whiteMove != null) {
                            Text(
                                text = whiteMove.san.ifEmpty { whiteMove.uci },
                                fontSize = 12.sp,
                                fontWeight = if (whiteIndex == moves.size - 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (whiteIndex == moves.size - 1) Color(0xFFFFD700) else Color(0xFFF8FAFC),
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (blackMove != null) {
                            Text(
                                text = blackMove.san.ifEmpty { blackMove.uci },
                                fontSize = 12.sp,
                                fontWeight = if (blackIndex == moves.size - 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (blackIndex == moves.size - 1) Color(0xFFFFD700) else Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
