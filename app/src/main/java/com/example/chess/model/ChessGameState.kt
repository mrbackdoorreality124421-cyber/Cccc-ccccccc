package com.example.chess.model

import com.example.chess.data.PuzzleRecord

enum class GameMode {
    PLAYER_VS_AI,
    HELPER_BOT,
    PLAYER_VS_PLAYER,
    ANALYSIS,
    TACTICAL_PUZZLE
}

enum class BoardTheme(val displayName: String, val lightColor: Long, val darkColor: Long, val accentColor: Long) {
    CLASSIC_WOOD("Classic Wood", 0xFFDFD8C8, 0xFF88735D, 0xFFD4AF37),
    TOURNAMENT_GREEN("Tournament Green", 0xFFEEEED2, 0xFF769656, 0xFFF6F669),
    MIDNIGHT_DARK("Midnight Dark", 0xFF2A2D34, 0xFF14171E, 0xFF4E9F3D),
    OCEAN_BLUE("Ocean Blue", 0xFFDDE6ED, 0xFF526D82, 0xFF2196F3)
}

data class PromotionPending(
    val from: Square,
    val to: Square,
    val pieceMoved: PieceType = PieceType.PAWN,
    val isCapture: Boolean = false,
    val capturedPiece: PieceType? = null
)

data class ChessGameState(
    val position: ChessPosition = ChessPosition.initial(),
    val moveHistory: List<ChessMove> = emptyList(),
    val positionHistory: List<ChessPosition> = listOf(ChessPosition.initial()),
    val redoStack: List<ChessMove> = emptyList(),
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<ChessMove> = emptyList(),
    val lastMove: ChessMove? = null,
    val engineArrowMove: ChessMove? = null,
    val isEngineThinking: Boolean = false,
    val engineEvaluationCp: Int = 0,
    val engineMateIn: Int? = null,
    val engineStatusMessage: String? = null,
    val gameMode: GameMode = GameMode.PLAYER_VS_AI,
    val boardOrientation: PieceColor = PieceColor.WHITE,
    val is3DView: Boolean = false,
    val isAssistantMode: Boolean = false,
    val isHapticEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.TOURNAMENT_GREEN,
    val aiSearchDepth: Int = 7,
    val aiMoveTimeMs: Int = 1200,
    val playerColor: PieceColor = PieceColor.WHITE,
    val helperBotColor: PieceColor = PieceColor.WHITE,
    val helperBotAutoPlay: Boolean = true,
    val promotionPending: PromotionPending? = null,
    val activePuzzle: PuzzleRecord? = null,
    val puzzleMoveIndex: Int = 0,
    val puzzleMessage: String? = null,
    val activeEngineName: String = "Built-in Grandmaster AI",
    val selectedOexEngineId: String? = null,
    val isStockfishActive: Boolean = false,
    val isExternalEngineRunning: Boolean = false
) {
    val isGameOver: Boolean
        get() = status != GameStatus.IN_PROGRESS

    val statusDescription: String
        get() = when (status) {
            GameStatus.IN_PROGRESS -> {
                val colorName = if (position.activeColor == PieceColor.WHITE) "White" else "Black"
                if (position.isKingInCheck(position.activeColor)) "$colorName is in CHECK!" else "$colorName's Turn"
            }
            GameStatus.CHECKMATE -> {
                val winner = if (position.activeColor == PieceColor.WHITE) "Black" else "White"
                "CHECKMATE! $winner wins."
            }
            GameStatus.STALEMATE -> "STALEMATE - Draw"
            GameStatus.DRAW_FIFTY_MOVE -> "Draw by 50-move rule"
            GameStatus.DRAW_REPETITION -> "Draw by threefold repetition"
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "Draw by insufficient material"
        }
}
