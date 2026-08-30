package com.example.chess.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.ChessPiece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceStyle
import com.example.chess.model.PieceType

@Composable
fun ChessPieceView(
    piece: ChessPiece,
    style: PieceStyle = PieceStyle.CLASSIC,
    size: Dp,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "piece_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 8.dp else 1.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            ),
        contentAlignment = Alignment.Center
    ) {
        when (style) {
            PieceStyle.CLASSIC -> ClassicPiece(piece = piece, size = size)
            PieceStyle.MODERN -> ModernPiece(piece = piece, size = size)
            PieceStyle.NEO -> NeoPiece(piece = piece, size = size)
        }
    }
}

// CLASSIC STYLE — High-detail Staunton Vector Canvas
@Composable
fun ClassicPiece(piece: ChessPiece, size: Dp) {
    val fillColor = if (piece.color == PieceColor.WHITE) {
        Color(0xFFF7F7F7) // Rich ivory white
    } else {
        Color(0xFF222428) // Deep jet obsidian
    }

    val outlineColor = if (piece.color == PieceColor.WHITE) {
        Color(0xFF475569) // Crisp slate outline
    } else {
        Color(0xFF0F172A) // Deep charcoal rim
    }

    Canvas(modifier = Modifier.size(size * 0.90f)) {
        val canvasSize = this.size.minDimension
        val center = Offset(canvasSize / 2f, canvasSize / 2f)

        when (piece.type) {
            PieceType.KING -> PieceDrawers.drawKing(this, center, canvasSize, fillColor, outlineColor)
            PieceType.QUEEN -> PieceDrawers.drawQueen(this, center, canvasSize, fillColor, outlineColor)
            PieceType.ROOK -> PieceDrawers.drawRook(this, center, canvasSize, fillColor, outlineColor)
            PieceType.BISHOP -> PieceDrawers.drawBishop(this, center, canvasSize, fillColor, outlineColor)
            PieceType.KNIGHT -> PieceDrawers.drawKnight(this, center, canvasSize, fillColor, outlineColor)
            PieceType.PAWN -> PieceDrawers.drawPawn(this, center, canvasSize, fillColor, outlineColor)
        }
    }
}

// MODERN STYLE — Clean Minimal Circular Badges with High Contrast Glyphs
@Composable
fun ModernPiece(piece: ChessPiece, size: Dp) {
    val isWhite = piece.color == PieceColor.WHITE
    val bgColor = if (isWhite) Color(0xFFF8FAFC) else Color(0xFF1E293B)
    val textColor = if (isWhite) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val ringColor = if (isWhite) Color(0xFFCBD5E1) else Color(0xFF334155)

    Box(
        modifier = Modifier
            .size(size * 0.82f)
            .background(bgColor, CircleShape)
            .border(2.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val symbol = piece.unicodeSymbol
        Text(
            text = symbol,
            color = textColor,
            fontSize = (size.value * 0.52f).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

// NEO STYLE — Futuristic Glowing Vector Token Cards
@Composable
fun NeoPiece(piece: ChessPiece, size: Dp) {
    val isWhite = piece.color == PieceColor.WHITE
    val primaryBg = if (isWhite) Color(0xFF1E293B) else Color(0xFF0B0F17)
    val glowColor = if (isWhite) Color(0xFF38BDF8) else Color(0xFFA855F7)

    Box(
        modifier = Modifier
            .size(size * 0.84f)
            .shadow(6.dp, shape = RoundedCornerShape(10.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(primaryBg, primaryBg.copy(alpha = 0.95f))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(1.5.dp, glowColor.copy(alpha = 0.7f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = when (piece.type) {
                PieceType.KING -> Icons.Default.Shield
                PieceType.QUEEN -> Icons.Default.AutoAwesome
                PieceType.ROOK -> Icons.Default.Fort
                PieceType.BISHOP -> Icons.Default.Navigation
                PieceType.KNIGHT -> Icons.Default.FlashOn
                PieceType.PAWN -> Icons.Default.Circle
            },
            contentDescription = piece.type.name,
            tint = glowColor,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}
