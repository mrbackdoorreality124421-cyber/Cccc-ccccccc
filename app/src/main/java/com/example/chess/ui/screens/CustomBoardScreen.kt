package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.chess.model.*
import com.example.chess.ui.ChessViewModel
import com.example.chess.ui.CustomPositionBotSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBoardScreen(viewModel: ChessViewModel, onBack: () -> Unit, onAnalyze: () -> Unit, modifier: Modifier = Modifier) {
    var position by remember { mutableStateOf(ChessPosition.initial()) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedType by remember { mutableStateOf(PieceType.QUEEN) }
    var activeColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedSquare by remember { mutableStateOf<Square?>(null) }
    var legalTargets by remember { mutableStateOf<List<ChessMove>>(emptyList()) }
    var playMode by remember { mutableStateOf(false) }
    var playerColor by remember { mutableStateOf(PieceColor.WHITE) }
    var moveHistory by remember { mutableStateOf<List<ChessMove>>(emptyList()) }
    var thinking by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val botSession = remember(context) { CustomPositionBotSession(context) }
    val state by viewModel.uiState.collectAsState()
    val depth = state.aiSearchDepth.coerceIn(1, 30)
    val fen = position.copy(activeColor = activeColor).toFen()

    DisposableEffect(botSession) { onDispose { botSession.stop() } }

    fun resetBoard() {
        botSession.stop()
        position = ChessPosition.initial()
        activeColor = PieceColor.WHITE
        selectedColor = PieceColor.WHITE
        selectedType = PieceType.QUEEN
        selectedSquare = null
        legalTargets = emptyList()
        playMode = false
        moveHistory = emptyList()
        thinking = false
        message = null
        copied = false
    }

    fun clearBoard() {
        botSession.stop()
        position = position.copy(board = MutableList(64) { null })
        selectedSquare = null
        legalTargets = emptyList()
        playMode = false
        thinking = false
        moveHistory = emptyList()
        message = null
        copied = false
    }

    fun editSquare(square: Square) {
        val next = position.board.toMutableList()
        val current = next[square.index]
        next[square.index] = if (current?.color == selectedColor && current.type == selectedType) null else ChessPiece(selectedType, selectedColor)
        position = position.copy(board = next)
        selectedSquare = null
        legalTargets = emptyList()
        message = null
        copied = false
    }

    fun playSquare(square: Square) {
        if (!playMode || thinking || activeColor != playerColor) return
        val clickedPiece = position.pieceAt(square)
        val matchingMove = legalTargets.firstOrNull { it.to == square }
        if (matchingMove != null) {
            val nextPosition = position.makeMove(matchingMove)
            val newHistory = moveHistory + matchingMove
            position = nextPosition
            activeColor = nextPosition.activeColor
            moveHistory = newHistory
            selectedSquare = null
            legalTargets = emptyList()
            message = "Stockfish is thinking…"
            scope.launch {
                thinking = true
                botSession.bestMove(nextPosition, newHistory, depth, state.aiMoveTimeMs.coerceAtLeast(250))
                    .onSuccess { engineMove ->
                        val botPosition = nextPosition.makeMove(engineMove.move)
                        position = botPosition
                        activeColor = botPosition.activeColor
                        moveHistory = newHistory + engineMove.move
                        message = if (engineMove.mateIn != null) "Stockfish: Mate in ${engineMove.mateIn}" else "Stockfish: ${if (engineMove.scoreCp >= 0) "+" else ""}${engineMove.scoreCp} cp"
                    }
                    .onFailure { message = it.message ?: "Stockfish could not calculate a move." }
                thinking = false
            }
            return
        }
        if (clickedPiece != null && clickedPiece.color == activeColor && clickedPiece.color == playerColor) {
            selectedSquare = square
            legalTargets = position.generateLegalMoves().filter { it.from == square }
        } else {
            selectedSquare = null
            legalTargets = emptyList()
        }
    }

    fun onSquare(square: Square) { if (playMode) playSquare(square) else editSquare(square) }

    fun loadIntoAnalysis() {
        val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
        if (whiteKings != 1 || blackKings != 1) {
            message = "Exactly one White King and one Black King are required."
            return
        }
        val target = position.copy(activeColor = activeColor, castlingRights = CastlingRights.NONE, enPassantTarget = null, halfmoveClock = 0, fullmoveNumber = 1)
        if (viewModel.loadFen(target.toFen())) { position = target; onAnalyze() } else message = "The position could not be loaded into the chess engine."
    }

    fun startStockfishGame() {
        val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
        if (whiteKings != 1 || blackKings != 1) {
            message = "Exactly one White King and one Black King are required."
            return
        }
        val target = position.copy(activeColor = activeColor, castlingRights = CastlingRights.NONE, enPassantTarget = null, halfmoveClock = 0, fullmoveNumber = 1)
        scope.launch {
            thinking = true
            botSession.start().onSuccess { engineName ->
                position = target
                playerColor = target.activeColor
                activeColor = target.activeColor
                playMode = true
                selectedSquare = null
                legalTargets = emptyList()
                moveHistory = emptyList()
                message = "Playing vs $engineName • You are ${if (playerColor == PieceColor.WHITE) "White" else "Black"}."
            }.onFailure { message = it.message ?: "Stockfish could not start." }
            thinking = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Custom Board") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }, actions = { IconButton(onClick = ::clearBoard) { Icon(Icons.Default.Delete, "Clear board") }; TextButton(onClick = ::resetBoard) { Text("Reset") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(if (playMode) "Custom Position • Stockfish Match" else "Position Editor", style = MaterialTheme.typography.titleMedium)
                    Text(if (playMode) "Tap a piece, then its highlighted destination square." else "Choose color + piece, then tap squares to place or replace.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Box(Modifier.fillMaxWidth().widthIn(max = 560.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp))) { EditorBoard(position, legalTargets, selectedSquare, ::onSquare) }

            if (!playMode) {
                Text("Piece color", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedColor == PieceColor.WHITE, onClick = { selectedColor = PieceColor.WHITE }, label = { Text("WHITE") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = selectedColor == PieceColor.BLACK, onClick = { selectedColor = PieceColor.BLACK }, label = { Text("BLACK") }, modifier = Modifier.weight(1f))
                }
                Text("Piece type", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { PieceType.values().forEach { type -> item { FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(pieceName(type)) }) } } }
            }

            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Side to move", Modifier.weight(1f))
                        FilterChip(selected = activeColor == PieceColor.WHITE, onClick = { if (!playMode) activeColor = PieceColor.WHITE }, enabled = !playMode, label = { Text("White") })
                        Spacer(Modifier.width(6.dp))
                        FilterChip(selected = activeColor == PieceColor.BLACK, onClick = { if (!playMode) activeColor = PieceColor.BLACK }, enabled = !playMode, label = { Text("Black") })
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Engine depth: $depth", style = MaterialTheme.typography.labelMedium)
                    Slider(value = depth.toFloat(), onValueChange = { viewModel.setAiDepth(it.toInt().coerceIn(1, 30)) }, valueRange = 1f..30f, steps = 28)
                }
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(fen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2); IconButton(onClick = { clipboard.setText(AnnotatedString(fen)); copied = true }) { Icon(Icons.Default.ContentCopy, "Copy FEN") } }
            }
            if (copied) Text("FEN copied", style = MaterialTheme.typography.labelSmall)
            if (thinking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            message?.let { msg -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) { Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall) } }

            if (!playMode) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = ::loadIntoAnalysis, enabled = !thinking, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Analyze") }
                    Button(onClick = ::startStockfishGame, enabled = !thinking, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play vs Stockfish") }
                }
            } else {
                OutlinedButton(onClick = { if (!thinking) { playMode = false; botSession.stop(); message = "Position editing enabled." } }, enabled = !thinking, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Stop & Edit Position") }
            }
        }
    }
}

