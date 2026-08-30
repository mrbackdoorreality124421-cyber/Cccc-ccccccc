package com.example.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceStyle
import com.example.chess.model.Square
import kotlin.math.roundToInt

@Composable
fun AnimatedPiece(
    piece: ChessPiece,
    square: Square,
    style: PieceStyle = PieceStyle.CLASSIC,
    size: Dp,
    isSelected: Boolean,
    isFlipped: Boolean = false,
    onDragStart: () -> Unit = {},
    onDragEnd: (Square, Square) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragAccumulatedOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val squareSizePx = with(density) { size.toPx() }

    // Smooth elevation when selected or dragged
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else if (isSelected) 8.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "piece_elevation"
    )

    // Spring scale effect
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.22f else if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
        label = "piece_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .zIndex(if (isDragging) 100f else if (isSelected) 50f else 1f)
            .offset {
                if (isDragging) {
                    IntOffset(dragAccumulatedOffset.x.roundToInt(), dragAccumulatedOffset.y.roundToInt())
                } else {
                    IntOffset.Zero
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = elevation,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.6f)
            )
            .pointerInput(square, isFlipped) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragAccumulatedOffset = Offset.Zero
                        onDragStart()
                    },
                    onDragEnd = {
                        isDragging = false
                        val dFiles = (dragAccumulatedOffset.x / squareSizePx).roundToInt()
                        val dRanks = (dragAccumulatedOffset.y / squareSizePx).roundToInt()

                        val targetFile = if (isFlipped) square.file - dFiles else square.file + dFiles
                        val targetRank = if (isFlipped) square.rank + dRanks else square.rank - dRanks

                        val targetSquare = Square(targetFile, targetRank)
                        if (targetSquare.isValid && targetSquare != square) {
                            onDragEnd(square, targetSquare)
                        }
                        dragAccumulatedOffset = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccumulatedOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatedOffset += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        ChessPieceView(
            piece = piece,
            style = style,
            size = size,
            isSelected = isSelected || isDragging
        )
    }
}
