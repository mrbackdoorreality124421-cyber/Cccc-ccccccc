package com.example.chess.model

object FenUtils {
    const val STARTING_FEN: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    fun getStartingPosition(): String = STARTING_FEN

    fun validateFen(fen: String): Boolean {
        return ChessPosition.fromFen(fen) != null
    }
}
