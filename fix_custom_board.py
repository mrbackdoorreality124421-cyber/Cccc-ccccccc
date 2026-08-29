import re

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "r") as f:
    content = f.read()

replacement = """
            Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Piece to Place", style = MaterialTheme.typography.labelLarge)
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PieceType.values().forEach { type ->
                            val isSelected = selectedColor == PieceColor.WHITE && selectedType == type
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedColor = PieceColor.WHITE; selectedType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ChessPiece(type, PieceColor.WHITE).unicodeSymbol,
                                    fontSize = 36.sp,
                                    color = Color.Black // White pieces usually outline or solid, let's keep it visible
                                )
                            }
                        }
                    }
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        PieceType.values().forEach { type ->
                            val isSelected = selectedColor == PieceColor.BLACK && selectedType == type
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { selectedColor = PieceColor.BLACK; selectedType = type },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ChessPiece(type, PieceColor.BLACK).unicodeSymbol,
                                    fontSize = 36.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
"""

content = re.sub(
    r'Text\("Piece color",.*?\n\s*LazyRow.*?\{.*?\}\n\s*\}\n\s*\}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "w") as f:
    f.write(content)
