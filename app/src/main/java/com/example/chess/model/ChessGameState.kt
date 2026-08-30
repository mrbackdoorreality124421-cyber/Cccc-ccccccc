package com.example.chess.model

import com.example.chess.data.PuzzleRecord

enum class GameMode {
    PLAYER_VS_AI,
    PLAYER_VS_PLAYER,
    HELPER_BOT,
    ANALYSIS,
    TACTICAL_PUZZLE
}

enum class PieceStyle(val displayName: String) {
    CLASSIC("Classic Staunton"),
    MODERN("Modern Vector"),
    NEO("Neo Futuristic")
}

enum class BoardTheme(
    val displayName: String,
    val lightColor: Long,
    val darkColor: Long,
    val accentColor: Long,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC
) {
    CLASSIC_WOOD("Classic Wood", 0xFFF0D9B5, 0xFFB58863, 0xFF8B4513, PieceStyle.CLASSIC),
    DARK_CHARCOAL("Dark Charcoal", 0xFF4A4A4A, 0xFF2B2B2B, 0xFF607D8B, PieceStyle.MODERN),
    TOURNAMENT_GREEN("Tournament Green", 0xFFEBECD0, 0xFF779556, 0xFF33691E, PieceStyle.CLASSIC),
    ROYAL_BLUE("Royal Blue", 0xFFDEE3E6, 0xFF5B8DB8, 0xFF1565C0, PieceStyle.MODERN),
    MIDNIGHT("Midnight", 0xFF3D3D3D, 0xFF1A1A1A, 0xFF9C27B0, PieceStyle.NEO),
    CHERRY_WOOD("Cherry Wood", 0xFFF5E6D3, 0xFF8B4513, 0xFF5D4037, PieceStyle.CLASSIC),
    GLASS("Glass", 0x80FFFFFF, 0x40000000, 0xFF00BCD4, PieceStyle.NEO)
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
    val engineCurrentDepth: Int = 0,
    val engineStatusMessage: String? = null,
    val gameMode: GameMode = GameMode.PLAYER_VS_AI,
    val difficultyLevel: Int = 5, // 1=Beginner, 2=Easy, 3=Medium, 4=Hard, 5=Master (MAX POWER)
    val boardOrientation: PieceColor = PieceColor.WHITE,
    val is3DView: Boolean = false,
    val isAssistantMode: Boolean = false,
    val isFenGame: Boolean = false,
    val isHapticEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val boardTheme: BoardTheme = BoardTheme.CLASSIC_WOOD,
    val pieceStyle: PieceStyle = PieceStyle.CLASSIC,
    val aiSearchDepth: Int = 30,
    val aiMoveTimeMs: Int = 5000,
    val playerColor: PieceColor = PieceColor.WHITE,
    val helperBotColor: PieceColor = PieceColor.WHITE,
    val helperBotAutoPlay: Boolean = true,
    val promotionPending: PromotionPending? = null,
    val activePuzzle: PuzzleRecord? = null,
    val puzzleMoveIndex: Int = 0,
    val puzzleMessage: String? = null,
    val activeEngineName: String = "Stockfish 18",
    val selectedOexEngineId: String? = null,
    val isStockfishActive: Boolean = false,
    val isExternalEngineRunning: Boolean = false,
    val engineErrorMessage: String? = null
) {
    val isGameOver: Boolean
        get() = status != GameStatus.IN_PROGRESS

    val whitePlayer: String
        get() = when (gameMode) {
            GameMode.PLAYER_VS_AI -> if (playerColor == PieceColor.WHITE) "You" else "Stockfish 18 (Master)"
            GameMode.HELPER_BOT -> if (helperBotColor == PieceColor.WHITE) "Helper Bot" else "You"
            GameMode.PLAYER_VS_PLAYER -> "White"
            GameMode.ANALYSIS -> "White"
            GameMode.TACTICAL_PUZZLE -> if (playerColor == PieceColor.WHITE) "You" else "Puzzle Bot"
        }

    val blackPlayer: String
        get() = when (gameMode) {
            GameMode.PLAYER_VS_AI -> if (playerColor == PieceColor.BLACK) "You" else "Stockfish 18 (Master)"
            GameMode.HELPER_BOT -> if (helperBotColor == PieceColor.BLACK) "Helper Bot" else "You"
            GameMode.PLAYER_VS_PLAYER -> "Black"
            GameMode.ANALYSIS -> "Black"
            GameMode.TACTICAL_PUZZLE -> if (playerColor == PieceColor.BLACK) "You" else "Puzzle Bot"
        }

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
