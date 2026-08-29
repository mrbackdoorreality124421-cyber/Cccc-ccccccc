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
import com.example.chess.engine.*
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
    private var gameStartTime = System.currentTimeMillis()

    init {
        scanOexEngines()
        viewModelScope.launch { repository.seedDefaultPuzzlesIfEmpty() }
        viewModelScope.launch {
            installState.collect { state ->
                if (state is com.example.chess.engine.EngineInstallState.Ready) {
                    _discoveredOexEngines.value = oexEngineManager.discoverEngines()
                    selectEngine(state.engine)
                }
            }
        }
        startStockfishAutoSetup(forceRedownload = false)
        updateAssistantEvaluation()
    }

    fun startStockfishAutoSetup(forceRedownload: Boolean = false) { stockfishDownloader.startAutoSetup(forceRedownload = forceRedownload) }
    fun clearEngineError() { _uiState.update { it.copy(engineErrorMessage = null) } }

    fun scanOexEngines() {
        viewModelScope.launch {
            val engines = oexEngineManager.discoverEngines()
            _discoveredOexEngines.value = engines
            val stockfish = engines.find { it.isStockfish } ?: engines.firstOrNull()
            if (stockfish != null && _uiState.value.selectedOexEngineId == null) selectEngine(stockfish)
        }
    }

    fun selectEngine(engine: OexEngineInfo?) {
        viewModelScope.launch {
            if (engine == null) {
                oexEngineManager.stopEngine()
                _uiState.update { it.copy(selectedOexEngineId = null, activeEngineName = "No Engine Selected", isStockfishActive = false, isExternalEngineRunning = false, engineErrorMessage = "No chess engine selected. Please select or import an engine to play.") }
            } else {
                val started = oexEngineManager.startEngine(engine)
                _uiState.update { it.copy(selectedOexEngineId = engine.id, activeEngineName = if (started) engine.name else "${engine.name} (Start Failed)", isStockfishActive = engine.isStockfish, isExternalEngineRunning = started, engineErrorMessage = if (!started) "Failed to start ${engine.name}. Binary may lack execution permission." else null) }
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
            } else onResult(false, result.exceptionOrNull()?.message ?: "Sorry, engine not detected.")
        }
    }

    fun autoDetectStockfishFromDownloads(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = oexEngineManager.autoScanAndImportFromDownloads()
            if (result.isSuccess) {
                val engine = result.getOrNull()
                _discoveredOexEngines.value = oexEngineManager.discoverEngines()
                if (engine != null) selectEngine(engine)
                onResult(true, "Stockfish (.tar) auto-detected and extracted: ${engine?.name}")
            } else onResult(false, result.exceptionOrNull()?.message ?: "No Stockfish file found directly in Downloads.")
        }
    }

    fun startPlayerVsBot(color: PieceColor = PieceColor.WHITE) { resetGame(GameMode.PLAYER_VS_AI, color) }
    fun startHelperBot(botColor: PieceColor = PieceColor.WHITE, autoPlay: Boolean = true) { resetGame(GameMode.HELPER_BOT, botColor, botColor, autoPlay) }

    fun toggleHelperAutoPlay() {
        val newAutoPlay = !_uiState.value.helperBotAutoPlay
        _uiState.update { it.copy(helperBotAutoPlay = newAutoPlay) }
        if (newAutoPlay && _uiState.value.gameMode == GameMode.HELPER_BOT && _uiState.value.position.activeColor == _uiState.value.helperBotColor) {
            triggerHelperBotMove(_uiState.value.position)
        }
    }

    fun triggerManualHelperStep() { if (_uiState.value.gameMode == GameMode.HELPER_BOT) triggerHelperBotMove(_uiState.value.position) }

    fun onSquareClicked(square: Square) {
        val state = _uiState.value
        if (state.isGameOver || state.isEngineThinking) return
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) return
        val clickedPiece = state.position.pieceAt(square)
        val matchingMove = state.legalMovesForSelected.find { it.to == square }
        if (matchingMove != null) {
            if (matchingMove.pieceMoved == PieceType.PAWN && (matchingMove.to.rank == 7 || matchingMove.to.rank == 0) && matchingMove.promotion == null) {
                _uiState.update { it.copy(promotionPending = PromotionPending(matchingMove.from, matchingMove.to, PieceType.PAWN, matchingMove.isCapture, matchingMove.capturedPiece)) }
                return
            }
            executeMove(matchingMove)
            return
        }
        if (clickedPiece != null && clickedPiece.color == state.position.activeColor) {
            val pieceLegalMoves = state.position.generateLegalMoves().filter { it.from == square }
            _uiState.update { it.copy(selectedSquare = square, legalMovesForSelected = pieceLegalMoves) }
            vibrate(15)
            return
        }
        _uiState.update { it.copy(selectedSquare = null, legalMovesForSelected = emptyList()) }
    }

    fun onPromotionPieceSelected(pieceType: PieceType) {
        val pending = _uiState.value.promotionPending ?: return
        _uiState.update { it.copy(promotionPending = null) }
        executeMove(ChessMove(pending.from, pending.to, pending.pieceMoved, pieceType, pending.isCapture, pending.capturedPiece))
    }

    fun onPromotionDismissed() { _uiState.update { it.copy(promotionPending = null, selectedSquare = null, legalMovesForSelected = emptyList()) } }

    private fun executeMove(move: ChessMove) {
        val currentState = _uiState.value
        val nextPos = currentState.position.makeMove(move)
        val updatedHistory = currentState.moveHistory + move
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
        if (status == GameStatus.CHECKMATE) vibrate(120) else if (move.isCapture || isCheck) vibrate(35) else vibrate(18)
        if (currentState.isSoundEnabled) when {
            status == GameStatus.CHECKMATE -> soundManager.playGameOverSound()
            isCheck -> soundManager.playCheckSound()
            move.isCastle -> soundManager.playCastleSound()
            move.isCapture -> soundManager.playCaptureSound()
            else -> soundManager.playMoveSound()
        }
        _uiState.update { it.copy(position = nextPos, moveHistory = updatedHistory, positionHistory = updatedPosHistory, redoStack = emptyList(), status = status, selectedSquare = null, legalMovesForSelected = emptyList(), lastMove = move, engineArrowMove = null) }
        if (status != GameStatus.IN_PROGRESS) saveCompletedGame(status)
        if (currentState.gameMode == GameMode.TACTICAL_PUZZLE && currentState.activePuzzle != null) { handlePuzzleStep(move); return }
        if (status == GameStatus.IN_PROGRESS) {
            if (currentState.gameMode == GameMode.PLAYER_VS_AI && nextPos.activeColor != currentState.playerColor) {
                triggerAiMove(nextPos)
            } else if (currentState.gameMode == GameMode.HELPER_BOT && currentState.isFenGame) {
                _uiState.update { it.copy(helperBotColor = nextPos.activeColor) }
                triggerHelperBotMove(nextPos)
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
            viewModelScope.launch { delay(1200); undoMove(); _uiState.update { it.copy(puzzleMessage = null) } }
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
                    _uiState.update { it.copy(position = posAfterOpp, moveHistory = it.moveHistory + oppMove, positionHistory = it.positionHistory + posAfterOpp, lastMove = oppMove, puzzleMoveIndex = nextMoveIdx + 1, puzzleMessage = "Your turn. Find the continuation!") }
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
            var isEngineActive = state.isExternalEngineRunning && oexEngineManager.isRunning
            if (!isEngineActive && state.selectedOexEngineId != null) {
                val selectedEngine = _discoveredOexEngines.value.find { it.id == state.selectedOexEngineId }
                if (selectedEngine != null) { isEngineActive = oexEngineManager.startEngine(selectedEngine); _uiState.update { it.copy(isExternalEngineRunning = isEngineActive) } }
            }
            if (!isEngineActive || !oexEngineManager.isRunning) { _uiState.update { it.copy(isEngineThinking = false, engineErrorMessage = "No Chess Engine Active!\n\nPlease select or import an external engine (e.g. Stockfish) from Engine Discovery to play.") }; return@launch }
            val oexResult = oexEngineManager.findBestMove(currentPos.toFen(), movesUci, state.aiSearchDepth, state.aiMoveTimeMs) { scoreCp, mateIn, _, _ -> _uiState.update { it.copy(engineEvaluationCp = scoreCp, engineMateIn = mateIn) } }
            _uiState.update { it.copy(isEngineThinking = false, engineEvaluationCp = oexResult?.scoreCp ?: 0, engineMateIn = oexResult?.mateIn) }
            val matchingMove = oexResult?.bestMoveUci?.let { parseUciToLegalMove(it, currentPos) }
            if (matchingMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                executeMove(matchingMove)
                _uiState.update { it.copy(engineArrowMove = matchingMove) }
            } else if (matchingMove == null && _uiState.value.status == GameStatus.IN_PROGRESS) {
                _uiState.update { it.copy(engineErrorMessage = "Engine Error: External engine failed to calculate a move. Check engine logs or binary permissions.") }
            }
        }
    }

    private fun triggerHelperBotMove(currentPos: ChessPosition) {
        aiJob?.cancel()
        aiJob = viewModelScope.launch {
            _uiState.update { it.copy(isEngineThinking = true, engineErrorMessage = null) }
            val state = _uiState.value
            val movesUci = state.moveHistory.map { it.uci }
            var isEngineActive = state.isExternalEngineRunning && oexEngineManager.isRunning
            if (!isEngineActive && state.selectedOexEngineId != null) {
                val selectedEngine = _discoveredOexEngines.value.find { it.id == state.selectedOexEngineId }
                if (selectedEngine != null) { isEngineActive = oexEngineManager.startEngine(selectedEngine); _uiState.update { it.copy(isExternalEngineRunning = isEngineActive) } }
            }
            if (!isEngineActive || !oexEngineManager.isRunning) { _uiState.update { it.copy(isEngineThinking = false, engineErrorMessage = "No Chess Engine Active!\n\nPlease select or import an external engine for Helper Bot.") }; return@launch }
            val oexResult = oexEngineManager.findBestMove(currentPos.toFen(), movesUci, state.aiSearchDepth, state.aiMoveTimeMs) { scoreCp, mateIn, _, _ -> _uiState.update { it.copy(engineEvaluationCp = scoreCp, engineMateIn = mateIn) } }
            val calculatedMove = oexResult?.bestMoveUci?.let { parseUciToLegalMove(it, currentPos) }
            _uiState.update { it.copy(isEngineThinking = false, engineArrowMove = calculatedMove, engineEvaluationCp = oexResult?.scoreCp ?: 0, engineMateIn = oexResult?.mateIn, engineErrorMessage = if (calculatedMove == null) "Engine Error: Could not calculate helper move." else null) }
            if (calculatedMove != null && _uiState.value.status == GameStatus.IN_PROGRESS && _uiState.value.helperBotAutoPlay) {
                delay(600)
                executeMove(calculatedMove)
                _uiState.update { it.copy(engineArrowMove = calculatedMove) }
            }
        }
    }

    fun updateAssistantEvaluation() {
        val state = _uiState.value
        if (state.gameMode == GameMode.PLAYER_VS_AI && state.position.activeColor != state.playerColor) { _uiState.update { it.copy(engineArrowMove = null) }; return }
        if (state.gameMode == GameMode.HELPER_BOT && state.isFenGame) return
        if (state.gameMode == GameMode.HELPER_BOT && state.position.activeColor != state.helperBotColor) { _uiState.update { it.copy(engineArrowMove = null) }; return }
        if (!state.isAssistantMode && state.gameMode != GameMode.ANALYSIS) { _uiState.update { it.copy(engineArrowMove = null) }; return }
        val pos = state.position
        val movesUci = state.moveHistory.map { it.uci }
        viewModelScope.launch(Dispatchers.Default) {
            val currentState = _uiState.value
            if ((currentState.gameMode == GameMode.HELPER_BOT && !currentState.isFenGame && currentState.position.activeColor != currentState.helperBotColor) || (currentState.gameMode == GameMode.PLAYER_VS_AI && currentState.position.activeColor != currentState.playerColor)) {
                _uiState.update { it.copy(engineArrowMove = null) }
                return@launch
            }
            if (!currentState.isExternalEngineRunning || !oexEngineManager.isRunning) { _uiState.update { it.copy(engineArrowMove = null) }; return@launch }
            val oexResult = oexEngineManager.findBestMove(pos.toFen(), movesUci, 10, 400)
            if (oexResult != null) _uiState.update { it.copy(engineArrowMove = oexResult.bestMoveUci?.let { u -> parseUciToLegalMove(u, pos) }, engineEvaluationCp = oexResult.scoreCp, engineMateIn = oexResult.mateIn) }
        }
    }

    private fun parseUciToLegalMove(uci: String, position: ChessPosition): ChessMove? {
        if (uci.length < 4) return null
        val fromSq = Square.fromAlgebraic(uci.substring(0, 2)) ?: return null
        val toSq = Square.fromAlgebraic(uci.substring(2, 4)) ?: return null
        val promoType = if (uci.length >= 5) when (uci[4]) { 'q', 'Q' -> PieceType.QUEEN; 'r', 'R' -> PieceType.ROOK; 'b', 'B' -> PieceType.BISHOP; 'n', 'N' -> PieceType.KNIGHT; else -> null } else null
        return position.generateLegalMoves().find { it.from == fromSq && it.to == toSq && (promoType == null || it.promotion == promoType) }
    }

    fun undoMove() {
        val state = _uiState.value
        if (state.moveHistory.isEmpty() || state.isEngineThinking) return
        val movesToUndo = if (state.gameMode == GameMode.PLAYER_VS_AI && state.moveHistory.size >= 2) 2 else 1
        val newHistory = state.moveHistory.dropLast(movesToUndo)
        val newPosHistory = state.positionHistory.dropLast(movesToUndo)
        val undoneMoves = state.moveHistory.takeLast(movesToUndo)
        val targetPos = newPosHistory.last()
        _uiState.update { it.copy(position = targetPos, moveHistory = newHistory, positionHistory = newPosHistory, redoStack = undoneMoves + it.redoStack, status = GameStatus.IN_PROGRESS, selectedSquare = null, legalMovesForSelected = emptyList(), lastMove = newHistory.lastOrNull(), engineArrowMove = null, puzzleMessage = null) }
        updateAssistantEvaluation()
    }

    fun redoMove() {
        val state = _uiState.value
        if (state.redoStack.isEmpty() || state.isEngineThinking) return
        val move = state.redoStack.first()
        val nextPos = state.position.makeMove(move)
        val newHistory = state.moveHistory + move
        val newPosHistory = state.positionHistory + nextPos
        val nextLegalMoves = nextPos.generateLegalMoves()
        val isCheck = nextPos.isKingInCheck(nextPos.activeColor)
        val status = when {
            nextLegalMoves.isEmpty() && isCheck -> GameStatus.CHECKMATE
            nextLegalMoves.isEmpty() -> GameStatus.STALEMATE
            nextPos.halfmoveClock >= 100 -> GameStatus.DRAW_FIFTY_MOVE
            nextPos.isInsufficientMaterial() -> GameStatus.DRAW_INSUFFICIENT_MATERIAL
            isThreefoldRepetition(newPosHistory) -> GameStatus.DRAW_REPETITION
            else -> GameStatus.IN_PROGRESS
        }
        _uiState.update { it.copy(position = nextPos, moveHistory = newHistory, positionHistory = newPosHistory, redoStack = it.redoStack.drop(1), status = status, selectedSquare = null, legalMovesForSelected = emptyList(), lastMove = move, engineArrowMove = null, puzzleMessage = null) }
        updateAssistantEvaluation()
    }

    fun resetGame(mode: GameMode = _uiState.value.gameMode, playerColor: PieceColor = PieceColor.WHITE, helperColor: PieceColor = playerColor, helperAutoPlay: Boolean = true) {
        aiJob?.cancel()
        oexEngineManager.sendNewGame()
        gameStartTime = System.currentTimeMillis()
        val initPos = ChessPosition.initial()
        val isHelper = mode == GameMode.HELPER_BOT
        val actualHelperColor = if (isHelper) helperColor else _uiState.value.helperBotColor
        val actualHelperAutoPlay = if (isHelper) helperAutoPlay else _uiState.value.helperBotAutoPlay
        _uiState.update { it.copy(position = initPos, moveHistory = emptyList(), positionHistory = listOf(initPos), redoStack = emptyList(), status = GameStatus.IN_PROGRESS, selectedSquare = null, legalMovesForSelected = emptyList(), lastMove = null, engineArrowMove = null, isEngineThinking = false, engineEvaluationCp = 0, engineMateIn = null, gameMode = mode, playerColor = playerColor, helperBotColor = actualHelperColor, helperBotAutoPlay = actualHelperAutoPlay, boardOrientation = playerColor, isAssistantMode = if (isHelper) true else it.isAssistantMode, isFenGame = false, promotionPending = null, activePuzzle = null, puzzleMessage = null) }
        if (mode == GameMode.PLAYER_VS_AI && playerColor == PieceColor.BLACK) triggerAiMove(initPos) else if (mode == GameMode.HELPER_BOT && actualHelperColor == PieceColor.WHITE) triggerHelperBotMove(initPos) else updateAssistantEvaluation()
    }

    fun startPuzzle(puzzle: PuzzleRecord) {
        aiJob?.cancel()
        val pos = ChessPosition.fromFen(puzzle.initialFen) ?: return
        val playerColor = if (puzzle.playerColor == "BLACK") PieceColor.BLACK else PieceColor.WHITE
        _uiState.update { it.copy(position = pos, moveHistory = emptyList(), positionHistory = listOf(pos), redoStack = emptyList(), status = GameStatus.IN_PROGRESS, selectedSquare = null, legalMovesForSelected = emptyList(), lastMove = null, engineArrowMove = null, isEngineThinking = false, gameMode = GameMode.TACTICAL_PUZZLE, boardOrientation = playerColor, playerColor = playerColor, activePuzzle = puzzle, puzzleMoveIndex = 0, puzzleMessage = puzzle.description, isFenGame = false) }
    }

    fun startCustomGame(fenString: String, mode: GameMode, playerColor: PieceColor): Boolean {
        val pos = ChessPosition.fromFen(fenString) ?: return false
        aiJob?.cancel()
        oexEngineManager.sendNewGame()
        gameStartTime = System.currentTimeMillis()
        val helperMode = mode == GameMode.HELPER_BOT
        val helperColor = pos.activeColor
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
                engineEvaluationCp = 0,
                engineMateIn = null,
                gameMode = mode,
                playerColor = playerColor,
                helperBotColor = helperColor,
                helperBotAutoPlay = false,
                boardOrientation = playerColor,
                isAssistantMode = true,
                isFenGame = true,
                promotionPending = null,
                activePuzzle = null,
                puzzleMessage = null
            )
        }
        when {
            mode == GameMode.PLAYER_VS_AI && pos.activeColor != playerColor -> triggerAiMove(pos)
            helperMode -> triggerHelperBotMove(pos)
            else -> _uiState.update { it.copy(engineArrowMove = null) }
        }
        return true
    }

    fun loadFen(fenString: String): Boolean {
        val pos = ChessPosition.fromFen(fenString) ?: return false
        aiJob?.cancel()
        _uiState.update { it.copy(position = pos, moveHistory = emptyList(), positionHistory = listOf(pos), redoStack = emptyList(), status = GameStatus.IN_PROGRESS, selectedSquare = null, legalMovesForSelected = emptyList(), lastMove = null, engineArrowMove = null, gameMode = GameMode.ANALYSIS, isFenGame = false) }
        updateAssistantEvaluation()
        return true
    }

    fun toggle3DView() { _uiState.update { it.copy(is3DView = !it.is3DView) } }
    fun flipBoard() { _uiState.update { it.copy(boardOrientation = it.boardOrientation.opponent) } }
    fun toggleAssistant() { val newAssistant = !_uiState.value.isAssistantMode; _uiState.update { it.copy(isAssistantMode = newAssistant) }; if (newAssistant) updateAssistantEvaluation() else _uiState.update { it.copy(engineArrowMove = null) } }
    fun toggleSound() { val newSound = !_uiState.value.isSoundEnabled; _uiState.update { it.copy(isSoundEnabled = newSound) }; soundManager.isSoundEnabled = newSound }
    fun toggleHaptic() { _uiState.update { it.copy(isHapticEnabled = !it.isHapticEnabled) } }
    fun setBoardTheme(theme: BoardTheme) { _uiState.update { it.copy(boardTheme = theme) } }
    fun setAiDepth(depth: Int) { _uiState.update { it.copy(aiSearchDepth = depth.coerceIn(1, 30)) } }
    fun deleteGameRecord(record: GameRecord) { viewModelScope.launch { repository.deleteGame(record) } }
    fun clearAllHistory() { viewModelScope.launch { repository.clearHistory() } }

    private fun saveCompletedGame(finalStatus: GameStatus) {
        val state = _uiState.value
        val resultStr = when (finalStatus) { GameStatus.CHECKMATE -> if (state.position.activeColor == PieceColor.WHITE) "0-1" else "1-0"; else -> "1/2-1/2" }
        val pgn = PgnUtils.exportToPgn(state, if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.BLACK) "AI (Master)" else "Player", if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.WHITE) "AI (Master)" else "Player")
        val duration = (System.currentTimeMillis() - gameStartTime) / 1000
        viewModelScope.launch { repository.saveGame(GameRecord(eventName = when (state.gameMode) { GameMode.PLAYER_VS_AI -> "Player vs Bot"; GameMode.HELPER_BOT -> "Helper Bot"; GameMode.PLAYER_VS_PLAYER -> "Pass & Play"; GameMode.ANALYSIS -> "Analysis Board"; GameMode.TACTICAL_PUZZLE -> "Puzzle" }, whitePlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.BLACK) "AI (Master)" else "Player", blackPlayer = if (state.gameMode == GameMode.PLAYER_VS_AI && state.playerColor == PieceColor.WHITE) "AI (Master)" else "Player", result = resultStr, moveCount = state.moveHistory.size, pgn = pgn, finalFen = state.position.toFen(), gameMode = state.gameMode.name, durationSeconds = duration)) }
    }

    private fun isThreefoldRepetition(posHistory: List<ChessPosition>): Boolean {
        if (posHistory.size < 6) return false
        val currentFenParts = posHistory.last().toFen().split(" ").take(4).joinToString(" ")
        var count = 0
        for (pos in posHistory) if (currentFenParts == pos.toFen().split(" ").take(4).joinToString(" ") && ++count >= 3) return true
        return false
    }

    private fun vibrate(durationMs: Long) {
        if (!_uiState.value.isHapticEnabled) return
        val context = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") it.vibrate(durationMs) }
    }
}
