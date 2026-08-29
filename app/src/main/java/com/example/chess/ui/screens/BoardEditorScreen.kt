package com.example.chess.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chess.model.*
import com.example.chess.ui.ChessViewModel
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardEditorScreen(
    viewModel: ChessViewModel,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    
    // We can use a local mutable state for the editor
    var boardState by remember { mutableStateOf(MutableList<ChessPiece?>(64) { null }) }
    var selectedPiece by remember { mutableStateOf<ChessPiece?>(null) }
    var isErasing by remember { mutableStateOf(false) }
    var activeColor by remember { mutableStateOf(PieceColor.WHITE) }

    // Init from current state if requested, otherwise empty
    LaunchedEffect(Unit) {
        // If coming from vision, state might have position. Otherwise clear.
        boardState = state.position.board.toMutableList()
        activeColor = state.position.activeColor
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Custom Board Editor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))

        // Basic Board rendering for Editor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        ) {
            Column(Modifier.fillMaxSize()) {
                for (rank in 7 downTo 0) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (file in 0..7) {
                            val square = Square.fromIndex(rank * 8 + file)
                            val isLight = (file + rank) % 2 != 0
                            val bgColor = if (isLight) Color(0xFFEEEED2) else Color(0xFF769656)
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(bgColor)
                                    .clickable {
                                        val newBoard = boardState.toMutableList()
                                        if (isErasing) {
                                            newBoard[square.index] = null
                                        } else if (selectedPiece != null) {
                                            newBoard[square.index] = selectedPiece
                                        }
                                        boardState = newBoard
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val piece = boardState[square.index]
                                                                if (piece != null) {
                                    Text(
                                        text = piece.unicodeSymbol,
                                        fontSize = 32.sp,
                                        color = if (piece.color == PieceColor.WHITE) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Piece Palette
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PiecePaletteButton(piece = ChessPiece(PieceType.PAWN, PieceColor.WHITE), selected = selectedPiece?.type == PieceType.PAWN && selectedPiece?.color == PieceColor.WHITE && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE), selected = selectedPiece?.type == PieceType.KNIGHT && selectedPiece?.color == PieceColor.WHITE && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.BISHOP, PieceColor.WHITE), selected = selectedPiece?.type == PieceType.BISHOP && selectedPiece?.color == PieceColor.WHITE && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.ROOK, PieceColor.WHITE), selected = selectedPiece?.type == PieceType.ROOK && selectedPiece?.color == PieceColor.WHITE && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.QUEEN, PieceColor.WHITE), selected = selectedPiece?.type == PieceType.QUEEN && selectedPiece?.color == PieceColor.WHITE && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.KING, PieceColor.WHITE), selected = selectedPiece?.type == PieceType.KING && selectedPiece?.color == PieceColor.WHITE && !isErasing) { selectedPiece = it; isErasing = false }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PiecePaletteButton(piece = ChessPiece(PieceType.PAWN, PieceColor.BLACK), selected = selectedPiece?.type == PieceType.PAWN && selectedPiece?.color == PieceColor.BLACK && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK), selected = selectedPiece?.type == PieceType.KNIGHT && selectedPiece?.color == PieceColor.BLACK && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.BISHOP, PieceColor.BLACK), selected = selectedPiece?.type == PieceType.BISHOP && selectedPiece?.color == PieceColor.BLACK && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.ROOK, PieceColor.BLACK), selected = selectedPiece?.type == PieceType.ROOK && selectedPiece?.color == PieceColor.BLACK && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.QUEEN, PieceColor.BLACK), selected = selectedPiece?.type == PieceType.QUEEN && selectedPiece?.color == PieceColor.BLACK && !isErasing) { selectedPiece = it; isErasing = false }
            PiecePaletteButton(piece = ChessPiece(PieceType.KING, PieceColor.BLACK), selected = selectedPiece?.type == PieceType.KING && selectedPiece?.color == PieceColor.BLACK && !isErasing) { selectedPiece = it; isErasing = false }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { isErasing = true; selectedPiece = null }) {
                Icon(Icons.Default.Delete, contentDescription = "Erase", tint = if (isErasing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = { boardState = MutableList(64) { null } }) {
                Text("Clear All", color = MaterialTheme.colorScheme.error)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Text("Turn:", color = MaterialTheme.colorScheme.onBackground)
            FilterChip(selected = activeColor == PieceColor.WHITE, onClick = { activeColor = PieceColor.WHITE }, label = { Text("White") })
            FilterChip(selected = activeColor == PieceColor.BLACK, onClick = { activeColor = PieceColor.BLACK }, label = { Text("Black") })
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                // Generate FEN
                val pos = ChessPosition(
                    board = boardState.toList(),
                    activeColor = activeColor,
                    castlingRights = CastlingRights.NONE, // For simplicity
                    enPassantTarget = null,
                    halfmoveClock = 0,
                    fullmoveNumber = 1
                )
                val fen = pos.toFen()
                viewModel.loadFen(fen)
                onAnalyze()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Analyze with Bot")
        }
    }
}

@Composable
fun PiecePaletteButton(piece: ChessPiece, selected: Boolean, onClick: (ChessPiece) -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { onClick(piece) }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = piece.unicodeSymbol,
            fontSize = 32.sp,
            color = if (piece.color == PieceColor.WHITE) Color.White else Color.Black
        )
    }
}
