package com.example.chess.model

enum class PieceType(val charUpper: Char, val baseValue: Int) {
    PAWN('P', 100),
    KNIGHT('N', 320),
    BISHOP('B', 330),
    ROOK('R', 500),
    QUEEN('Q', 900),
    KING('K', 20000);

    val char: Char get() = charUpper
    val value: Int get() = baseValue

    val sanLetter: String
        get() = if (this == PAWN) "" else charUpper.toString()
}

enum class PieceColor {
    WHITE, BLACK;

    val opponent: PieceColor
        get() = if (this == WHITE) BLACK else WHITE

    val direction: Int
        get() = if (this == WHITE) 1 else -1

    val pawnStartRank: Int
        get() = if (this == WHITE) 1 else 6

    val promotionRank: Int
        get() = if (this == WHITE) 7 else 0

    val backRank: Int
        get() = if (this == WHITE) 0 else 7
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.charUpper else type.charUpper.lowercaseChar()

    val unicodeSymbol: String
        get() = when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }

    companion object {
        fun fromFenChar(char: Char): ChessPiece? {
            val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (char.uppercaseChar()) {
                'P' -> PieceType.PAWN
                'N' -> PieceType.KNIGHT
                'B' -> PieceType.BISHOP
                'R' -> PieceType.ROOK
                'Q' -> PieceType.QUEEN
                'K' -> PieceType.KING
                else -> return null
            }
            return ChessPiece(type, color)
        }
    }
}
