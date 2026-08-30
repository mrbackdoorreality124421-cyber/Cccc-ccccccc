package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceStyle
import com.example.chess.model.PieceType

@Composable
fun PromotionDialog(
    color: PieceColor,
    style: PieceStyle = PieceStyle.CLASSIC,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Promote Pawn",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "Choose promotion piece",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val promotionPieces = listOf(
                        PieceType.QUEEN,
                        PieceType.KNIGHT,
                        PieceType.ROOK,
                        PieceType.BISHOP
                    )

                    for (pieceType in promotionPieces) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFF1D2736), RoundedCornerShape(10.dp))
                                .clickable { onPieceSelected(pieceType) },
                            contentAlignment = Alignment.Center
                        ) {
                            ChessPieceView(
                                piece = ChessPiece(pieceType, color),
                                style = style,
                                size = 44.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
