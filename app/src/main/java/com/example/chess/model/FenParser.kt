package com.example.chess.model

/** Safe FEN parser used by Custom Board and image-puzzle import flows. */
object FenParser {
    fun parse(fen: String): Result<ChessPosition> = runCatching {
        val fields = fen.trim().split(Regex("\\s+"))
        require(fields.size in 2..6) { "FEN must contain at least board and active-color fields" }

        val ranks = fields[0].split('/')
        require(ranks.size == 8) { "FEN board must contain 8 ranks" }
        val board = MutableList<ChessPiece?>(64) { null }

        for ((fenRankIndex, rankText) in ranks.withIndex()) {
            var file = 0
            val rank = 7 - fenRankIndex
            for (ch in rankText) {
                when {
                    ch.isDigit() -> file += ch.digitToInt()
                    ch in "PNBRQKpnbrqk" -> {
                        require(file in 0..7) { "Invalid FEN rank" }
                        board[rank * 8 + file] = ChessPiece.fromFenChar(ch)
                        file++
                    }
                    else -> error("Invalid FEN character: $ch")
                }
            }
            require(file == 8) { "Each FEN rank must contain exactly 8 squares" }
        }

        val activeColor = when (fields.getOrNull(1)?.lowercase()) {
            "w" -> PieceColor.WHITE
            "b" -> PieceColor.BLACK
            else -> error("Invalid active color in FEN")
        }
        val castling = CastlingRights.fromFen(fields.getOrNull(2) ?: "-")
        val enPassant = fields.getOrNull(3)?.takeUnless { it == "-" }?.let {
            Square.fromAlgebraic(it) ?: error("Invalid en-passant square")
        }
        val halfmove = fields.getOrNull(4)?.toIntOrNull() ?: 0
        val fullmove = fields.getOrNull(5)?.toIntOrNull() ?: 1
        require(halfmove >= 0 && fullmove >= 1) { "Invalid FEN move counters" }

        ChessPosition(board, activeColor, castling, enPassant, halfmove, fullmove)
    }
}
