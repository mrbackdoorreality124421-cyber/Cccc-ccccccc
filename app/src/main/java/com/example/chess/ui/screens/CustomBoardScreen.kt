package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    fun updateSquare(square: Square) {
        val next = position.board.toMutableList()
        val index = square.index
        next[index] = if (next[index]?.color == selectedColor && next[index]?.type == selectedType) null
        else ChessPiece(selectedType, selectedColor)
        position = position.copy(board = next)
        error = null
    }

    val editablePosition = position
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Custom Board Setup") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { position = ChessPosition.initial(); error = null }) { Icon(Icons.Default.Delete, "Reset") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tap a square to place the selected piece. Tap it again to remove it.", style = MaterialTheme.typography.bodySmall)

            Box(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SimpleEditorBoard(editablePosition, onSquareClick = ::updateSquare)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                item { ColorChip("White", selectedColor == PieceColor.WHITE) { selectedColor = PieceColor.WHITE } }
                item { ColorChip("Black", selectedColor == PieceColor.BLACK) { selectedColor = PieceColor.BLACK } }
                PieceType.values().forEach { type ->
                    item { ColorChip(type.name.lowercase().replaceFirstChar { it.uppercase() }, selectedType == type) { selectedType = type } }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Side to move")
                FilterChip(selected = activeColor == PieceColor.WHITE, onClick = { activeColor = PieceColor.WHITE }, label = { Text("White") })
                FilterChip(selected = activeColor == PieceColor.BLACK, onClick = { activeColor = PieceColor.BLACK }, label = { Text("Black") })
            }

            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
                    val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
                    if (whiteKings != 1 || blackKings != 1) {
                        error = "Position must contain exactly one White King and one Black King."
                        return@Button
                    }
                    val fenPosition = position.copy(activeColor = activeColor, castlingRights = CastlingRights.NONE, enPassantTarget = null, halfmoveClock = 0, fullmoveNumber = 1)
                    if (viewModel.loadFen(fenPosition.toFen())) onAnalyze() else error = "Unable to load this position."
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.Psychology, null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze with Bot")
            }
        }
    }
}

@Composable
private fun ColorChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    ) { Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium) }
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
                        Modifier.weight(1f).fillMaxHeight().background(if (square.isLightSquare) Color(0xFFE8DCC8) else Color(0xFF7A6046)).clickable { onSquareClick(square) },
                        contentAlignment = Alignment.Center
                    ) { Text(piece?.unicodeSymbol ?: "", style = MaterialTheme.typography.headlineLarge) }
                }
            }
        }
    }
}
