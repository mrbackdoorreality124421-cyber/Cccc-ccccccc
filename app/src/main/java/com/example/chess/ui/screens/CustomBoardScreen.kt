package com.example.chess.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedPiece by remember { mutableStateOf<ChessPiece?>(ChessPiece(PieceType.QUEEN, PieceColor.WHITE)) }
    
    var activeColor by remember { mutableStateOf(PieceColor.WHITE) }
    
    // Castling Rights
    var wK by remember { mutableStateOf(true) }
    var wQ by remember { mutableStateOf(true) }
    var bK by remember { mutableStateOf(true) }
    var bQ by remember { mutableStateOf(true) }
    
    var message by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val fen = remember(position, activeColor, wK, wQ, bK, bQ) {
        val cr = CastlingRights(
            whiteKingside = wK && position.pieceAt(Square(7, 4))?.type == PieceType.KING && position.pieceAt(Square(7, 7))?.type == PieceType.ROOK,
            whiteQueenside = wQ && position.pieceAt(Square(7, 4))?.type == PieceType.KING && position.pieceAt(Square(7, 0))?.type == PieceType.ROOK,
            blackKingside = bK && position.pieceAt(Square(0, 4))?.type == PieceType.KING && position.pieceAt(Square(0, 7))?.type == PieceType.ROOK,
            blackQueenside = bQ && position.pieceAt(Square(0, 4))?.type == PieceType.KING && position.pieceAt(Square(0, 0))?.type == PieceType.ROOK
        )
        position.copy(activeColor = activeColor, castlingRights = cr, enPassantTarget = null, halfmoveClock = 0, fullmoveNumber = 1).toFen()
    }

    fun clearBoard() {
        position = position.copy(board = MutableList(64) { null })
        message = null
    }

    fun setInitialBoard() {
        position = ChessPosition.initial()
        wK = true; wQ = true; bK = true; bQ = true; activeColor = PieceColor.WHITE
        message = null
    }

    fun editSquare(square: Square) {
        val next = position.board.toMutableList()
        next[square.index] = selectedPiece
        position = position.copy(board = next)
    }

    fun validateAndLaunch(mode: GameMode, pColor: PieceColor = PieceColor.WHITE) {
        val whiteKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackKings = position.board.count { it?.type == PieceType.KING && it.color == PieceColor.BLACK }
        if (whiteKings != 1 || blackKings != 1) {
            message = "Exactly one White King and one Black King are required."
            return
        }
        val success = viewModel.startCustomGame(fen, mode, pColor)
        if (success) {
            onAnalyze()
        } else {
            message = "Invalid position setup."
        }
    }

    var showPvBotDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Board Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = ::setInitialBoard) { Icon(Icons.Default.ContentCopy, "Reset Initial") }
                    IconButton(onClick = ::clearBoard) { Icon(Icons.Default.Delete, "Clear Board") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (message != null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(message!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Box(Modifier.fillMaxWidth().widthIn(max = 560.dp).aspectRatio(1f).clip(RoundedCornerShape(12.dp))) {
                EditorBoard(position, emptyList(), null, ::editSquare)
            }

            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Piece to Place (or Erase)", style = MaterialTheme.typography.labelLarge)
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PieceType.values().forEach { type ->
                            val piece = ChessPiece(type, PieceColor.WHITE)
                            val isSelected = selectedPiece == piece
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF90A4AE) else Color(0xFFCFD8DC))
                                    .clickable { selectedPiece = piece },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = piece.unicodeSymbol,
                                    fontSize = 36.sp,
                                    color = Color.White,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color(0x99000000), offset = androidx.compose.ui.geometry.Offset(0f, 4f), blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
                    }
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PieceType.values().forEach { type ->
                            val piece = ChessPiece(type, PieceColor.BLACK)
                            val isSelected = selectedPiece == piece
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF90A4AE) else Color(0xFFCFD8DC))
                                    .clickable { selectedPiece = piece },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = piece.unicodeSymbol,
                                    fontSize = 36.sp,
                                    color = Color(0xFF111111),
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = androidx.compose.ui.graphics.Shadow(
                                            color = Color(0x66FFFFFF), offset = androidx.compose.ui.geometry.Offset(0f, -2f), blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
                    }
                    
                    // Eraser
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        val isSelected = selectedPiece == null
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.errorContainer else Color.Transparent)
                                .clickable { selectedPiece = null }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Erase", tint = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                                Text("Eraser Tool", color = if (isSelected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Side to Move", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = activeColor == PieceColor.WHITE, onClick = { activeColor = PieceColor.WHITE }, label = { Text("White") })
                        FilterChip(selected = activeColor == PieceColor.BLACK, onClick = { activeColor = PieceColor.BLACK }, label = { Text("Black") })
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    Text("Castling Rights", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = wK, onClick = { wK = !wK }, label = { Text("W: O-O") })
                        FilterChip(selected = wQ, onClick = { wQ = !wQ }, label = { Text("W: O-O-O") })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = bK, onClick = { bK = !bK }, label = { Text("B: O-O") })
                        FilterChip(selected = bQ, onClick = { bQ = !bQ }, label = { Text("B: O-O-O") })
                    }
                }
            }

            Surface(shape = RoundedCornerShape(10.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 2)
                    IconButton(onClick = { clipboard.setText(AnnotatedString(fen)); copied = true }) {
                        Icon(Icons.Default.ContentCopy, "Copy FEN", tint = if (copied) Color.Green else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { validateAndLaunch(GameMode.ANALYSIS) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Analyze") }
                
                Button(
                    onClick = { showPvBotDialog = true },
                    modifier = Modifier.weight(1.5f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play vs Bot")
                }
            }
        }
    }

    if (showPvBotDialog) {
        var dialogPlayerColor by remember { mutableStateOf(PieceColor.WHITE) }
        AlertDialog(
            onDismissRequest = { showPvBotDialog = false },
            title = { Text("Play vs Bot Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Choose your color to play:")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FilterChip(selected = dialogPlayerColor == PieceColor.WHITE, onClick = { dialogPlayerColor = PieceColor.WHITE }, label = { Text("White") })
                        FilterChip(selected = dialogPlayerColor == PieceColor.BLACK, onClick = { dialogPlayerColor = PieceColor.BLACK }, label = { Text("Black") })
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPvBotDialog = false; validateAndLaunch(GameMode.PLAYER_VS_AI, dialogPlayerColor) }) { Text("Start Match") }
            },
            dismissButton = {
                TextButton(onClick = { showPvBotDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EditorBoard(
    position: ChessPosition,
    legalTargets: List<ChessMove>,
    selectedSquare: Square?,
    onSquare: (Square) -> Unit
) {
    val lightSquare = Color(0xFFE8EDF9)
    val darkSquare = Color(0xFFB7C0D8)

    Column(modifier = Modifier.fillMaxSize()) {
        for (rank in 7 downTo 0) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (file in 0..7) {
                    val isLight = (rank + file) % 2 != 0
                    val sqColor = if (isLight) lightSquare else darkSquare
                    val square = Square(file = file, rank = rank)
                    val piece = position.pieceAt(square)
                    val isSelected = square == selectedSquare
                    val isTarget = legalTargets.any { it.to == square }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(sqColor)
                            .clickable { onSquare(square) }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) Box(Modifier.fillMaxSize().background(Color(0x66FFEB3B)))
                        if (isTarget) Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0x88000000)))
                        if (piece != null) {
                            val isW = piece.color == PieceColor.WHITE
                            Text(
                                text = piece.unicodeSymbol,
                                fontSize = 32.sp,
                                color = if (isW) Color.White else Color(0xFF111111),
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = if (isW) Color(0x99000000) else Color(0x66FFFFFF),
                                        offset = androidx.compose.ui.geometry.Offset(0f, if (isW) 4f else -2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
