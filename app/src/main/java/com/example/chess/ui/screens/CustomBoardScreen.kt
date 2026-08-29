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
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.chess.model.*
import com.example.chess.ui.ChessViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomBoardScreen(viewModel: ChessViewModel, onBack: () -> Unit, onAnalyze: () -> Unit, modifier: Modifier = Modifier) {
    var position by remember { mutableStateOf(ChessPosition.initial()) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedType by remember { mutableStateOf(PieceType.QUEEN) }
    var activeColor by remember { mutableStateOf(PieceColor.WHITE) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsState()
    val depth = state.aiSearchDepth.coerceIn(1, 30)
    val fen = position.copy(activeColor = activeColor).toFen()

    fun updateSquare(square: Square) {
        val next = position.board.toMutableList()
        val current = next[square.index]
        next[square.index] = if (current?.color == selectedColor && current.type == selectedType) null else ChessPiece(selectedType, selectedColor)
        position = position.copy(board = next)
        error = null
        copied = false
    }

    fun resetBoard() {
        position = ChessPosition.initial()
        activeColor = PieceColor.WHITE
        selectedColor = PieceColor.WHITE
        selectedType = PieceType.QUEEN
        error = null
        copied = false
    }

    fun clearBoard() {
        position = position.copy(board = MutableList(64) { null })
        error = null
        copied = false
    }

    fun loadIntoGame() {
        val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
        if (whiteKings != 1 || blackKings != 1) {
            error = "Exactly one White King and one Black King are required."
            return
        }
        val target = position.copy(activeColor = activeColor, castlingRights = CastlingRights.NONE, enPassantTarget = null, halfmoveClock = 0, fullmoveNumber = 1)
        if (viewModel.loadFen(target.toFen())) onAnalyze() else error = "The position could not be loaded into the chess engine."
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Custom Board") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = ::clearBoard) { Icon(Icons.Default.Delete, "Clear board") }
                    TextButton(onClick = ::resetBoard) { Text("Reset") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Position Editor", style = MaterialTheme.typography.titleMedium)
                    Text("Choose color + piece, then tap squares to place or replace.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Box(Modifier.fillMaxWidth().widthIn(max = 560.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp))) { EditorBoard(position, ::updateSquare) }

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
                    Text("Engine depth: $depth", style = MaterialTheme.typography.labelMedium)
                    Slider(value = depth.toFloat(), onValueChange = { viewModel.setAiDepth(it.toInt().coerceIn(1, 30)) }, valueRange = 1f..30f, steps = 28)
                }
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2)
                    IconButton(onClick = { clipboard.setText(AnnotatedString(fen)); copied = true }) { Icon(Icons.Default.ContentCopy, "Copy FEN") }
                }
            }
            if (copied) Text("FEN copied", style = MaterialTheme.typography.labelSmall)

            error?.let { msg -> Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) { Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall) } }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = ::loadIntoGame, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Text("Play Position") }
                Button(onClick = ::loadIntoGame, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Psychology, null); Spacer(Modifier.width(6.dp)); Text("Analyze") }
            }
        }
    }
}

private fun pieceName(type: PieceType): String = when (type) {
    PieceType.KING -> "King"
    PieceType.QUEEN -> "Queen"
    PieceType.ROOK -> "Rook"
    PieceType.BISHOP -> "Bishop"
    PieceType.KNIGHT -> "Knight"
    PieceType.PAWN -> "Pawn"
}

@Composable
private fun EditorBoard(position: ChessPosition, onSquareClick: (Square) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        for (rank in 7 downTo 0) {
            Row(Modifier.weight(1f)) {
                for (file in 0..7) {
                    val square = Square(file, rank)
                    val piece = position.pieceAt(square)
                    Box(Modifier.weight(1f).fillMaxHeight().background(if (square.isLightSquare) Color(0xFFF0D9B5) else Color(0xFFB58863)).clickable { onSquareClick(square) }, contentAlignment = Alignment.Center) {
                        if (piece != null) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF303030)), contentAlignment = Alignment.Center) {
                                Text(piece.unicodeSymbol, style = MaterialTheme.typography.headlineLarge, color = if (piece.color == PieceColor.WHITE) Color(0xFF202020) else Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
