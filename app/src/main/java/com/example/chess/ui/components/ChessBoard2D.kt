package com.example.chess.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.chess.model.*

@Composable
fun ChessBoard2D(
    position: ChessPosition,
    orientation: PieceColor,
    selectedSquare: Square?,
    legalMoves: List<ChessMove>,
    lastMove: ChessMove?,
    engineArrowMove: ChessMove?,
    theme: BoardTheme,
    onSquareClicked: (Square) -> Unit,
    onPieceDragStart: () -> Unit = {},
    onPieceDragEnd: (Square, Square) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    ChessBoard(
        position = position,
        theme = theme,
        pieceStyle = theme.pieceStyle,
        selectedSquare = selectedSquare,
        legalMoves = legalMoves,
        lastMove = lastMove,
        engineArrow = engineArrowMove,
        isFlipped = orientation == PieceColor.BLACK,
        onSquareClick = onSquareClicked,
        onPieceDragStart = onPieceDragStart,
        onPieceDragEnd = onPieceDragEnd,
        modifier = modifier
    )
}
