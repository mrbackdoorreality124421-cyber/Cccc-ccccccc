package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.chess.model.BoardTheme
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceStyle
import com.example.chess.model.Square

@Composable
fun SquareCell(
    square: Square,
    piece: ChessPiece?,
    isLight: Boolean,
    isSelected: Boolean,
    isLegalMove: Boolean,
    isLastMove: Boolean,
    isCheck: Boolean,
    hasEngineArrow: Boolean,
    theme: BoardTheme,
    pieceStyle: PieceStyle,
    isFlipped: Boolean,
    size: Dp,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: (Square, Square) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val squareSizePx = with(LocalDensity.current) { size.toPx() }

    val baseLight = Color(theme.lightColor)
    val baseDark = Color(theme.darkColor)
    val baseColor = if (isLight) baseLight else baseDark

    // Realistic wood grain multi-stop gradient simulation
    val woodGrainBrush = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.94f),
            baseColor,
            baseColor.copy(alpha = 0.90f),
            baseColor
        ),
        start = Offset(0f, 0f),
        end = Offset(squareSizePx * 0.35f, squareSizePx * 0.75f)
    )

    Box(
        modifier = modifier
            .size(size)
            .background(woodGrainBrush)
            .then(
                // Selection glow - luxury gold border
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFFFFD700).copy(alpha = 0.95f),
                        shape = RectangleShape
                    )
                } else Modifier
            )
            .then(
                // Last move highlight (soft sapphire tint)
                if (isLastMove) {
                    Modifier.background(Color(0xFF3B82F6).copy(alpha = 0.28f))
                } else Modifier
            )
            .then(
                // Check highlight (deep red warning glow)
                if (isCheck) {
                    Modifier.background(Color(0xFFEF4444).copy(alpha = 0.45f))
                } else Modifier
            )
            .then(
                // Engine recommendation hint glow
                if (hasEngineArrow) {
                    Modifier.background(Color(0xFF10B981).copy(alpha = 0.22f))
                } else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Legal move indicator (subtle dot for empty squares, capture ring for enemy squares)
        if (isLegalMove && piece == null) {
            Box(
                modifier = Modifier
                    .size(size * 0.30f)
                    .background(
                        color = if (isLight) Color(0xFF1E293B).copy(alpha = 0.25f) else Color(0xFFFFFFFF).copy(alpha = 0.35f),
                        shape = CircleShape
                    )
            )
        } else if (isLegalMove && piece != null) {
            // Capture target ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .border(
                        width = 3.dp,
                        color = Color(0xFFEF4444).copy(alpha = 0.75f),
                        shape = CircleShape
                    )
            )
        }

        // The piece with full physics animations & gestures
        if (piece != null) {
            AnimatedPiece(
                piece = piece,
                square = square,
                style = pieceStyle,
                size = size,
                isSelected = isSelected,
                isFlipped = isFlipped,
                onDragStart = onDragStart,
                onDragEnd = onDragEnd
            )
        }
    }
}
