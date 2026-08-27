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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.chess.model.*
import kotlin.math.min

@Composable
fun ChessBoard3D(
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
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(orientation, position) {
                    detectTapGestures { offset ->
                        val w = size.width
                        val h = size.height

                        // Reverse 3D isometric mapping approximation
                        val topY = h * 0.12f
                        val bottomY = h * 0.88f
                        val boardH = bottomY - topY

                        if (offset.y in topY..bottomY) {
                            val normY = (offset.y - topY) / boardH
                            val r = (normY * 8f).toInt().coerceIn(0, 7)

                            // Perspective width compression
                            val topWidthRatio = 0.72f
                            val currentWidthRatio = topWidthRatio + (1f - topWidthRatio) * normY
                            val rowWidth = w * currentWidthRatio
                            val leftX = (w - rowWidth) / 2f

                            if (offset.x in leftX..(leftX + rowWidth)) {
                                val normX = (offset.x - leftX) / rowWidth
                                val c = (normX * 8f).toInt().coerceIn(0, 7)

                                val file = if (orientation == PieceColor.WHITE) c else 7 - c
                                val rank = if (orientation == PieceColor.WHITE) 7 - r else r
                                onSquareClicked(Square(file, rank))
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            val topY = h * 0.12f
            val bottomY = h * 0.88f
            val boardH = bottomY - topY
            val topWidthRatio = 0.72f

            val lightColor = Color(theme.lightColor)
            val darkColor = Color(theme.darkColor)
            val selectedColor = Color(0xAAF6F669)
            val lastMoveColor = Color(0x77CDD26A)
            val checkColor = Color(0xDDDC2626)

            // Draw 3D Base Platform Bevel
            val bevelDepth = 24f
            val bevelColor = Color(0xFF3E2723)
            val bevelFrontColor = Color(0xFF2E1C12)

            val baseBottomLeft = Offset(0f, bottomY + bevelDepth)
            val baseBottomRight = Offset(w, bottomY + bevelDepth)
            val baseTopLeft = Offset((w * (1f - topWidthRatio)) / 2f, topY)
            val baseTopRight = Offset(w - (w * (1f - topWidthRatio)) / 2f, topY)

            // 3D Front Face
            val frontPath = Path().apply {
                moveTo(0f, bottomY)
                lineTo(w, bottomY)
                lineTo(baseBottomRight.x, baseBottomRight.y)
                lineTo(baseBottomLeft.x, baseBottomLeft.y)
                close()
            }
            drawPath(frontPath, bevelFrontColor)

            // 3D Side Bevel
            val sidePath = Path().apply {
                moveTo(0f, bottomY)
                lineTo(baseTopLeft.x, baseTopLeft.y)
                lineTo(baseTopLeft.x - 12f, baseTopLeft.y + 12f)
                lineTo(baseBottomLeft.x, baseBottomLeft.y)
                close()
            }
            drawPath(sidePath, bevelColor)

            // Helper to get 3D quad coordinates for row r, col c
            fun getSquareQuad(r: Int, c: Int): List<Offset> {
                val y1 = topY + (r / 8f) * boardH
                val y2 = topY + ((r + 1) / 8f) * boardH

                val ratio1 = topWidthRatio + (1f - topWidthRatio) * (r / 8f)
                val ratio2 = topWidthRatio + (1f - topWidthRatio) * ((r + 1) / 8f)

                val row1W = w * ratio1
                val row2W = w * ratio2

                val left1 = (w - row1W) / 2f
                val left2 = (w - row2W) / 2f

                val p0 = Offset(left1 + (c / 8f) * row1W, y1)
                val p1 = Offset(left1 + ((c + 1) / 8f) * row1W, y1)
                val p2 = Offset(left2 + ((c + 1) / 8f) * row2W, y2)
                val p3 = Offset(left2 + (c / 8f) * row2W, y2)

                return listOf(p0, p1, p2, p3)
            }

            // Draw Tiles in 3D
            for (r in 0..7) {
                for (c in 0..7) {
                    val quad = getSquareQuad(r, c)
                    val isLight = (r + c) % 2 == 0
                    val file = if (orientation == PieceColor.WHITE) c else 7 - c
                    val rank = if (orientation == PieceColor.WHITE) 7 - r else r
                    val sq = Square(file, rank)

                    var tileColor = if (isLight) lightColor else darkColor

                    if (lastMove != null && (lastMove.from == sq || lastMove.to == sq)) {
                        tileColor = lastMoveColor
                    }
                    if (selectedSquare != null && selectedSquare == sq) {
                        tileColor = selectedColor
                    }

                    val piece = position.pieceAt(sq)
                    if (piece != null && piece.type == PieceType.KING && piece.color == position.activeColor && position.isKingInCheck(piece.color)) {
                        tileColor = checkColor
                    }

                    val path = Path().apply {
                        moveTo(quad[0].x, quad[0].y)
                        lineTo(quad[1].x, quad[1].y)
                        lineTo(quad[2].x, quad[2].y)
                        lineTo(quad[3].x, quad[3].y)
                        close()
                    }
                    drawPath(path, tileColor)
                    drawPath(path, Color(0x33000000), style = Stroke(width = 1f))
                }
            }

            // Legal Move Highlights in 3D
            for (move in legalMoves) {
                val c = if (orientation == PieceColor.WHITE) move.to.file else 7 - move.to.file
                val r = if (orientation == PieceColor.WHITE) 7 - move.to.rank else move.to.rank
                val quad = getSquareQuad(r, c)
                val center = Offset(
                    (quad[0].x + quad[1].x + quad[2].x + quad[3].x) / 4f,
                    (quad[0].y + quad[1].y + quad[2].y + quad[3].y) / 4f
                )

                if (move.isCapture) {
                    drawCircle(
                        color = Color(0x77000000),
                        radius = (quad[2].x - quad[3].x) * 0.38f,
                        center = center,
                        style = Stroke(width = 4f)
                    )
                } else {
                    drawCircle(
                        color = Color(0x77000000),
                        radius = (quad[2].x - quad[3].x) * 0.16f,
                        center = center
                    )
                }
            }

            // Pieces in 3D (Render from back row 0 to front row 7 for natural z-ordering)
            for (r in 0..7) {
                for (c in 0..7) {
                    val file = if (orientation == PieceColor.WHITE) c else 7 - c
                    val rank = if (orientation == PieceColor.WHITE) 7 - r else r
                    val piece = position.pieceAt(Square(file, rank))
                    if (piece != null) {
                        val quad = getSquareQuad(r, c)
                        val sqWidth = quad[2].x - quad[3].x
                        val center = Offset(
                            (quad[0].x + quad[1].x + quad[2].x + quad[3].x) / 4f,
                            (quad[0].y + quad[1].y + quad[2].y + quad[3].y) / 4f - (sqWidth * 0.22f) // Elevated 3D lift
                        )

                        // 3D Drop Shadow
                        drawOval(
                            color = Color(0x44000000),
                            topLeft = Offset(center.x - sqWidth * 0.25f, (quad[2].y + quad[0].y) / 2f - 4f),
                            size = androidx.compose.ui.geometry.Size(sqWidth * 0.5f, sqWidth * 0.25f)
                        )

                        draw3DPiece(piece, center, sqWidth)
                    }
                }
            }
        }
    }
}

private fun DrawScope.draw3DPiece(
    piece: ChessPiece,
    center: Offset,
    sqSize: Float
) {
    val symbol = piece.unicodeSymbol
    val isWhite = piece.color == PieceColor.WHITE

    val paint = AndroidPaint().apply {
        textSize = sqSize * 0.95f
        textAlign = AndroidPaint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
        color = if (isWhite) android.graphics.Color.WHITE else android.graphics.Color.rgb(24, 24, 24)
        setShadowLayer(
            12f,
            0f,
            8f,
            if (isWhite) android.graphics.Color.argb(180, 0, 0, 0) else android.graphics.Color.argb(120, 255, 255, 255)
        )
    }

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
