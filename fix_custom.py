with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "r") as f:
    content = f.read()

content = content.replace("whiteKingSide", "whiteKingside")
content = content.replace("whiteQueenSide", "whiteQueenside")
content = content.replace("blackKingSide", "blackKingside")
content = content.replace("blackQueenSide", "blackQueenside")
content = content.replace("com.example.chess.ui.components.pieceUnicode(piece)", "piece.unicodeSymbol")

if "import androidx.compose.ui.unit.sp" not in content:
    content = content.replace("import androidx.compose.ui.unit.dp", "import androidx.compose.ui.unit.dp\nimport androidx.compose.ui.unit.sp")

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "w") as f:
    f.write(content)
