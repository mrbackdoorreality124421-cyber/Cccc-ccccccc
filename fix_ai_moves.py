with open("app/src/main/java/com/example/chess/ui/ChessViewModel.kt", "r") as f:
    content = f.read()

import re

# In triggerAiMove:
# Find: if (matchingMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) executeMove(matchingMove)
# Replace with setting engineArrowMove after executing
content = re.sub(
    r'if \(matchingMove != null && _uiState\.value\.status == GameStatus\.IN_PROGRESS\) executeMove\(matchingMove\)',
    'if (matchingMove != null && _uiState.value.status == GameStatus.IN_PROGRESS) { executeMove(matchingMove); _uiState.update { it.copy(engineArrowMove = matchingMove) } }',
    content
)

# In triggerHelperBotMove:
# Find: if (calculatedMove != null && _uiState.value.status == GameStatus.IN_PROGRESS && _uiState.value.helperBotAutoPlay) { delay(600); executeMove(calculatedMove); _uiState.update { it.copy(engineArrowMove = null) } }
# Replace with keeping the arrow
content = re.sub(
    r'if \(calculatedMove != null && _uiState\.value\.status == GameStatus\.IN_PROGRESS && _uiState\.value\.helperBotAutoPlay\) \{ delay\(600\); executeMove\(calculatedMove\); _uiState\.update \{ it\.copy\(engineArrowMove = null\) \} \}',
    'if (calculatedMove != null && _uiState.value.status == GameStatus.IN_PROGRESS && _uiState.value.helperBotAutoPlay) { delay(600); executeMove(calculatedMove); _uiState.update { it.copy(engineArrowMove = calculatedMove) } }',
    content
)

with open("app/src/main/java/com/example/chess/ui/ChessViewModel.kt", "w") as f:
    f.write(content)
