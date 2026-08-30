package com.example.chess.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.example.chess.model.ChessMove
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun HelperArrowOverlay(
    arrowMove: ChessMove,
    isFlipped: Boolean,
    boardSize: Dp,
    modifier: Modifier = Modifier
) {
    val squareSize = boardSize / 8
    val density = LocalDensity.current

    // Calculate arrow start and end positions in pixels
    val fromFile = if (isFlipped) 7 - arrowMove.from.file else arrowMove.from.file
    val fromRank = if (isFlipped) 7 - arrowMove.from.rank else arrowMove.from.rank
    val toFile = if (isFlipped) 7 - arrowMove.to.file else arrowMove.to.file
    val toRank = if (isFlipped) 7 - arrowMove.to.rank else arrowMove.to.rank

    val startX = with(density) { (fromFile * squareSize.toPx() + squareSize.toPx() / 2) }
    val startY = with(density) { ((7 - fromRank) * squareSize.toPx() + squareSize.toPx() / 2) }
    val endX = with(density) { (toFile * squareSize.toPx() + squareSize.toPx() / 2) }
    val endY = with(density) { ((7 - toRank) * squareSize.toPx() + squareSize.toPx() / 2) }

    // Animation: arrow fades in smoothly
    val arrowAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "arrow_alpha"
    )

    // Pulse animation for target ring & highlight
    val infiniteTransition = rememberInfiniteTransition(label = "arrow_pulse_infinite")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_pulse"
    )

    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_glow_pulse"
    )

    // Arrow color based on move characteristics
    val arrowColor = when {
        arrowMove.isCapture -> Color(0xFFFF6D00) // Orange for captures
        arrowMove.isCastle -> Color(0xFF00BCD4)  // Cyan for castling
        arrowMove.promotion != null -> Color(0xFFE040FB) // Purple for promotion
        else -> Color(0xFF76FF03) // Bright vibrant lime green for normal moves
    }

    Canvas(
        modifier = modifier
            .size(boardSize)
            .graphicsLayer { alpha = arrowAlpha }
            .pointerInput(Unit) {
                // Allow touches to pass through to squares beneath
            }
    ) {
        val startOffset = Offset(startX, startY)
        val endOffset = Offset(endX, endY)

        // Calculate arrow direction
        val dx = endX - startX
        val dy = endY - startY
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < 1f) return@Canvas // Don't draw if same square

        // Normalize direction
        val dirX = dx / distance
        val dirY = dy / distance

        // Perpendicular for arrow head
        val perpX = -dirY
        val perpY = dirX

        // Arrow shaft thickness and head metrics
        val shaftWidth = squareSize.toPx() * 0.13f
        val headLength = squareSize.toPx() * 0.36f
        val headWidth = squareSize.toPx() * 0.46f

        // Shorten end slightly so arrow points cleanly at the target center without completely overlapping pieces
        val endShortenedX = endX - dirX * (squareSize.toPx() * 0.18f)
        val endShortenedY = endY - dirY * (squareSize.toPx() * 0.18f)
        val endShortened = Offset(endShortenedX, endShortenedY)

        // Arrow head points
        val headBaseX = endShortenedX - dirX * headLength
        val headBaseY = endShortenedY - dirY * headLength

        val headLeftX = headBaseX + perpX * headWidth / 2
        val headLeftY = headBaseY + perpY * headWidth / 2
        val headRightX = headBaseX - perpX * headWidth / 2
        val headRightY = headBaseY - perpY * headWidth / 2

        // 1. Draw Cast Drop Shadow (for luxury depth on board)
        val shadowOffset = 4f
        drawArrowShaft(
            start = Offset(startX + shadowOffset, startY + shadowOffset),
            end = Offset(headBaseX + shadowOffset, headBaseY + shadowOffset),
            width = shaftWidth,
            color = Color.Black.copy(alpha = 0.40f)
        )
        drawArrowHead(
            tip = Offset(endShortenedX + shadowOffset, endShortenedY + shadowOffset),
            left = Offset(headLeftX + shadowOffset, headLeftY + shadowOffset),
            right = Offset(headRightX + shadowOffset, headRightY + shadowOffset),
            color = Color.Black.copy(alpha = 0.40f)
        )

        // 2. Glow Layer (wider, pulsing semi-transparent neon glow)
        val glowColor = arrowColor.copy(alpha = pulseGlowAlpha)
        drawArrowShaft(
            start = startOffset,
            end = Offset(headBaseX, headBaseY),
            width = shaftWidth * 1.8f,
            color = glowColor
        )
        drawArrowHead(
            tip = endShortened,
            left = Offset(headLeftX, headLeftY),
            right = Offset(headRightX, headRightY),
            color = glowColor
        )

        // 3. Main Solid Arrow Shaft and Head
        drawArrowShaft(
            start = startOffset,
            end = Offset(headBaseX, headBaseY),
            width = shaftWidth,
            color = arrowColor
        )
        drawArrowHead(
            tip = endShortened,
            left = Offset(headLeftX, headLeftY),
            right = Offset(headRightX, headRightY),
            color = arrowColor
        )

        // 4. Highlight Origin Disc on "from" square
        drawCircle(
            color = arrowColor.copy(alpha = 0.85f),
            radius = squareSize.toPx() * 0.16f,
            center = startOffset
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.90f),
            radius = squareSize.toPx() * 0.07f,
            center = startOffset
        )

        // 5. Pulsing Target Reticle on "to" square center
        drawCircle(
            color = arrowColor.copy(alpha = 0.9f),
            radius = squareSize.toPx() * 0.22f * pulseScale,
            center = endOffset,
            style = Stroke(width = 3.5f)
        )
        drawCircle(
            color = arrowColor.copy(alpha = 0.35f),
            radius = squareSize.toPx() * 0.10f * pulseScale,
            center = endOffset
        )
    }
}

private fun DrawScope.drawArrowShaft(
    start: Offset,
    end: Offset,
    width: Float,
    color: Color
) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f) return

    val perpX = -dy / length * width / 2
    val perpY = dx / length * width / 2

    val path = Path().apply {
        moveTo(start.x + perpX, start.y + perpY)
        lineTo(end.x + perpX, end.y + perpY)
        lineTo(end.x - perpX, end.y - perpY)
        lineTo(start.x - perpX, start.y - perpY)
        close()
    }

    drawPath(path, color = color)
}

private fun DrawScope.drawArrowHead(
    tip: Offset,
    left: Offset,
    right: Offset,
    color: Color
) {
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color = color)
}
