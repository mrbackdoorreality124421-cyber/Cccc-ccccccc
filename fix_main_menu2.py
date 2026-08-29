with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "r") as f:
    content = f.read()

content = content.replace("onSquareClick = {}", "onSquareClicked = {}")
content = content.replace("is3DView = false,", "")
content = content.replace("boardTheme = state.boardTheme", "theme = state.boardTheme")

with open("app/src/main/java/com/example/chess/ui/screens/MainMenuScreen.kt", "w") as f:
    f.write(content)
