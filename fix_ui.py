import re

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "r") as f:
    content = f.read()

# Fix EditorBoard piece rendering to use shadows
editor_board_old = """
                        if (piece != null) {
                            Text(
                                text = piece.unicodeSymbol,
                                fontSize = 32.sp,
                                color = if (piece.color == PieceColor.WHITE) Color.White else Color.Black
                            )
                        }
"""
editor_board_new = """
                        if (piece != null) {
                            val isW = piece.color == PieceColor.WHITE
                            Text(
                                text = piece.unicodeSymbol,
                                fontSize = 32.sp,
                                color = if (isW) Color.White else Color(0xFF111111),
                                style = androidx.compose.ui.text.TextStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = if (isW) Color(0x99000000) else Color(0x66FFFFFF),
                                        offset = Offset(0f, if (isW) 4f else -2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
"""
content = content.replace(editor_board_old.strip(), editor_board_new.strip())

# Fix piece selector UI to use shadows and look like board squares
selector_white = """
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
                                            color = Color(0x99000000), offset = Offset(0f, 4f), blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
"""
content = re.sub(
    r'PieceType\.values\(\)\.forEach \{ type ->\n\s*val piece = ChessPiece\(type, PieceColor\.WHITE\)\n\s*val isSelected = selectedPiece == piece\n\s*Box\(\n\s*modifier = Modifier\n\s*\.size\(48\.dp\)\n\s*\.clip\(RoundedCornerShape\(8\.dp\)\)\n\s*\.background\(if \(isSelected\) Color\(0xFFB7C0D8\) else Color\(0xFFE8EDF9\)\)\n\s*\.clickable \{ selectedPiece = piece \},\n\s*contentAlignment = Alignment\.Center\n\s*\) \{\n\s*Text\(\n\s*text = piece\.unicodeSymbol,\n\s*fontSize = 36\.sp,\n\s*color = Color\.Black\n\s*\)\n\s*\}\n\s*\}',
    selector_white.strip(),
    content,
    flags=re.DOTALL
)

selector_black = """
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
                                            color = Color(0x66FFFFFF), offset = Offset(0f, -2f), blurRadius = 4f
                                        )
                                    )
                                )
                            }
                        }
"""
content = re.sub(
    r'PieceType\.values\(\)\.forEach \{ type ->\n\s*val piece = ChessPiece\(type, PieceColor\.BLACK\)\n\s*val isSelected = selectedPiece == piece\n\s*Box\(\n\s*modifier = Modifier\n\s*\.size\(48\.dp\)\n\s*\.clip\(RoundedCornerShape\(8\.dp\)\)\n\s*\.background\(if \(isSelected\) Color\(0xFFB7C0D8\) else Color\(0xFFE8EDF9\)\)\n\s*\.clickable \{ selectedPiece = piece \},\n\s*contentAlignment = Alignment\.Center\n\s*\) \{\n\s*Text\(\n\s*text = piece\.unicodeSymbol,\n\s*fontSize = 36\.sp,\n\s*color = Color\.Black\n\s*\)\n\s*\}\n\s*\}',
    selector_black.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "w") as f:
    f.write(content)
