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
fun CustomBoardScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    var position by remember { mutableStateOf(ChessPosition.initial()) }
    var selectedColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedType by remember { mutableStateOf(PieceType.QUEEN) }
    var activeColor by remember { mutableStateOf(PieceColor.WHITE) }
    var error by remember { mutableStateOf<String?>(null) }
    var placementMode by remember { mutableStateOf(true) }
    var copied by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsState()
    val depth = state.aiSearchDepth
    val fen = position.copy(activeColor = activeColor).toFen()

    fun updateSquare(square: Square) {
        val next = position.board.toMutableList()
        val index = square.index
        next[index] = if (next[index]?.color == selectedColor && next[index]?.type == selectedType) null
        else ChessPiece(selectedType, selectedColor)
        position = position.copy(board = next)
        error = null
        copied = false
    }

    fun clearBoard() {
        position = position.copy(board = MutableList(64) { null })
        error = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Custom Board") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { clearBoard() }) { Icon(Icons.Default.Delete, "Clear board") }
                    TextButton(onClick = { position = ChessPosition.initial(); activeColor = PieceColor.WHITE; error = null }) { Text("Reset") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Position Editor", style = MaterialTheme.typography.titleMedium)
                        Text("Select a piece, then tap any square", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (placementMode) "EDIT" else "READY", style = MaterialTheme.typography.labelMedium)
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SimpleEditorBoard(position, onSquareClick = ::updateSquare)
            }

            Text("Piece", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                item { ColorChip("White", selectedColor == PieceColor.WHITE) { selectedColor = PieceColor.WHITE } }
                item { ColorChip("Black", selectedColor == PieceColor.BLACK) { selectedColor = PieceColor.BLACK } }
                PieceType.values().forEach { type ->
                    item { ColorChip(pieceName(type), selectedType == type) { selectedType = type } }
                }
            }

            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Side to move", Modifier.weight(1f))
                        FilterChip(selected = activeColor == PieceColor.WHITE, onClick = { activeColor = PieceColor.WHITE }, label = { Text("White") })
                        Spacer(Modifier.width(6.dp))
                        FilterChip(selected = activeColor == PieceColor.BLACK, onClick = { activeColor = PieceColor.BLACK }, label = { Text("Black") })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Engine depth: $depth", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = depth.toFloat(),
                        onValueChange = { viewModel.setAiDepth(it.toInt().coerceIn(1, 30)) },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                    Text("Lower = faster • Higher = deeper analysis", style = MaterialTheme.typography.bodySmall)
                }
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2)
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(fen))
                        copied = true
                    }) { Icon(Icons.Default.ContentCopy, "Copy FEN") }
                }
            }
            if (copied) Text("FEN copied", style = MaterialTheme.typography.labelSmall)

            if (error != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = {
                    val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
                    val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
                    if (whiteKings != 1 || blackKings != 1) {
                        error = "Exactly one White King and one Black King are required."
                        return@Button
                    }
                    val fenPosition = position.copy(activeColor = activeColor, castlingRights = CastlingRights.NONE, enPassantTarget = null, halfmoveClock = 0, fullmoveNumber = 1)
                    if (viewModel.loadFen(fenPosition.toFen())) onAnalyze() else error = "The position could not be loaded into the chess engine."
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Psychology, null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze with Bot")
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
private fun ColorChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun SimpleEditorBoard(position: ChessPosition, onSquareClick: (Square) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        for (rank in 7 downTo 0) {
            Row(Modifier.weight(1f)) {
                for (file in 0..7) {
                    val square = Square(file, rank)
                    val piece = position.pieceAt(square)
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .background(if (square.isLightSquare) Color(0xFFE8DCC8) else Color(0xFF7A6046))
                            .clickable { onSquareClick(square) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(piece?.unicodeSymbol ?: "", style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
        }
    }
}
