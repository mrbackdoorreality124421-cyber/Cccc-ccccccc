package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
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

    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsState()
    val depth = state.aiSearchDepth.coerceIn(1, 30)
    val fen = position.copy(activeColor = activeColor).toFen()
    val botSession = remember { CustomPositionBotSession(context) }

    DisposableEffect(Unit) { onDispose { botSession.stop() } }

    fun placePiece(square: Square) {
        if (playMode || thinking) return
        val next = position.board.toMutableList()
        val current = next[square.index]
        next[square.index] = if (current != null && current.color == selectedColor && current.type == selectedType) null else ChessPiece(selectedType, selectedColor)
        position = position.copy(board = next)
        selectedSquare = null; legalTargets = emptyList(); copied = false; message = null
    }

    fun playSquare(square: Square) {
        if (!playMode || thinking || position.activeColor != playerColor) return
        val piece = position.pieceAt(square)
        val target = legalTargets.firstOrNull { it.to == square }
        if (target != null) {
            position = position.makeMove(target)
            activeColor = position.activeColor
            moveHistory = moveHistory + target
            selectedSquare = null; legalTargets = emptyList(); message = null
            if (position.activeColor != playerColor) {
                thinking = true
                scope.launch {
                    val result = botSession.bestMove(position, moveHistory, depth, 1200)
                    result.onSuccess {
                        position = position.makeMove(it.move)
                        activeColor = position.activeColor
                        moveHistory = moveHistory + it.move
                        message = it.mateIn?.let { mate -> "Stockfish • Mate in $mate" } ?: "Stockfish • ${"%.2f".format(it.scoreCp / 100.0)}"
                    }.onFailure { message = it.message ?: "Stockfish failed to calculate a move." }
                    thinking = false
                }
            }
            return
        }
        if (piece != null && piece.color == playerColor && piece.color == position.activeColor) {
            selectedSquare = square
            legalTargets = position.generateLegalMoves().filter { it.from == square }
        } else {
            selectedSquare = null; legalTargets = emptyList()
        }
    }

    fun startStockfishGame() {
        val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
        if (whiteKings != 1 || blackKings != 1) { message = "Exactly one White King and one Black King are required."; return }
        playerColor = activeColor
        moveHistory = emptyList()
        selectedSquare = null; legalTargets = emptyList(); playMode = true; thinking = false
        message = "Play vs Stockfish • You are ${playerColor.name.lowercase().replaceFirstChar { it.uppercase() }}"
        if (position.activeColor != playerColor) {
            thinking = true
            scope.launch {
                val result = botSession.bestMove(position, moveHistory, depth, 1200)
                result.onSuccess { position = position.makeMove(it.move); activeColor = position.activeColor; moveHistory = moveHistory + it.move }
                    .onFailure { message = it.message ?: "Stockfish failed to start." }
                thinking = false
            }
        }
    }

    fun resetBoard() {
        position = ChessPosition.initial(); activeColor = PieceColor.WHITE; selectedColor = PieceColor.WHITE; selectedType = PieceType.QUEEN
        selectedSquare = null; legalTargets = emptyList(); playMode = false; moveHistory = emptyList(); thinking = false; message = null; copied = false
        botSession.stop()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (playMode) "Custom Position • Stockfish" else "Custom Board") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { position = position.copy(board = MutableList(64) { null }); selectedSquare = null; legalTargets = emptyList(); message = null }) { Icon(Icons.Default.Delete, "Clear board") }
                    TextButton(onClick = ::resetBoard) { Text("Reset") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(if (playMode) "Stockfish Match" else "Position Editor", style = MaterialTheme.typography.titleMedium)
                    Text(if (playMode) "Your moves are played normally; Stockfish responds automatically." else "Choose color + piece, then tap a square to place or remove it.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Box(Modifier.fillMaxWidth().widthIn(max = 560.dp).aspectRatio(1f)) {
                EditorBoard(position, legalTargets, selectedSquare, if (playMode) ::playSquare else ::placePiece)
            }

            if (!playMode) {
                Text("Piece color", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedColor == PieceColor.WHITE, onClick = { selectedColor = PieceColor.WHITE }, label = { Text("WHITE") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = selectedColor == PieceColor.BLACK, onClick = { selectedColor = PieceColor.BLACK }, label = { Text("BLACK") }, modifier = Modifier.weight(1f))
                }
                Text("Piece type", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    PieceType.values().forEach { type -> item { FilterChip(selected = selectedType == type, onClick = { selectedType = type }, label = { Text(pieceName(type)) }) } }
                }
                Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Side to move", Modifier.weight(1f))
                            FilterChip(selected = activeColor == PieceColor.WHITE, onClick = { activeColor = PieceColor.WHITE }, label = { Text("White") })
                            Spacer(Modifier.width(6.dp))
                            FilterChip(selected = activeColor == PieceColor.BLACK, onClick = { activeColor = PieceColor.BLACK }, label = { Text("Black") })
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Stockfish depth: $depth", style = MaterialTheme.typography.labelMedium)
                        Slider(value = depth.toFloat(), onValueChange = { viewModel.setAiDepth(it.toInt()) }, valueRange = 1f..30f, steps = 28)
                    }
                }
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2)
                    IconButton(onClick = { clipboard.setText(AnnotatedString(fen)); copied = true }) { Icon(Icons.Default.ContentCopy, "Copy FEN") }
                }
            }
            if (copied) Text("FEN copied", style = MaterialTheme.typography.labelSmall)
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (it.contains("failed", true) || it.contains("error", true) || it.contains("required", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }

            if (!playMode) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { if (viewModel.loadFen(fen)) onAnalyze() }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Analyze") }
                    Button(onClick = ::startStockfishGame, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play vs Stockfish")
                    }
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
                        if (piece != null) {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF202020)), contentAlignment = Alignment.Center) {
                                Text(piece.unicodeSymbol, style = MaterialTheme.typography.headlineLarge, color = if (piece.color == PieceColor.WHITE) Color(0xFF1B1B1B) else Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
