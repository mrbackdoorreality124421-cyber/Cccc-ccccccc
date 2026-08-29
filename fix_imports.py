import re

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "r") as f:
    content = f.read()

content = content.replace("Offset(0f,", "androidx.compose.ui.geometry.Offset(0f,")

with open("app/src/main/java/com/example/chess/ui/screens/CustomBoardScreen.kt", "w") as f:
    f.write(content)
