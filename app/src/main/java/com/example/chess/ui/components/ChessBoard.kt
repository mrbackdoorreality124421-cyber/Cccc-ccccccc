package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.chess.model.*

@Composable
fun ChessBoard(
    position: ChessPosition,
    theme: BoardTheme,
    pieceStyle: PieceStyle = theme.pieceStyle,
    selectedSquare: Square?,
    legalMoves: List<ChessMove>,
    lastMove: ChessMove?,
    engineArrow: ChessMove?,
    isFlipped: Boolean,
    onSquareClick: (Square) -> Unit,
    onPieceDragStart: () -> Unit = {},
    onPieceDragEnd: (Square, Square) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val minDimension = minOf(screenWidth - 24.dp, screenHeight - 160.dp, 440.dp)
    val outerBoardSize = maxOf(minDimension, 280.dp)
    val innerBoardPadding = 8.dp
    val innerBoardSize = outerBoardSize - (innerBoardPadding * 2)
    val squareSize = innerBoardSize / 8

    val kingSquareInCheck = if (position.isKingInCheck(position.activeColor)) {
        findKingSquare(position, position.activeColor)
    } else null

    // 1. Luxury 3D Outer Wooden Frame with Multi-stop Radial Grain & Cast Shadow
    Box(
        modifier = modifier
            .size(outerBoardSize)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.45f),
                spotColor = Color.Black.copy(alpha = 0.70f)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF3E2216), // Warm polished dark walnut
                        Color(0xFF2C1810), // Deep mahogany edge
                        Color(0xFF190D08)  // Outer shadow bevel
                    ),
                    radius = outerBoardSize.value * 1.4f
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(innerBoardPadding),
        contentAlignment = Alignment.Center
    ) {
        // 2. Inset Beveled Inner Board Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(6.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                )
        ) {
            // 3. The 8x8 Grid of Squares
            Column(modifier = Modifier.fillMaxSize()) {
                for (rank in 7 downTo 0) {
                    val displayRank = if (isFlipped) 7 - rank else rank
                    Row(modifier = Modifier.weight(1f)) {
                        for (file in 0..7) {
                            val displayFile = if (isFlipped) 7 - file else file
                            val square = Square(displayFile, displayRank)
                            val piece = position.pieceAt(square)
                            val isLight = (displayFile + displayRank) % 2 != 0
                            val isSelected = selectedSquare == square
                            val isLegalMove = legalMoves.any { it.to == square }
                            val isLastMove = lastMove?.from == square || lastMove?.to == square
                            val isCheck = kingSquareInCheck == square
                            val hasEngineArrow = engineArrow?.from == square || engineArrow?.to == square

                            SquareCell(
                                square = square,
                                piece = piece,
                                isLight = isLight,
                                isSelected = isSelected,
                                isLegalMove = isLegalMove,
                                isLastMove = isLastMove,
                                isCheck = isCheck,
                                hasEngineArrow = hasEngineArrow,
                                theme = theme,
                                pieceStyle = pieceStyle,
                                isFlipped = isFlipped,
                                size = squareSize,
                                onClick = { onSquareClick(square) },
                                onDragStart = onPieceDragStart,
                                onDragEnd = onPieceDragEnd,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Rank/File Coordinates Overlay (a-h, 1-8)
            BoardCoordinates(isFlipped = isFlipped, theme = theme)

            // 5. Helper Bot Animated Arrow Overlay
            if (engineArrow != null) {
                HelperArrowOverlay(
                    arrowMove = engineArrow,
                    isFlipped = isFlipped,
                    boardSize = innerBoardSize,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(10f)
                )
            }
        }
    }
}

private fun findKingSquare(position: ChessPosition, color: PieceColor): Square? {
    for (i in 0..63) {
        val p = position.pieceAtIndex(i)
        if (p != null && p.type == PieceType.KING && p.color == color) {
            return Square.fromIndex(i)
        }
    }
    return null
}
