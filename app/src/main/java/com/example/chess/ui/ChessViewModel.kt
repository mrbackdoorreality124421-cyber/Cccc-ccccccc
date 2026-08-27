package com.example.chess.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.ChessSoundManager
import com.example.chess.data.ChessDatabase
import com.example.chess.data.ChessRepository
import com.example.chess.data.GameRecord
import com.example.chess.data.PuzzleRecord
import com.example.chess.engine.AlphaBetaEngine
import com.example.chess.engine.OexEngineInfo
import com.example.chess.engine.OexEngineManager
import com.example.chess.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChessDatabase.getDatabase(application, viewModelScope)
    private val repository = ChessRepository(db.chessDao())
    private val alphaBetaEngine = AlphaBetaEngine()
    private val oexEngineManager = OexEngineManager(application)
    private val soundManager = ChessSoundManager()

    private val _uiState = MutableStateFlow(ChessGameState())
    val uiState: StateFlow<ChessGameState> = _uiState.asStateFlow()

    private val _discoveredOexEngines = MutableStateFlow<List<OexEngineInfo>>(emptyList())
    val discoveredOexEngines: StateFlow<List<OexEngineInfo>> = _discoveredOexEngines.asStateFlow()

    val allGameHistory = repository.allGameRecords
    val allPuzzles = repository.allPuzzles

    private var aiJob: Job? = null
    private var gameStartTime = System.currentTimeMillis()

    init {
        scanOexEngines()
        viewModelScope.launch {
            repository.seedDefaultPuzzlesIfEmpty()
        }
        updateAssistantEvaluation()
    }

    fun scanOexEngines() {
        viewModelScope.launch {
            val engines = oexEngineManager.discoverEngines()
            _discoveredOexEngines.value = engines

            // Auto-detect and connect to Stockfish if present
            val stockfish = engines.find { it.isStockfish } ?: engines.firstOrNull()
            if (stockfish != null && _uiState.value.selectedOexEngineId == null) {
                selectEngine(stockfish)
            }
        }
    }

    fun selectEngine(engine: OexEngineInfo?) {
        viewModelScope.launch {
            if (engine == null) {
                oexEngineManager.stopEngine()
                _uiState.update {
                    it.copy(
                        selectedOexEngineId = null,
                        activeEngineName = "Built-in Grandmaster AI",
                        isStockfishActive = false,
                        isExternalEngineRunning = false
                    )
                }
            } else {
                val started = oexEngineManager.startEngine(engine)
                _uiState.update {
                    it.copy(
                        selectedOexEngineId = engine.id,
                        activeEngineName = engine.name,
                        isStockfishActive = engine.isStockfish,
                        isExternalEngineRunning = started
                    )
                }
            }
            updateAssistantEvaluation()
        }
    }

    fun importCustomEngine(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = oexEngineManager.importCustomEngineFromUri(uri)
            if (result.isSuccess) {
                val engine = result.getOrNull()
                val engines = oexEngineManager.discoverEngines()
                _discoveredOexEngines.value = engines
                if (engine != null) {
                    selectEngine(engine)
                }
                onResult(true, "Engine imported successfully: ${engine?.name}")
            } else {
                onResult(false, "Sorry, engine not detected.")
            }
        }
    }

    fun startPlayerVsBot(color: PieceColor = PieceColor.WHITE) {
        resetGame()
        _uiState.update {
            it.copy(
                gameMode = GameMode.PLAYER_VS_AI,
                playerColor = color,
                boardOrientation = color
            )
        }
        if (color == PieceColor.BLACK) {
            triggerAiMove(_uiState.value.position)
        }
    }

    fun startHelperBot(botColor: PieceColor = PieceColor.WHITE, autoPlay: Boolean = true) {
        resetGame()
        _uiState.update {
            it.copy(
                gameMode = GameMode.HELPER_BOT,
                playerColor = botColor,
                helperBotColor = botColor,
                helperBotAutoPlay = autoPlay,
                boardOrientation = botColor,
                isAssistantMode = true
            )
        }
        if (botColor == PieceColor.WHITE) {
            triggerHelperBotMove(_uiState.value.position)
        }
    }

    fun toggleHelperAutoPlay() {
        val newAutoPlay = !_uiState.value.helperBotAutoPlay
        _uiState.update { it.copy(helperBotAutoPlay = newAutoPlay) }
        if (newAutoPlay && _uiState.value.gameMode == GameMode.HELPER_BOT && _uiState.value.position.activeColor == _uiState.value.helperBotColor) {
            triggerHelperBotMove(_uiState.value.position)
        }
    }

    fun triggerManualHelperStep() {
        if (_uiState.value.gameMode == GameMode.HELPER_BOT) {
            triggerHelperBotMove(_uiState.value.position)
        }
    }

    fun onSquareClicked(square: Square) {
        val state = _uiState.value
        if (state.isGameOver || state.isEngineThinking) return

        // In PvE mode, don't allow moving AI's pieces
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) {
            return
        }

        val clickedPiece = state.position.pieceAt(square)

        // Case 1: Square is a target of an already selected piece's legal move
        val matchingMove = state.legalMovesForSelected.find { it.to == square }
        if (matchingMove != null) {
            // Check if pawn promotion requires user piece choice
            if (matchingMove.pieceMoved == PieceType.PAWN &&
                (matchingMove.to.rank == 7 || matchingMove.to.rank == 0) &&
                matchingMove.promotion == null
            ) {
                _uiState.update {
                    it.copy(
                        promotionPending = PromotionPending(
                            from = matchingMove.from,
                            to = matchingMove.to,
                            pieceMoved = PieceType.PAWN,
                            isCapture = matchingMove.isCapture,
                            capturedPiece = matchingMove.capturedPiece
                        )
                    )
                }
                return
            }

            executeMove(matchingMove)
            return
        }

        // Case 2: Player selects their own piece
        if (clickedPiece != null && clickedPiece.color == state.position.activeColor) {
            val allLegal = state.position.generateLegalMoves()
            val pieceLegalMoves = allLegal.filter { it.from == square }
            _uiState.update {
                it.copy(
                    selectedSquare = square,
                    legalMovesForSelected = pieceLegalMoves
                )
            }
            vibrate(15)
            return
        }

        // Case 3: Player tapped empty square or invalid target
        _uiState.update {
            it.copy(
                selectedSquare = null,
                legalMovesForSelected = emptyList()
            )
        }
    }

    fun onPromotionPieceSelected(pieceType: PieceType) {
        val pending = _uiState.value.promotionPending ?: return
        val move = ChessMove(
            from = pending.from,
            to = pending.to,
            pieceMoved = pending.pieceMoved,
            promotion = pieceType,
            isCapture = pending.isCapture,
            capturedPiece = pending.capturedPiece
        )
        _uiState.update { it.copy(promotionPending = null) }
        executeMove(move)
    }

    fun onPromotionDismissed() {
        _uiState.update {
            it.copy(
                promotionPending = null,
                selectedSquare = null,
                legalMovesForSelected = emptyList()
            )
        }
    }

    private fun executeMove(move: ChessMove) {
        val currentState = _uiState.value
        val nextPos = currentState.position.makeMove(move)
        val updatedHistory = currentState.moveHistory + move
        val updatedPosHistory = currentState.positionHistory + nextPos

        // Check for Game Over conditions
        val nextLegalMoves = nextPos.generateLegalMoves()
        val isCheck = nextPos.isKingInCheck(nextPos.activeColor)
        val status = when {
            nextLegalMoves.isEmpty() && isCheck -> GameStatus.CHECKMATE
            nextLegalMoves.isEmpty() && !isCheck -> GameStatus.STALEMATE
            nextPos.halfmoveClock >= 100 -> GameStatus.DRAW_FIFTY_MOVE
            nextPos.isInsufficientMaterial() -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            isThreefoldRepetition(updatedPosHistory) -> GameStatus.DRAW_REPETITION
            else -> GameStatus.IN_PROGRESS
        }

        // Haptic feedback
        if (status == GameStatus.CHECKMATE) {
            vibrate(120)
        } else if (move.isCapture || isCheck) {
            vibrate(35)
        } else {
            vibrate(18)
        }

        // Real Acoustic Chess Sounds
        if (currentState.isSoundEnabled) {
            when {
                status == GameStatus.CHECKMATE -> soundManager.playGameOverSound()
                isCheck -> soundManager.playCheckSound()
                move.isCastle -> soundManager.playCastleSound()
                move.isCapture -> soundManager.playCaptureSound()
                else -> soundManager.playMoveSound()
            }
        }

        _uiState.update {
            it.copy(
                position = nextPos,
                moveHistory = updatedHistory,
                positionHistory = updatedPosHistory,
                redoStack = emptyList(),
                status = status,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMove = move,
                engineArrowMove = null
            )
        }

        // Save game if finished
        if (status != GameStatus.IN_PROGRESS) {
            saveCompletedGame(status)
        }

        // Handle Puzzle progression
        if (currentState.gameMode == GameMode.TACTICAL_PUZZLE && currentState.activePuzzle != null) {
            handlePuzzleStep(move)
            return
        }

        // Trigger AI move if active & in PvE mode or Helper Bot mode
        if (status == GameStatus.IN_PROGRESS) {
            if (currentState.gameMode == GameMode.PLAYER_VS_AI && nextPos.activeColor != currentState.playerColor) {
                triggerAiMove(nextPos)
            } else if (currentState.gameMode == GameMode.HELPER_BOT && nextPos.activeColor == currentState.helperBotColor) {
                triggerHelperBotMove(nextPos)
            } else {
                updateAssistantEvaluation()
            }
        }
    }

    private fun handlePuzzleStep(playerMove: ChessMove) {
        val state = _uiState.value
        val puzzle = state.activePuzzle ?: return
        val solutionMoves = puzzle.solutionMovesSan.trim().split(Regex("\\s+"))

        val expectedSan = solutionMoves.getOrNull(state.puzzleMoveIndex)
        val playerSan = playerMove.san.ifEmpty { playerMove.uci }

        if (expectedSan == null || (!playerSan.startsWith(expectedSan.replace("+", "").replace("#", "")) && playerSan != expectedSan)) {
            _uiState.update { it.copy(puzzleMessage = "Incorrect move. Try again!") }
            viewModelScope.launch {
                delay(1200)
                undoMove()
                _uiState.update { it.copy(puzzleMessage = null) }
            }
            return
        }

        val nextMoveIdx = state.puzzleMoveIndex + 1
        if (nextMoveIdx >= solutionMoves.size) {
            // Puzzle Complete!
            _uiState.update {
                it.copy(
                    puzzleMessage = "Tactical Puzzle Solved! Brilliant!",
                    puzzleMoveIndex = nextMoveIdx
                )
            }
            viewModelScope.launch {
                repository.markPuzzleSolved(puzzle.id)
            }
        } else {
            // Play engine opponent reply from puzzle sequence
            _uiState.update {
                it.copy(
                    puzzleMessage = "Correct! Defending against response...",
                    puzzleMoveIndex = nextMoveIdx
                )
            }
            val opponentSan = solutionMoves[nextMoveIdx]
            viewModelScope.launch {
                delay(600)
                val legalMoves = _uiState.value.position.generateLegalMoves()
                val oppMove = legalMoves.find { it.san == opponentSan || it.san.replace("+", "") == opponentSan.replace("+", "") }
                if (oppMove != null) {
                    val posAfterOpp = _uiState.value.position.makeMove(oppMove)
                    _uiState.update {
                        it.copy(
                            position = posAfterOpp,
                            moveHistory = it.moveHistory + oppMove,
                            positionHistory = it.positionHistory + posAfterOpp,
                            lastMove = oppMove,
                            puzzleMoveIndex = nextMoveIdx + 1,
                            puzzleMessage = "Your turn. Find the continuation!"
                        )
                    }
                }
            }
        }
    }

    private fun triggerAiMove(currentPos: ChessPosition) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isEngineThinking = true) }
            val state = _uiState.value

            if (state.isExternalEngineRunning && oexEngineManager.isRunning) {
                // Use Stockfish 18 / External UCI Engine
                val oexResult = oexEngineManager.findBestMove(
                    fen = currentPos.toFen(),
                    depth = state.aiSearchDepth,
                    moveTimeMs = state.aiMoveTimeMs
                ) { scoreCp, mateIn ->
                    _uiState.update {
                        it.copy(
                            engineEvaluationCp = scoreCp,
                            engineMateIn = mateIn
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        isEngineThinking = false,
                        engineEvaluationCp = oexResult?.scoreCp ?: 0,
                        engineMateIn = oexResult?.mateIn
                    )
                }

                val bestMoveUci = oexResult?.bestMoveUci
                val matchingMove = bestMoveUci?.let { parseUciToLegalMove(it, currentPos) }
                if (matchingMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                    executeMove(matchingMove)
                    return@launch
                }
            }

            // Fallback or Primary: Built-in Grandmaster Alpha-Beta engine
            val eval = alphaBetaEngine.findBestMove(
                position = currentPos,
                maxDepth = state.aiSearchDepth,
                timeLimitMs = state.aiMoveTimeMs
            ) { progress ->
                _uiState.update {
                    it.copy(
                        engineEvaluationCp = progress.scoreCp,
                        engineMateIn = progress.mateIn
                    )
                }
            }

            _uiState.update {
                it.copy(
                    isEngineThinking = false,
                    engineEvaluationCp = eval.scoreCp,
                    engineMateIn = eval.mateIn
                )
            }

            if (eval.bestMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                executeMove(eval.bestMove)
            }
        }
    }

    private fun triggerHelperBotMove(currentPos: ChessPosition) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isEngineThinking = true) }
            val state = _uiState.value

            var calculatedMove: ChessMove? = null
            var calculatedScoreCp = 0
            var calculatedMateIn: Int? = null

            if (state.isExternalEngineRunning && oexEngineManager.isRunning) {
                // Calculate with external engine (Stockfish 18)
                val oexResult = oexEngineManager.findBestMove(
                    fen = currentPos.toFen(),
                    depth = state.aiSearchDepth,
                    moveTimeMs = state.aiMoveTimeMs
                ) { scoreCp, mateIn ->
                    _uiState.update {
                        it.copy(
                            engineEvaluationCp = scoreCp,
                            engineMateIn = mateIn
                        )
                    }
                }
                calculatedMove = oexResult?.bestMoveUci?.let { parseUciToLegalMove(it, currentPos) }
                calculatedScoreCp = oexResult?.scoreCp ?: 0
                calculatedMateIn = oexResult?.mateIn
            }

            if (calculatedMove == null) {
                // Fallback to built-in Alpha-Beta
                val eval = alphaBetaEngine.findBestMove(
                    position = currentPos,
                    maxDepth = state.aiSearchDepth,
                    timeLimitMs = state.aiMoveTimeMs
                )
                calculatedMove = eval.bestMove
                calculatedScoreCp = eval.scoreCp
                calculatedMateIn = eval.mateIn
            }

            // Draw the recommendation arrow on board
            _uiState.update {
                it.copy(
                    isEngineThinking = false,
                    engineArrowMove = calculatedMove,
                    engineEvaluationCp = calculatedScoreCp,
                    engineMateIn = calculatedMateIn
                )
            }

            // Helper bot plays automatically on its turn
            if (calculatedMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                delay(600) // Pacing so the selected arrow is noticed
                val executedMove = calculatedMove
                executeMove(executedMove)
                // Clear arrow after helper bot plays so opponent does not get any suggestions!
                _uiState.update { it.copy(engineArrowMove = null) }
            }
        }
    }

    fun updateAssistantEvaluation() {
        val state = _uiState.value
        // Do NOT calculate or show suggestions for the opponent
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) {
            _uiState.update { it.copy(engineArrowMove = null) }
            return
        }
        if (state.gameMode == GameMode.HELPER_BOT && state.position.activeColor != state.helperBotColor) {
            _uiState.update { it.copy(engineArrowMove = null) }
            return
        }
        if (!state.isAssistantMode && state.gameMode != GameMode.ANALYSIS) {
            _uiState.update { it.copy(engineArrowMove = null) }
            return
        }

        val pos = state.position
        viewModelScope.launch(Dispatchers.Default) {
            val currentState = _uiState.value
            if (currentState.gameMode == GameMode.HELPER_BOT && currentState.position.activeColor != currentState.helperBotColor) {
                _uiState.update { it.copy(engineArrowMove = null) }
                return@launch
            }
            if (currentState.gameMode == GameMode.PLAYER_VS_AI && currentState.position.activeColor != currentState.playerColor) {
                _uiState.update { it.copy(engineArrowMove = null) }
                return@launch
            }

            if (currentState.isExternalEngineRunning && oexEngineManager.isRunning) {
                val oexResult = oexEngineManager.findBestMove(
                    fen = pos.toFen(),
                    depth = 10,
                    moveTimeMs = 400
                )
                if (oexResult != null) {
                    val arrowMove = oexResult.bestMoveUci?.let { parseUciToLegalMove(it, pos) }
                    _uiState.update {
                        it.copy(
                            engineArrowMove = arrowMove,
                            engineEvaluationCp = oexResult.scoreCp,
                            engineMateIn = oexResult.mateIn
                        )
                    }
                    return@launch
                }
            }

            val eval = alphaBetaEngine.findBestMove(
                position = pos,
                maxDepth = 5,
                timeLimitMs = 400
            )
            _uiState.update {
                it.copy(
                    engineArrowMove = eval.bestMove,
                    engineEvaluationCp = eval.scoreCp,
                    engineMateIn = eval.mateIn
                )
            }
        }
    }

    private fun parseUciToLegalMove(uci: String, position: ChessPosition): ChessMove? {
        if (uci.length < 4) return null
        val fromSq = Square.fromAlgebraic(uci.substring(0, 2)) ?: return null
        val toSq = Square.fromAlgebraic(uci.substring(2, 4)) ?: return null
        val promoChar = if (uci.length >= 5) uci[4] else null
        val promoType = when (promoChar) {
            'q', 'Q' -> PieceType.QUEEN
            'r', 'R' -> PieceType.ROOK
            'b', 'B' -> PieceType.BISHOP
            'n', 'N' -> PieceType.KNIGHT
            else -> null
        }
        val legalMoves = position.generateLegalMoves()
        return legalMoves.find { it.from == fromSq && it.to == toSq && (promoType == null || it.promotion == promoType) }
    }

    fun undoMove() {
        val state = _uiState.value
        if (state.moveHistory.isEmpty() || state.isEngineThinking) return

        // In PvE mode, undo 2 moves (both AI and Player) if possible
        val movesToUndo = if (state.gameMode == GameMode.PLAYER_VS_AI && state.moveHistory.size >= 2) 2 else 1

        val newHistory = state.moveHistory.dropLast(movesToUndo)
        val newPosHistory = state.positionHistory.dropLast(movesToUndo)
        val undoneMoves = state.moveHistory.takeLast(movesToUndo)
        val targetPos = newPosHistory.last()

        _uiState.update {
            it.copy(
                position = targetPos,
                moveHistory = newHistory,
                positionHistory = newPosHistory,
                redoStack = undoneMoves + it.redoStack,
                status = GameStatus.IN_PROGRESS,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMove = newHistory.lastOrNull(),
                engineArrowMove = null,
                puzzleMessage = null
            )
        }
        updateAssistantEvaluation()
    }

    fun redoMove() {
        val state = _uiState.value
        if (state.redoStack.isEmpty() || state.isEngineThinking) return

        val move = state.redoStack.first()
        _uiState.update { it.copy(redoStack = it.redoStack.drop(1)) }
        executeMove(move)
    }

    fun resetGame(mode: GameMode = _uiState.value.gameMode, playerColor: PieceColor = PieceColor.WHITE) {
        aiJob?.cancel()
        gameStartTime = System.currentTimeMillis()
        val initPos = ChessPosition.initial()

        _uiState.update {
            it.copy(
                position = initPos,
                moveHistory = emptyList(),
                positionHistory = listOf(initPos),
                redoStack = emptyList(),
                status = GameStatus.IN_PROGRESS,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                engineArrowMove = null,
                isEngineThinking = false,
                engineEvaluationCp = 0,
                engineMateIn = null,
                gameMode = mode,
                playerColor = playerColor,
                boardOrientation = playerColor,
                promotionPending = null,
                activePuzzle = null,
                puzzleMessage = null
            )
        }

        if (mode == GameMode.PLAYER_VS_AI && playerColor == PieceColor.BLACK) {
            triggerAiMove(initPos)
        } else {
            updateAssistantEvaluation()
        }
    }

    fun startPuzzle(puzzle: PuzzleRecord) {
        aiJob?.cancel()
        val pos = ChessPosition.fromFen(puzzle.initialFen) ?: return
        val playerColor = if (puzzle.playerColor == "BLACK") PieceColor.BLACK else PieceColor.WHITE

        _uiState.update {
            it.copy(
                position = pos,
                moveHistory = emptyList(),
                positionHistory = listOf(pos),
                redoStack = emptyList(),
                status = GameStatus.IN_PROGRESS,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                engineArrowMove = null,
                isEngineThinking = false,
                gameMode = GameMode.TACTICAL_PUZZLE,
                boardOrientation = playerColor,
                playerColor = playerColor,
                activePuzzle = puzzle,
                puzzleMoveIndex = 0,
                puzzleMessage = puzzle.description
            )
        }
    }

    fun loadFen(fenString: String): Boolean {
        val pos = ChessPosition.fromFen(fenString) ?: return false
        aiJob?.cancel()
        _uiState.update {
            it.copy(
                position = pos,
                moveHistory = emptyList(),
                positionHistory = listOf(pos),
                redoStack = emptyList(),
                status = GameStatus.IN_PROGRESS,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                lastMove = null,
                engineArrowMove = null,
                gameMode = GameMode.ANALYSIS
            )
        }
        updateAssistantEvaluation()
        return true
    }

    fun toggle3DView() {
        _uiState.update { it.copy(is3DView = !it.is3DView) }
    }

    fun flipBoard() {
        _uiState.update { it.copy(boardOrientation = it.boardOrientation.opponent) }
    }

    fun toggleAssistant() {
        val newAssistant = !_uiState.value.isAssistantMode
        _uiState.update { it.copy(isAssistantMode = newAssistant) }
        if (newAssistant) {
            updateAssistantEvaluation()
        } else {
            _uiState.update { it.copy(engineArrowMove = null) }
        }
    }

    fun toggleSound() {
        val newSound = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = newSound) }
        soundManager.isSoundEnabled = newSound
    }

    fun toggleHaptic() {
        _uiState.update { it.copy(isHapticEnabled = !it.isHapticEnabled) }
    }

    fun setBoardTheme(theme: BoardTheme) {
        _uiState.update { it.copy(boardTheme = theme) }
    }

    fun setAiDepth(depth: Int) {
        _uiState.update { it.copy(aiSearchDepth = depth) }
    }

    fun deleteGameRecord(record: GameRecord) {
        viewModelScope.launch {
            repository.deleteGame(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun saveCompletedGame(finalStatus: GameStatus) {
        val state = _uiState.value
        val resultStr = when (finalStatus) {
            GameStatus.CHECKMATE -> if (state.position.activeColor == PieceColor.WHITE) "0-1" else "1-0"
            else -> "1/2-1/2"
        }
        val pgn = PgnUtils.exportToPgn(
            state = state,
            whitePlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.BLACK) "AI (Master)" else "Player",
            blackPlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.WHITE) "AI (Master)" else "Player"
        )
        val duration = (System.currentTimeMillis() - gameStartTime) / 1000

        viewModelScope.launch {
            repository.saveGame(
                GameRecord(
                    eventName = when (state.gameMode) {
                        GameMode.PLAYER_VS_AI -> "Player vs Bot"
                        GameMode.HELPER_BOT -> "Helper Bot"
                        GameMode.PLAYER_VS_PLAYER -> "Pass & Play"
                        GameMode.ANALYSIS -> "Analysis Board"
                        GameMode.TACTICAL_PUZZLE -> "Puzzle"
                    },
                    whitePlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.BLACK) "AI (Master)" else "Player",
                    blackPlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.WHITE) "AI (Master)" else "Player",
                    result = resultStr,
                    moveCount = state.moveHistory.size,
                    pgn = pgn,
                    finalFen = state.position.toFen(),
                    gameMode = state.gameMode.name,
                    durationSeconds = duration
                )
            )
        }
    }

    private fun isThreefoldRepetition(posHistory: List<ChessPosition>): Boolean {
        if (posHistory.size < 6) return false
        val currentFenParts = posHistory.last().toFen().split(" ").take(4).joinToString(" ")
        var count = 0
        for (pos in posHistory) {
            val pastFenParts = pos.toFen().split(" ").take(4).joinToString(" ")
            if (currentFenParts == pastFenParts) {
                count++
                if (count >= 3) return true
            }
        }
        return false
    }

    private fun vibrate(durationMs: Long) {
        if (!_uiState.value.isHapticEnabled) return
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }
}
