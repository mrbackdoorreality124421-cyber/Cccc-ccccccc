package com.example.chess.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.components.*
import com.example.chess.utils.PgnUtils

@Composable
fun GameScreen(
    viewModel: ChessViewModel,
    onBackToMenu: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var showNewGameDialog by remember { mutableStateOf(false) }
    var showPgnDialog by remember { mutableStateOf(false) }
    var pgnExportText by remember { mutableStateOf("") }

    // Recommendation arrow for current turn
    val shouldShowArrow = when {
        state.gameMode == GameMode.HELPER_BOT -> state.position.activeColor == state.helperBotColor && state.status == GameStatus.IN_PROGRESS && !state.isEngineThinking
        state.gameMode == GameMode.PLAYER_VS_AI -> state.position.activeColor == state.playerColor && state.isAssistantMode && state.status == GameStatus.IN_PROGRESS && !state.isEngineThinking
        state.gameMode == GameMode.ANALYSIS -> state.status == GameStatus.IN_PROGRESS && !state.isEngineThinking
        state.isAssistantMode -> state.status == GameStatus.IN_PROGRESS && !state.isEngineThinking
        else -> false
    }
    val activeArrow = if (shouldShowArrow) state.engineArrowMove else null

    // Compute captured pieces for White & Black
    val allStartingWhitePieces = mapOf(
        PieceType.PAWN to 8, PieceType.KNIGHT to 2, PieceType.BISHOP to 2,
        PieceType.ROOK to 2, PieceType.QUEEN to 1
    )
    val allStartingBlackPieces = mapOf(
        PieceType.PAWN to 8, PieceType.KNIGHT to 2, PieceType.BISHOP to 2,
        PieceType.ROOK to 2, PieceType.QUEEN to 1
    )

    val currentWhitePieces = mutableMapOf<PieceType, Int>()
    val currentBlackPieces = mutableMapOf<PieceType, Int>()
    for (i in 0..63) {
        val p = state.position.pieceAtIndex(i) ?: continue
        if (p.type == PieceType.KING) continue
        if (p.color == PieceColor.WHITE) {
            currentWhitePieces[p.type] = (currentWhitePieces[p.type] ?: 0) + 1
        } else {
            currentBlackPieces[p.type] = (currentBlackPieces[p.type] ?: 0) + 1
        }
    }

    // Black pieces captured by White
    val blackCapturedByWhite = mutableListOf<PieceType>()
    for ((type, count) in allStartingBlackPieces) {
        val remaining = currentBlackPieces[type] ?: 0
        repeat(count - remaining) { blackCapturedByWhite.add(type) }
    }

    // White pieces captured by Black
    val whiteCapturedByBlack = mutableListOf<PieceType>()
    for ((type, count) in allStartingWhitePieces) {
        val remaining = currentWhitePieces[type] ?: 0
        repeat(count - remaining) { whiteCapturedByBlack.add(type) }
    }

    val whiteMaterial = currentWhitePieces.entries.sumOf { it.key.baseValue * it.value }
    val blackMaterial = currentBlackPieces.entries.sumOf { it.key.baseValue * it.value }
    val whiteAdvantage = ((whiteMaterial - blackMaterial) / 100).coerceAtLeast(0)
    val blackAdvantage = ((blackMaterial - whiteMaterial) / 100).coerceAtLeast(0)

    val topCaptures = if (state.boardOrientation == PieceColor.WHITE) whiteCapturedByBlack else blackCapturedByWhite
    val topColor = if (state.boardOrientation == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
    val topAdvantage = if (state.boardOrientation == PieceColor.WHITE) blackAdvantage else whiteAdvantage

    val bottomCaptures = if (state.boardOrientation == PieceColor.WHITE) blackCapturedByWhite else whiteCapturedByBlack
    val bottomColor = if (state.boardOrientation == PieceColor.WHITE) PieceColor.WHITE else PieceColor.BLACK
    val bottomAdvantage = if (state.boardOrientation == PieceColor.WHITE) whiteAdvantage else blackAdvantage

    Scaffold(
        topBar = {
            GameTopBar(
                whitePlayer = state.whitePlayer,
                blackPlayer = state.blackPlayer,
                evaluation = state.engineEvaluationCp,
                mateIn = state.engineMateIn,
                depth = state.engineCurrentDepth,
                isThinking = state.isEngineThinking,
                isAssistantActive = state.isAssistantMode,
                isSoundActive = state.isSoundEnabled,
                onBack = onBackToMenu,
                onToggleAssistant = { viewModel.toggleAssistant() },
                onToggleSound = { viewModel.toggleSound() },
                onSettings = onOpenSettings
            )
        },
        containerColor = Color(0xFF0F141C),
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (isLandscape) {
            // LANDSCAPE LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Board & Captured Pieces
                Column(
                    modifier = Modifier.weight(1.1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CapturedPiecesBar(
                        capturedPieces = topCaptures,
                        color = topColor,
                        materialAdvantage = topAdvantage,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    ChessBoard(
                        position = state.position,
                        theme = state.boardTheme,
                        pieceStyle = state.pieceStyle,
                        selectedSquare = state.selectedSquare,
                        legalMoves = state.legalMovesForSelected,
                        lastMove = state.lastMove,
                        engineArrow = activeArrow,
                        isFlipped = state.boardOrientation == PieceColor.BLACK,
                        onSquareClick = { viewModel.onSquareClicked(it) },
                        onPieceDragStart = { /* Haptics handled in cell */ },
                        onPieceDragEnd = { from, to -> viewModel.onPieceDrop(from, to) }
                    )

                    CapturedPiecesBar(
                        capturedPieces = bottomCaptures,
                        color = bottomColor,
                        materialAdvantage = bottomAdvantage,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Right Pane: Evaluation, Moves, Controls
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    EvaluationBar(
                        evaluation = state.engineEvaluationCp,
                        mateIn = state.engineMateIn,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    MoveListCompact(
                        moves = state.moveHistory,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    )

                    if (state.gameMode == GameMode.HELPER_BOT || state.isAssistantMode) {
                        HelperInfoBar(
                            arrowMove = state.engineArrowMove,
                            evaluation = state.engineEvaluationCp,
                            mateIn = state.engineMateIn,
                            isThinking = state.isEngineThinking,
                            onRequestHint = { viewModel.requestFreshHint() }
                        )
                    }

                    GameControlBar(
                        canUndo = state.moveHistory.isNotEmpty() && !state.isEngineThinking,
                        canRedo = state.redoStack.isNotEmpty() && !state.isEngineThinking,
                        onUndo = { viewModel.undoMove() },
                        onRedo = { viewModel.redoMove() },
                        onFlip = { viewModel.flipBoard() },
                        onNewGame = { showNewGameDialog = true },
                        onHint = { viewModel.requestFreshHint() }
                    )
                }
            }
        } else {
            // PORTRAIT LAYOUT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Opponent Captured Pieces & Eval Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EvaluationBar(
                        evaluation = state.engineEvaluationCp,
                        mateIn = state.engineMateIn
                    )

                    CapturedPiecesBar(
                        capturedPieces = topCaptures,
                        color = topColor,
                        materialAdvantage = topAdvantage
                    )
                }

                // Master Wooden Chess Board
                ChessBoard(
                    position = state.position,
                    theme = state.boardTheme,
                    pieceStyle = state.pieceStyle,
                    selectedSquare = state.selectedSquare,
                    legalMoves = state.legalMovesForSelected,
                    lastMove = state.lastMove,
                    engineArrow = activeArrow,
                    isFlipped = state.boardOrientation == PieceColor.BLACK,
                    onSquareClick = { viewModel.onSquareClicked(it) },
                    onPieceDragStart = { },
                    onPieceDragEnd = { from, to -> viewModel.onPieceDrop(from, to) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                // Helper Bot Suggestion Bar
                if (state.gameMode == GameMode.HELPER_BOT || state.isAssistantMode) {
                    HelperInfoBar(
                        arrowMove = state.engineArrowMove,
                        evaluation = state.engineEvaluationCp,
                        mateIn = state.engineMateIn,
                        isThinking = state.isEngineThinking,
                        onRequestHint = { viewModel.requestFreshHint() }
                    )
                }

                // Player Captured Pieces Bar
                CapturedPiecesBar(
                    capturedPieces = bottomCaptures,
                    color = bottomColor,
                    materialAdvantage = bottomAdvantage
                )

                // Move List Bar
                MoveListCompact(
                    moves = state.moveHistory,
                    modifier = Modifier.height(40.dp)
                )

                // Controls Bar
                GameControlBar(
                    canUndo = state.moveHistory.isNotEmpty() && !state.isEngineThinking,
                    canRedo = state.redoStack.isNotEmpty() && !state.isEngineThinking,
                    onUndo = { viewModel.undoMove() },
                    onRedo = { viewModel.redoMove() },
                    onFlip = { viewModel.flipBoard() },
                    onNewGame = { showNewGameDialog = true },
                    onHint = { viewModel.requestFreshHint() }
                )
            }
        }
    }

    // Pawn Promotion Dialog
    if (state.promotionPending != null) {
        PromotionDialog(
            color = state.position.activeColor,
            style = state.pieceStyle,
            onPieceSelected = { viewModel.onPromotionPieceSelected(it) },
            onDismiss = { viewModel.onPromotionDismissed() }
        )
    }

    // Game Over Celebration Dialog
    if (state.isGameOver) {
        GameOverDialog(
            status = state.status,
            statusDescription = state.statusDescription,
            onRematch = {
                viewModel.resetGame()
            },
            onAnalysis = {
                viewModel.startAnalysis()
            },
            onDismiss = { }
        )
    }

    // New Game Confirmation Dialog
    if (showNewGameDialog) {
        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text("Restart Game", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Start a fresh match from the initial position?", color = Color(0xFF94A3B8)) },
            confirmButton = {
                Button(
                    onClick = {
                        showNewGameDialog = false
                        viewModel.resetGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Restart", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF161E2B)
        )
    }
}
