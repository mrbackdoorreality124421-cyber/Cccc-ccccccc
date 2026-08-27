package com.example.chess.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.chess.model.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .pointerInput(orientation, position) {
                    detectTapGestures { offset ->
                        val boardWidth = size.width
                        val sqSize = boardWidth / 8f
                        val col = (offset.x / sqSize).toInt().coerceIn(0, 7)
                        val row = (offset.y / sqSize).toInt().coerceIn(0, 7)

                        val file = if (orientation == PieceColor.WHITE) col else 7 - col
                        val rank = if (orientation == PieceColor.WHITE) 7 - row else row

                        onSquareClicked(Square(file, rank))
                    }
                }
        ) {
            val boardDim = min(size.width, size.height)
            val sqSize = boardDim / 8f

            val lightColor = Color(theme.lightColor)
            val darkColor = Color(theme.darkColor)
            val selectedColor = Color(0x99F6F669)
            val lastMoveColor = Color(0x66CDD26A)
            val checkColor = Color(0xDDDC2626)

            // Draw Board Tiles
            for (r in 0..7) {
                for (c in 0..7) {
                    val isLight = (r + c) % 2 == 0
                    val rectColor = if (isLight) lightColor else darkColor
                    val squareOffset = Offset(c * sqSize, r * sqSize)

                    drawRect(
                        color = rectColor,
                        topLeft = squareOffset,
                        size = Size(sqSize, sqSize)
                    )

                    val file = if (orientation == PieceColor.WHITE) c else 7 - c
                    val rank = if (orientation == PieceColor.WHITE) 7 - r else r
                    val sq = Square(file, rank)

                    // Last Move Highlight
                    if (lastMove != null && (lastMove.from == sq || lastMove.to == sq)) {
                        drawRect(
                            color = lastMoveColor,
                            topLeft = squareOffset,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // Selected Square Highlight
                    if (selectedSquare != null && selectedSquare == sq) {
                        drawRect(
                            color = selectedColor,
                            topLeft = squareOffset,
                            size = Size(sqSize, sqSize)
                        )
                    }

                    // King in Check Highlight
                    val piece = position.pieceAt(sq)
                    if (piece != null && piece.type == PieceType.KING && piece.color == position.activeColor) {
                        if (position.isKingInCheck(piece.color)) {
                            drawRect(
                                color = checkColor,
                                topLeft = squareOffset,
                                size = Size(sqSize, sqSize)
                            )
                        }
                    }

                    // Coordinate Labels (files on bottom rank, ranks on left col)
                    if (r == 7) {
                        val fileLetter = ('a'.code + file).toChar().toString()
                        drawCoordinateText(
                            text = fileLetter,
                            offset = Offset(c * sqSize + sqSize - 14f, r * sqSize + sqSize - 4f),
                            textColor = if (isLight) darkColor else lightColor,
                            textSizePx = sqSize * 0.22f
                        )
                    }
                    if (c == 0) {
                        val rankNumber = (rank + 1).toString()
                        drawCoordinateText(
                            text = rankNumber,
                            offset = Offset(c * sqSize + 4f, r * sqSize + 16f),
                            textColor = if (isLight) darkColor else lightColor,
                            textSizePx = sqSize * 0.22f
                        )
                    }
                }
            }

            // Legal Move Dots & Capture Rings
            val dotPaintColor = Color(0x66000000)
            for (move in legalMoves) {
                val c = if (orientation == PieceColor.WHITE) move.to.file else 7 - move.to.file
                val r = if (orientation == PieceColor.WHITE) 7 - move.to.rank else move.to.rank
                val center = Offset((c + 0.5f) * sqSize, (r + 0.5f) * sqSize)

                if (move.isCapture) {
                    drawCircle(
                        color = dotPaintColor,
                        radius = sqSize * 0.42f,
                        center = center,
                        style = Stroke(width = sqSize * 0.1f)
                    )
                } else {
                    drawCircle(
                        color = dotPaintColor,
                        radius = sqSize * 0.16f,
                        center = center
                    )
                }
            }

            // Pieces
            for (r in 0..7) {
                for (c in 0..7) {
                    val file = if (orientation == PieceColor.WHITE) c else 7 - c
                    val rank = if (orientation == PieceColor.WHITE) 7 - r else r
                    val piece = position.pieceAt(Square(file, rank))
                    if (piece != null) {
                        drawPieceSymbol(
                            piece = piece,
                            center = Offset((c + 0.5f) * sqSize, (r + 0.5f) * sqSize),
                            sqSize = sqSize
                        )
                    }
                }
            }

            // Engine Suggestion Arrow
            if (engineArrowMove != null) {
                drawEngineArrow(engineArrowMove, orientation, sqSize)
            }
        }
    }
}

private fun DrawScope.drawCoordinateText(
    text: String,
    offset: Offset,
    textColor: Color,
    textSizePx: Float
) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        offset.x,
        offset.y,
        AndroidPaint().apply {
            color = textColor.toArgb()
            textSize = textSizePx
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
    )
}

private fun DrawScope.drawPieceSymbol(
    piece: ChessPiece,
    center: Offset,
    sqSize: Float
) {
    val symbol = piece.unicodeSymbol
    val isWhite = piece.color == PieceColor.WHITE

    val paint = AndroidPaint().apply {
        textSize = sqSize * 0.82f
        textAlign = AndroidPaint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        color = if (isWhite) android.graphics.Color.WHITE else android.graphics.Color.rgb(20, 20, 20)
        setShadowLayer(
            6f,
            if (isWhite) 0f else 0f,
            if (isWhite) 4f else -2f,
            if (isWhite) android.graphics.Color.argb(160, 0, 0, 0) else android.graphics.Color.argb(100, 255, 255, 255)
        )
    }

    // Baseline vertical centering
    val textBounds = android.graphics.Rect()
    paint.getTextBounds(symbol, 0, symbol.length, textBounds)
    val yOffset = center.y + textBounds.height() / 2f - textBounds.bottom

    drawContext.canvas.nativeCanvas.drawText(
        symbol,
        center.x,
        yOffset,
        paint
    )
}

private fun DrawScope.drawEngineArrow(
    move: ChessMove,
    orientation: PieceColor,
    sqSize: Float
) {
    val fromC = if (orientation == PieceColor.WHITE) move.from.file else 7 - move.from.file
    val fromR = if (orientation == PieceColor.WHITE) 7 - move.from.rank else move.from.rank
    val toC = if (orientation == PieceColor.WHITE) move.to.file else 7 - move.to.file
    val toR = if (orientation == PieceColor.WHITE) 7 - move.to.rank else move.to.rank

    val start = Offset((fromC + 0.5f) * sqSize, (fromR + 0.5f) * sqSize)
    val end = Offset((toC + 0.5f) * sqSize, (toR + 0.5f) * sqSize)

    val arrowColor = Color(0xEE10B981)
    val strokeWidth = sqSize * 0.12f

    drawLine(
        color = arrowColor,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val angle = atan2(end.y - start.y, end.x - start.x)
    val headSize = sqSize * 0.28f

    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            end.x - headSize * cos(angle - Math.PI / 6).toFloat(),
            end.y - headSize * sin(angle - Math.PI / 6).toFloat()
        )
        lineTo(
            end.x - headSize * cos(angle + Math.PI / 6).toFloat(),
            end.y - headSize * sin(angle + Math.PI / 6).toFloat()
        )
        close()
    }

    drawPath(path, arrowColor)
}