private fun pieceName(type: PieceType): String = when (type) {
    PieceType.KING -> "King"; PieceType.QUEEN -> "Queen"; PieceType.ROOK -> "Rook"; PieceType.BISHOP -> "Bishop"; PieceType.KNIGHT -> "Knight"; PieceType.PAWN -> "Pawn"
}

@Composable
private fun EditorBoard(position: ChessPosition, targets: List<ChessMove>, selected: Square?, onSquareClick: (Square) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        for (rank in 7 downTo 0) {
            Row(Modifier.weight(1f)) {
                for (file in 0..7) {
                    val square = Square(file, rank)
                    val piece = position.pieceAt(square)
                    val highlighted = targets.any { it.to == square } || selected == square
                    Box(Modifier.weight(1f).fillMaxHeight().background(if (highlighted) Color(0xFFC8A951) else if (square.isLightSquare) Color(0xFFF0D9B5) else Color(0xFFB58863)).clickable { onSquareClick(square) }, contentAlignment = Alignment.Center) {
                        if (piece != null) Box(Modifier.size(46.dp).clip(CircleShape).background(if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF202020)), contentAlignment = Alignment.Center) { Text(piece.unicodeSymbol, style = MaterialTheme.typography.headlineLarge, color = if (piece.color == PieceColor.WHITE) Color(0xFF1B1B1B) else Color.White) }
                    }
                }
            }
        }
    }
}
