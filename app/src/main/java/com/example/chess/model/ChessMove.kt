package com.example.chess.model

data class ChessMove(
    val from: Square,
    val to: Square,
    val pieceMoved: PieceType,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val capturedPiece: PieceType? = null,
    val isCastleKingside: Boolean = false,
    val isCastleQueenside: Boolean = false,
    val isEnPassant: Boolean = false,
    val san: String = "",
    val isDrop: Boolean = false
) {
    val isCastle: Boolean
        get() = isCastleKingside || isCastleQueenside

    val uci: String
        get() {
            if (isDrop) {
                return "${pieceMoved.charUpper}@${to.algebraic}"
            }
            val promo = when (promotion) {
                PieceType.QUEEN -> "q"
                PieceType.ROOK -> "r"
                PieceType.BISHOP -> "b"
                PieceType.KNIGHT -> "n"
                else -> ""
            }
            return "${from.algebraic}${to.algebraic}$promo"
        }

    override fun toString(): String = san.ifEmpty { uci }
}
