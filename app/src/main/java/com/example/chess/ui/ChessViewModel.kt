package com.example.chess.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.audio.ChessSoundManager
import com.example.chess.audio.HapticType
import com.example.chess.data.ChessDatabase
import com.example.chess.data.ChessRepository
import com.example.chess.data.GameRecord
import com.example.chess.data.PuzzleRecord
import com.example.chess.engine.*
import com.example.chess.model.*
import com.example.chess.utils.PgnUtils
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
    private val oexEngineManager = OexEngineManager(application)
    private val soundManager = ChessSoundManager()
    val stockfishDownloader = StockfishDownloader(application, oexEngineManager)
    val installState = stockfishDownloader.installState
    val deviceSpecs = DeviceSpecsDetector.detect(application)

    private val _uiState = MutableStateFlow(ChessGameState())
    val uiState: StateFlow<ChessGameState> = _uiState.asStateFlow()
    private val _discoveredOexEngines = MutableStateFlow<List<OexEngineInfo>>(emptyList())
    val discoveredOexEngines: StateFlow<List<OexEngineInfo>> = _discoveredOexEngines.asStateFlow()
    val allGameHistory = repository.allGameRecords
    val allPuzzles = repository.allPuzzles
    private var aiJob: Job? = null
    private var assistantJob: Job? = null
    private var gameStartTime = System.currentTimeMillis()

    init {
        scanOexEngines()
        viewModelScope.launch { repository.seedDefaultPuzzlesIfEmpty() }
        viewModelScope.launch {
            installState.collect { state ->
                if (state is EngineInstallState.Ready) {
                    _discoveredOexEngines.value = oexEngineManager.discoverEngines()
                    selectEngine(state.engine)
                }
            }
        }
        
        // Auto-detect and launch Bundled Native Stockfish 18 if present
        viewModelScope.launch {
            val bundledEngine = oexEngineManager.findBundledStockfish()
            if (bundledEngine != null) {
                Log.i("ChessViewModel", "Auto-starting bundled Stockfish 18...")
                val started = oexEngineManager.startEngine(bundledEngine)
                if (started) {
                    _uiState.update { 
                        it.copy(
                            isExternalEngineRunning = true,
                            isStockfishActive = true,
                            activeEngineName = bundledEngine.name,
                            selectedOexEngineId = bundledEngine.id,
                            engineErrorMessage = null
                        ) 
                    }
                    _discoveredOexEngines.value = oexEngineManager.discoverEngines()
                    Log.i("ChessViewModel", "Bundled Stockfish 18 started successfully!")
                } else {
                    _uiState.update { 
                        it.copy(engineErrorMessage = "Built-in engine found but failed to start.") 
                    }
                }
            } else {
                Log.w("ChessViewModel", "No bundled Stockfish found. Checking download/OEX fallback.")
                startStockfishAutoSetup(forceRedownload = false)
            }
        }
    }

    fun startStockfishAutoSetup(forceRedownload: Boolean = false) { 
        if (oexEngineManager.findBundledStockfish() != null && !forceRedownload) {
            Log.i("ChessViewModel", "Bundled engine available. Skipping auto-download.")
            return
        }
        stockfishDownloader.startAutoSetup(forceRedownload = forceRedownload) 
    }
    
    fun clearEngineError() { 
        _uiState.update { it.copy(engineErrorMessage = null) } 
    }

    fun scanOexEngines() {
        viewModelScope.launch {
            val engines = oexEngineManager.discoverEngines()
            _discoveredOexEngines.value = engines
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
                        activeEngineName = "No Engine Selected", 
                        isStockfishActive = false, 
                        isExternalEngineRunning = false, 
                        engineErrorMessage = "No chess engine selected. Please select or import an engine to play."
                    ) 
                }
            } else {
                val started = oexEngineManager.startEngine(engine)
                _uiState.update { 
                    it.copy(
                        selectedOexEngineId = engine.id, 
                        activeEngineName = if (started) engine.name else "${engine.name} (Start Failed)", 
                        isStockfishActive = engine.isStockfish, 
                        isExternalEngineRunning = started, 
                        engineErrorMessage = if (!started) "Failed to start ${engine.name}. Engine may lack execute permission." else null
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
                _discoveredOexEngines.value = oexEngineManager.discoverEngines()
                if (engine != null) selectEngine(engine)
                onResult(true, "Stockfish engine unpacked & loaded successfully: ${engine?.name}")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Engine import failed.")
            }
        }
    }

    fun autoDetectStockfishFromDownloads(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = oexEngineManager.autoScanAndImportFromDownloads()
            if (result.isSuccess) {
                val engine = result.getOrNull()
                _discoveredOexEngines.value = oexEngineManager.discoverEngines()
                if (engine != null) selectEngine(engine)
                onResult(true, "Stockfish archive auto-detected: ${engine?.name}")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "No Stockfish file found in Downloads.")
            }
        }
    }

    fun setDifficultyLevel(level: Int) {
        _uiState.update { it.copy(difficultyLevel = level.coerceIn(1, 5)) }
    }

    fun startPlayerVsBot(color: PieceColor = PieceColor.WHITE) { 
        viewModelScope.launch {
            ensureEngineRunning()
            resetGame(GameMode.PLAYER_VS_AI, color)
        }
    }

    fun startHelperBot(botColor: PieceColor = PieceColor.WHITE, autoPlay: Boolean = true) { 
        viewModelScope.launch {
            ensureEngineRunning()
            resetGame(GameMode.HELPER_BOT, botColor, botColor, autoPlay)
        }
    }

    fun startPassAndPlay() {
        resetGame(GameMode.PLAYER_VS_PLAYER, PieceColor.WHITE)
    }

    fun startAnalysis() {
        resetGame(GameMode.ANALYSIS, PieceColor.WHITE)
    }

    private suspend fun ensureEngineRunning(): Boolean {
        if (oexEngineManager.isRunning) return true
        val state = _uiState.value
        val engines = if (_discoveredOexEngines.value.isEmpty()) oexEngineManager.discoverEngines() else _discoveredOexEngines.value
        _discoveredOexEngines.value = engines
        val targetEngine = engines.find { it.id == state.selectedOexEngineId } 
            ?: engines.find { it.isStockfish } 
            ?: engines.firstOrNull()
            
        if (targetEngine != null) {
            val started = oexEngineManager.startEngine(targetEngine)
            _uiState.update { 
                it.copy(
                    isExternalEngineRunning = started,
                    activeEngineName = targetEngine.name,
                    selectedOexEngineId = targetEngine.id
                ) 
            }
            return started
        }
        return false
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

    fun requestFreshHint() {
        val state = _uiState.value
        if (state.status != GameStatus.IN_PROGRESS) return
        if (state.gameMode == GameMode.HELPER_BOT && state.position.activeColor != state.helperBotColor) return
        _uiState.update { it.copy(engineArrowMove = null, isEngineThinking = true) }
        if (state.gameMode == GameMode.HELPER_BOT) {
            triggerHelperBotMove(state.position)
        } else {
            updateAssistantEvaluation()
        }
    }

    fun onSquareClicked(square: Square) {
        val state = _uiState.value
        if (state.isGameOver || state.isEngineThinking) return
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) return

        // 1. If user clicked the "from" square of the active helper arrow, auto-select that piece
        if (state.engineArrowMove != null && square == state.engineArrowMove.from && state.selectedSquare != square) {
            val legalMoves = state.position.generateLegalMoves().filter { it.from == square }
            _uiState.update {
                it.copy(selectedSquare = square, legalMovesForSelected = legalMoves)
            }
            ChessSoundManager.performHaptic(getApplication(), HapticType.LIGHT)
            return
        }

        // 2. If user clicked the "to" square of the arrow after selecting "from", play correct feedback
        val matchingMove = state.legalMovesForSelected.find { it.to == square }
        if (matchingMove != null) {
            val isSuggestedMove = state.engineArrowMove?.from == matchingMove.from && state.engineArrowMove?.to == matchingMove.to
            if (isSuggestedMove && state.isSoundEnabled) {
                soundManager.playCorrectMove()
            }

            if (matchingMove.pieceMoved == PieceType.PAWN && (matchingMove.to.rank == 7 || matchingMove.to.rank == 0) && matchingMove.promotion == null) {
                _uiState.update { 
                    it.copy(promotionPending = PromotionPending(matchingMove.from, matchingMove.to, PieceType.PAWN, matchingMove.isCapture, matchingMove.capturedPiece)) 
                }
                return
            }
            executeMove(matchingMove)
            return
        }

        val clickedPiece = state.position.pieceAt(square)
        if (clickedPiece != null && clickedPiece.color == state.position.activeColor) {
            val pieceLegalMoves = state.position.generateLegalMoves().filter { it.from == square }
            _uiState.update { 
                it.copy(selectedSquare = square, legalMovesForSelected = pieceLegalMoves) 
            }
            ChessSoundManager.performHaptic(getApplication(), HapticType.LIGHT)
            return
        }
        _uiState.update { it.copy(selectedSquare = null, legalMovesForSelected = emptyList()) }
    }

    fun onPieceDrop(from: Square, to: Square) {
        val state = _uiState.value
        if (state.isGameOver || state.isEngineThinking) return
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) return

        val legalMoves = state.position.generateLegalMoves().filter { it.from == from }
        val matchingMove = legalMoves.find { it.to == to }
        if (matchingMove != null) {
            if (matchingMove.pieceMoved == PieceType.PAWN && (matchingMove.to.rank == 7 || matchingMove.to.rank == 0) && matchingMove.promotion == null) {
                _uiState.update { 
                    it.copy(promotionPending = PromotionPending(matchingMove.from, matchingMove.to, PieceType.PAWN, matchingMove.isCapture, matchingMove.capturedPiece)) 
                }
                return
            }
            executeMove(matchingMove)
        } else {
            if (state.isSoundEnabled) {
                soundManager.playIllegal()
            }
        }
    }

    fun setPieceStyle(style: PieceStyle) {
        _uiState.update { it.copy(pieceStyle = style) }
    }

    fun onPromotionPieceSelected(pieceType: PieceType) {
        val pending = _uiState.value.promotionPending ?: return
        _uiState.update { it.copy(promotionPending = null) }
        val move = ChessMove(
            from = pending.from, 
            to = pending.to, 
            pieceMoved = pending.pieceMoved, 
            promotion = pieceType, 
            isCapture = pending.isCapture, 
            capturedPiece = pending.capturedPiece
        )
        executeMove(move)
    }

    fun onPromotionDismissed() { 
        _uiState.update { 
            it.copy(promotionPending = null, selectedSquare = null, legalMovesForSelected = emptyList()) 
        } 
    }

    private fun executeMove(move: ChessMove) {
        val currentState = _uiState.value
        val legalMoves = currentState.position.generateLegalMoves()
        val isLegal = legalMoves.any { 
            it.from == move.from && it.to == move.to && (move.promotion == null || it.promotion == move.promotion) 
        }

        if (!isLegal) {
            Log.e("ChessViewModel", "Illegal move rejected: ${move.uci}")
            _uiState.update { it.copy(engineErrorMessage = "Illegal move rejected: ${move.uci}") }
            return
        }

        val actualMove = legalMoves.first { 
            it.from == move.from && it.to == move.to && (move.promotion == null || it.promotion == move.promotion) 
        }

        val nextPos = currentState.position.makeMove(actualMove)
        val updatedHistory = currentState.moveHistory + actualMove
        val updatedPosHistory = currentState.positionHistory + nextPos
        val nextLegalMoves = nextPos.generateLegalMoves()
        val isCheck = nextPos.isKingInCheck(nextPos.activeColor)

        val status = when {
            nextLegalMoves.isEmpty() && isCheck -> GameStatus.CHECKMATE
            nextLegalMoves.isEmpty() -> GameStatus.STALEMATE
            nextPos.halfmoveClock >= 100 -> GameStatus.DRAW_FIFTY_MOVE
            nextPos.isInsufficientMaterial() -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            isThreefoldRepetition(updatedPosHistory) -> GameStatus.DRAW_REPETITION
            else -> GameStatus.IN_PROGRESS
        }

        if (status == GameStatus.CHECKMATE) {
            ChessSoundManager.performHaptic(getApplication(), HapticType.CHECKMATE)
        } else if (isCheck) {
            ChessSoundManager.performHaptic(getApplication(), HapticType.CHECK)
        } else if (actualMove.isCapture) {
            ChessSoundManager.performHaptic(getApplication(), HapticType.HEAVY)
        } else {
            ChessSoundManager.performHaptic(getApplication(), HapticType.MEDIUM)
        }

        if (currentState.isSoundEnabled) {
            when {
                status == GameStatus.CHECKMATE -> soundManager.playCheckmate()
                actualMove.promotion != null -> soundManager.playPromotion()
                isCheck -> soundManager.playCheck()
                actualMove.isCastle -> soundManager.playCastle()
                actualMove.isCapture -> soundManager.playCapture()
                else -> soundManager.playMove()
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
                lastMove = actualMove,
                engineArrowMove = null
            )
        }

        if (status != GameStatus.IN_PROGRESS) {
            saveCompletedGame(status)
        }

        if (currentState.gameMode == GameMode.TACTICAL_PUZZLE && currentState.activePuzzle != null) { 
            handlePuzzleStep(actualMove)
            return 
        }

        if (status == GameStatus.IN_PROGRESS) {
            if (currentState.gameMode == GameMode.PLAYER_VS_AI && nextPos.activeColor != currentState.playerColor) {
                triggerAiMove(nextPos)
            } else if (currentState.gameMode == GameMode.HELPER_BOT) {
                if (nextPos.activeColor == currentState.helperBotColor) {
                    triggerHelperBotMove(nextPos)
                } else {
                    _uiState.update { it.copy(engineArrowMove = null) }
                }
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
            _uiState.update { it.copy(puzzleMessage = "Tactical Puzzle Solved! Brilliant!", puzzleMoveIndex = nextMoveIdx) }
            viewModelScope.launch { repository.markPuzzleSolved(puzzle.id) }
        } else {
            _uiState.update { it.copy(puzzleMessage = "Correct! Defending against response...", puzzleMoveIndex = nextMoveIdx) }
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
            _uiState.update { it.copy(isEngineThinking = true, engineErrorMessage = null) }

            val state = _uiState.value
            val movesUci = state.moveHistory.map { it.uci }

            // Ensure engine is running
            val isEngineActive = ensureEngineRunning()
            if (!isEngineActive || !oexEngineManager.isRunning) {
                _uiState.update { 
                    it.copy(
                        isEngineThinking = false, 
                        engineErrorMessage = "Stockfish engine not running. Please start Stockfish in Engine Settings."
                    ) 
                }
                return@launch
            }

            // === MAX POWER SEARCH ALLOCATION ===
            val thinkTime = when (state.difficultyLevel) {
                1 -> 500L      // Beginner
                2 -> 1500L     // Easy
                3 -> 3000L     // Medium
                4 -> 5000L     // Hard
                5 -> 10000L    // Master (MAX POWER)
                else -> 5000L
            }

            val searchDepth = when (state.difficultyLevel) {
                1 -> 8
                2 -> 12
                3 -> 18
                4 -> 25
                5 -> 30        // MAX depth
                else -> 30
            }

            val oexResult = oexEngineManager.findBestMove(
                fen = currentPos.toFen(),
                movesUci = movesUci,
                depth = searchDepth,
                moveTimeMs = thinkTime.toInt()
            ) { scoreCp, mateIn, depth, pv ->
                _uiState.update { 
                    it.copy(
                        engineEvaluationCp = scoreCp,
                        engineMateIn = mateIn,
                        engineCurrentDepth = depth
                    ) 
                }
            }

            _uiState.update { 
                it.copy(
                    isEngineThinking = false,
                    engineEvaluationCp = oexResult?.scoreCp ?: 0,
                    engineMateIn = oexResult?.mateIn,
                    engineCurrentDepth = oexResult?.depth ?: 0
                ) 
            }

            val matchingMove = oexResult?.bestMoveUci?.let { 
                parseUciToLegalMove(it, currentPos) 
            }

            if (matchingMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                executeMove(matchingMove)
                _uiState.update { it.copy(engineArrowMove = matchingMove) }
            } else if (matchingMove == null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                _uiState.update { 
                    it.copy(engineErrorMessage = "Engine calculation returned no move. Check Engine Settings.") 
                }
            }
        }
    }

    private fun triggerHelperBotMove(currentPos: ChessPosition) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isEngineThinking = true, engineErrorMessage = null) }
            val state = _uiState.value
            val movesUci = state.moveHistory.map { it.uci }

            val isEngineActive = ensureEngineRunning()
            if (!isEngineActive || !oexEngineManager.isRunning) {
                _uiState.update { 
                    it.copy(
                        isEngineThinking = false,
                        engineErrorMessage = "Stockfish engine not running."
                    ) 
                }
                return@launch
            }

            val thinkTime = when (state.difficultyLevel) {
                1 -> 500L
                2 -> 1500L
                3 -> 3000L
                4 -> 5000L
                5 -> 8000L
                else -> 4000L
            }

            val searchDepth = when (state.difficultyLevel) {
                1 -> 10
                2 -> 14
                3 -> 20
                4 -> 25
                5 -> 30
                else -> 25
            }

            val oexResult = oexEngineManager.findBestMove(
                fen = currentPos.toFen(),
                movesUci = movesUci,
                depth = searchDepth,
                moveTimeMs = thinkTime.toInt()
            ) { scoreCp, mateIn, depth, _ ->
                _uiState.update {
                    it.copy(
                        engineEvaluationCp = scoreCp,
                        engineMateIn = mateIn,
                        engineCurrentDepth = depth
                    )
                }
            }

            val calculatedMove = oexResult?.bestMoveUci?.let { parseUciToLegalMove(it, currentPos) }

            if (calculatedMove != null && state.isSoundEnabled && !_uiState.value.helperBotAutoPlay) {
                soundManager.playHintReady()
            }

            _uiState.update {
                it.copy(
                    isEngineThinking = false,
                    engineArrowMove = calculatedMove,
                    engineEvaluationCp = oexResult?.scoreCp ?: 0,
                    engineMateIn = oexResult?.mateIn,
                    engineCurrentDepth = oexResult?.depth ?: 0,
                    engineErrorMessage = if (calculatedMove == null) "Could not calculate helper move." else null
                )
            }

            if (calculatedMove != null && _uiState.value.status == GameStatus.IN_PROGRESS && _uiState.value.helperBotAutoPlay) {
                delay(600)
                executeMove(calculatedMove)
                _uiState.update { it.copy(engineArrowMove = calculatedMove) }
            }
        }
    }

    fun updateAssistantEvaluation() {
        val state = _uiState.value
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) { 
            _uiState.update { it.copy(engineArrowMove = null) }
            return 
        }
        if (state.gameMode == GameMode.HELPER_BOT && state.position.activeColor != state.helperBotColor) { 
            _uiState.update { it.copy(engineArrowMove = null) }
            return 
        }
        if (!state.isAssistantMode && state.gameMode != GameMode.ANALYSIS && state.gameMode != GameMode.HELPER_BOT) { 
            _uiState.update { it.copy(engineArrowMove = null) }
            return 
        }

        assistantJob?.cancel()
        val pos = state.position
        val movesUci = state.moveHistory.map { it.uci }

        assistantJob = viewModelScope.launch(Dispatchers.IO) {
            if (!oexEngineManager.isRunning) return@launch
            val res = oexEngineManager.findBestMove(
                fen = pos.toFen(),
                movesUci = movesUci,
                depth = 15,
                moveTimeMs = 1200
            ) { scoreCp, mateIn, depth, _ ->
                _uiState.update { 
                    it.copy(
                        engineEvaluationCp = scoreCp,
                        engineMateIn = mateIn,
                        engineCurrentDepth = depth
                    ) 
                }
            }
            val bestMove = res?.bestMoveUci?.let { parseUciToLegalMove(it, pos) }
            if (bestMove != null) {
                _uiState.update { 
                    it.copy(
                        engineArrowMove = bestMove,
                        engineEvaluationCp = res.scoreCp,
                        engineMateIn = res.mateIn,
                        engineCurrentDepth = res.depth
                    ) 
                }
            }
        }
    }

    private fun parseUciToLegalMove(uci: String, position: ChessPosition): ChessMove? {
        val trimmed = uci.trim()
        if (trimmed.length < 4) return null

        val fromSq = Square.fromAlgebraic(trimmed.substring(0, 2)) ?: return null
        val toSq = Square.fromAlgebraic(trimmed.substring(2, 4)) ?: return null

        val promoType = if (trimmed.length >= 5) {
            when (trimmed[4].lowercaseChar()) {
                'q' -> PieceType.QUEEN
                'r' -> PieceType.ROOK
                'b' -> PieceType.BISHOP
                'n' -> PieceType.KNIGHT
                else -> null
            }
        } else null

        val legalMoves = position.generateLegalMoves()
        return legalMoves.find { 
            it.from == fromSq && it.to == toSq && (promoType == null || it.promotion == promoType)
        }
    }

    fun undoMove() {
        val state = _uiState.value
        if (state.moveHistory.isEmpty() || state.isEngineThinking) return
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
        executeMove(move)
    }

    fun resetGame(
        mode: GameMode = _uiState.value.gameMode,
        playerColor: PieceColor = PieceColor.WHITE,
        helperColor: PieceColor = playerColor,
        helperAutoPlay: Boolean = true
    ) {
        aiJob?.cancel()
        assistantJob?.cancel()
        oexEngineManager.sendNewGame()
        gameStartTime = System.currentTimeMillis()

        val initPos = ChessPosition.initial()
        val isHelper = mode == GameMode.HELPER_BOT
        val actualHelperColor = if (isHelper) helperColor else playerColor
        val actualHelperAutoPlay = if (isHelper) helperAutoPlay else true

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
                engineCurrentDepth = 0,
                gameMode = mode,
                playerColor = playerColor,
                helperBotColor = actualHelperColor,
                helperBotAutoPlay = actualHelperAutoPlay,
                boardOrientation = playerColor,
                isAssistantMode = if (isHelper) true else it.isAssistantMode,
                isFenGame = false,
                promotionPending = null,
                activePuzzle = null,
                puzzleMessage = null
            )
        }

        if (mode == GameMode.PLAYER_VS_AI && playerColor == PieceColor.BLACK) {
            viewModelScope.launch {
                delay(500)
                triggerAiMove(initPos)
            }
        } else if (mode == GameMode.HELPER_BOT && actualHelperColor == PieceColor.WHITE) {
            triggerHelperBotMove(initPos)
        } else {
            updateAssistantEvaluation()
        }
    }

    fun startPuzzle(puzzle: PuzzleRecord) {
        aiJob?.cancel()
        assistantJob?.cancel()
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
                puzzleMessage = puzzle.description,
                isFenGame = false
            )
        }
    }

    fun startCustomGame(
        fenString: String,
        mode: GameMode,
        playerColor: PieceColor,
        helperColor: PieceColor = playerColor,
        helperAutoPlay: Boolean = true
    ): Boolean {
        val position = ChessPosition.fromFen(fenString) ?: return false
        aiJob?.cancel()
        assistantJob?.cancel()
        oexEngineManager.sendNewGame()
        gameStartTime = System.currentTimeMillis()
        
        val actualHelperColor = if (mode == GameMode.HELPER_BOT) helperColor else playerColor
        val actualHelperAutoPlay = if (mode == GameMode.HELPER_BOT) helperAutoPlay else false
        
        _uiState.update { 
            it.copy(
                position = position,
                moveHistory = emptyList(), 
                positionHistory = listOf(position), 
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
                helperBotColor = actualHelperColor, 
                helperBotAutoPlay = actualHelperAutoPlay, 
                boardOrientation = playerColor, 
                isFenGame = true, 
                isAssistantMode = if (mode == GameMode.HELPER_BOT) true else it.isAssistantMode,
                promotionPending = null, 
                activePuzzle = null, 
                puzzleMessage = null
            ) 
        }
        
        if (mode == GameMode.PLAYER_VS_AI && position.activeColor != playerColor) {
            triggerAiMove(position)
        } else if (mode == GameMode.HELPER_BOT && position.activeColor == actualHelperColor) {
            triggerHelperBotMove(position)
        } else {
            updateAssistantEvaluation()
        }
        return true
    }

    fun loadFen(fenString: String): Boolean {
        val pos = ChessPosition.fromFen(fenString) ?: return false
        aiJob?.cancel()
        assistantJob?.cancel()
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
                gameMode = GameMode.ANALYSIS,
                isFenGame = false
            )
        }
        updateAssistantEvaluation()
        return true
    }

    fun toggle3DView() { _uiState.update { it.copy(is3DView = !it.is3DView) } }
    fun flipBoard() { _uiState.update { it.copy(boardOrientation = it.boardOrientation.opponent) } }
    fun toggleAssistant() { 
        val newAssistant = !_uiState.value.isAssistantMode
        _uiState.update { it.copy(isAssistantMode = newAssistant) }
        if (newAssistant) updateAssistantEvaluation() else _uiState.update { it.copy(engineArrowMove = null) } 
    }
    fun toggleSound() { 
        val newSound = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = newSound) }
        soundManager.isSoundEnabled = newSound 
    }
    fun toggleHaptic() { _uiState.update { it.copy(isHapticEnabled = !it.isHapticEnabled) } }
    fun setBoardTheme(theme: BoardTheme) { _uiState.update { it.copy(boardTheme = theme) } }
    fun setAiDepth(depth: Int) { _uiState.update { it.copy(aiSearchDepth = depth.coerceIn(1, 30)) } }
    fun deleteGameRecord(record: GameRecord) { viewModelScope.launch { repository.deleteGame(record) } }
    fun clearAllHistory() { viewModelScope.launch { repository.clearHistory() } }

    private fun saveCompletedGame(finalStatus: GameStatus) {
        val state = _uiState.value
        val resultStr = when (finalStatus) { 
            GameStatus.CHECKMATE -> if (state.position.activeColor == PieceColor.WHITE) "0-1" else "1-0"
            else -> "1/2-1/2" 
        }
        val pgn = PgnUtils.exportToPgn(
            state, 
            if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.BLACK) "Stockfish 18 (Master)" else "Player", 
            if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.WHITE) "Stockfish 18 (Master)" else "Player"
        )
        val duration = (System.currentTimeMillis() - gameStartTime) / 1000
        val eventName = when (state.gameMode) { 
            GameMode.PLAYER_VS_AI -> "Player vs Bot"
            GameMode.HELPER_BOT -> "Helper Bot"
            GameMode.PLAYER_VS_PLAYER -> "Pass & Play"
            GameMode.ANALYSIS -> "Analysis Board"
            GameMode.TACTICAL_PUZZLE -> "Tactical Puzzle"
        }
        viewModelScope.launch { 
            repository.saveGame(
                GameRecord(
                    eventName = eventName, 
                    whitePlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.BLACK) "Stockfish 18" else "Player", 
                    blackPlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.WHITE) "Stockfish 18" else "Player", 
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
            if (currentFenParts == pos.toFen().split(" ").take(4).joinToString(" ") && ++count >= 3) return true
        }
        return false
    }

    private fun vibrate(durationMs: Long) {
        if (!_uiState.value.isHapticEnabled) return
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let { 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") it.vibrate(durationMs)
            }
        }
    }
}
