import re

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "r") as f:
    content = f.read()

# Replace variables
content = re.sub(
    r'var selectedColor by remember \{ mutableStateOf\(PieceColor\.WHITE\) \}\n\s*var selectedType by remember \{ mutableStateOf\(PieceType\.QUEEN\) \}',
    'var selectedPiece by remember { mutableStateOf<ChessPiece?>(ChessPiece(PieceType.QUEEN, PieceColor.WHITE)) }',
    content
)

# Replace editSquare
content = re.sub(
    r'fun editSquare\(square: Square\) \{\n\s*val next = position\.board\.toMutableList\(\)\n\s*val current = next\[square\.index\]\n\s*next\[square\.index\] = if \(current\?\.color == selectedColor && current\.type == selectedType\) null else ChessPiece\(selectedType, selectedColor\)\n\s*position = position\.copy\(board = next\)\n\s*\}',
    'fun editSquare(square: Square) {\n        val next = position.board.toMutableList()\n        next[square.index] = selectedPiece\n        position = position.copy(board = next)\n    }',
    content
)

# Replace UI
replacement = """
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
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedPiece = piece },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = piece.unicodeSymbol,
                                    fontSize = 36.sp,
                                    color = Color.Black
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
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedPiece = piece },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = piece.unicodeSymbol,
                                    fontSize = 36.sp,
                                    color = Color.Black
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
"""

content = re.sub(
    r'Surface\(shape = RoundedCornerShape\(12\.dp\), tonalElevation = 1\.dp, modifier = Modifier\.fillMaxWidth\(\)\) \{\n\s*Column\(Modifier\.padding\(12\.dp\), verticalArrangement = Arrangement\.spacedBy\(12\.dp\)\) \{\n\s*Text\("Select Piece to Place",.*?\n\s*Row\(Modifier\.fillMaxWidth\(\), horizontalArrangement = Arrangement\.SpaceEvenly\) \{\n\s*PieceType\.values\(\)\.forEach \{ type ->\n\s*val isSelected = selectedColor == PieceColor\.BLACK && selectedType == type.*?\}\n\s*\}\n\s*\}\n\s*\}\n\s*\}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "w") as f:
    f.write(content)
