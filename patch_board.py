import re

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "r") as f:
    content = f.read()

# Fix the board orientation in EditorBoard
content = re.sub(
    r'val square = Square\(rank, file\)',
    'val square = Square(file = file, rank = rank)',
    content
)

# Fix the visibility of pieces in selector
replacement = """
                        PieceType.values().forEach { type ->
                            val piece = ChessPiece(type, PieceColor.WHITE)
                            val isSelected = selectedPiece == piece
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFB7C0D8) else Color(0xFFE8EDF9))
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
"""

content = re.sub(
    r'PieceType\.values\(\)\.forEach \{ type ->\n\s*val piece = ChessPiece\(type, PieceColor\.WHITE\)\n\s*val isSelected = selectedPiece == piece\n\s*Box\(\n\s*modifier = Modifier\n\s*\.size\(48\.dp\)\n\s*\.clip\(RoundedCornerShape\(8\.dp\)\)\n\s*\.background\(if \(isSelected\) MaterialTheme\.colorScheme\.primaryContainer else Color\.Transparent\)\n\s*\.clickable \{ selectedPiece = piece \},\n\s*contentAlignment = Alignment\.Center\n\s*\) \{\n\s*Text\(\n\s*text = piece\.unicodeSymbol,\n\s*fontSize = 36\.sp,\n\s*color = Color\.Black\n\s*\)\n\s*\}\n\s*\}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

replacement2 = """
                        PieceType.values().forEach { type ->
                            val piece = ChessPiece(type, PieceColor.BLACK)
                            val isSelected = selectedPiece == piece
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFB7C0D8) else Color(0xFFE8EDF9))
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
"""

content = re.sub(
    r'PieceType\.values\(\)\.forEach \{ type ->\n\s*val piece = ChessPiece\(type, PieceColor\.BLACK\)\n\s*val isSelected = selectedPiece == piece\n\s*Box\(\n\s*modifier = Modifier\n\s*\.size\(48\.dp\)\n\s*\.clip\(RoundedCornerShape\(8\.dp\)\)\n\s*\.background\(if \(isSelected\) MaterialTheme\.colorScheme\.primaryContainer else Color\.Transparent\)\n\s*\.clickable \{ selectedPiece = piece \},\n\s*contentAlignment = Alignment\.Center\n\s*\) \{\n\s*Text\(\n\s*text = piece\.unicodeSymbol,\n\s*fontSize = 36\.sp,\n\s*color = Color\.Black\n\s*\)\n\s*\}\n\s*\}',
    replacement2.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "w") as f:
    f.write(content)
