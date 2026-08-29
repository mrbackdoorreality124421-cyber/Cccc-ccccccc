with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "r") as f:
    content = f.read()

content = content.replace("ChessBoardView", "ChessBoard2D")
content = content.replace("state.activeEngine?.name", "state.activeEngineName")

with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "w") as f:
    f.write(content)
