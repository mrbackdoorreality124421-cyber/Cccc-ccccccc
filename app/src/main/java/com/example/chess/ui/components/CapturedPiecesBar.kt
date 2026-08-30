package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*

@Composable
fun CapturedPiecesBar(
    capturedPieces: List<PieceType>,
    color: PieceColor,
    materialAdvantage: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(
                color = Color(0xFF161E2B).copy(alpha = 0.90f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player badge
        val isWhite = color == PieceColor.WHITE
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (isWhite) Color(0xFFF8FAFC) else Color(0xFF334155),
                    shape = CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Grouped captured pieces
        if (capturedPieces.isEmpty()) {
            Text(
                text = if (isWhite) "White's captures" else "Black's captures",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        } else {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val grouped = capturedPieces.groupBy { it }
                // Sort by piece value descending
                val sortedKeys = grouped.keys.sortedByDescending { it.baseValue }

                for (type in sortedKeys) {
                    val count = grouped[type]?.size ?: 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ChessPieceView(
                            piece = ChessPiece(type, color.opponent),
                            style = PieceStyle.CLASSIC,
                            size = 20.dp
                        )
                        if (count > 1) {
                            Text(
                                text = "x$count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // Material Advantage Label (e.g. +3)
        if (materialAdvantage > 0) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color(0xFF10B981).copy(alpha = 0.20f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "+$materialAdvantage",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399)
                )
            }
        }
    }
}
