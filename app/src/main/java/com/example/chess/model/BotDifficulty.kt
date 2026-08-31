package com.example.chess.model

enum class BotDifficulty(val label: String, val moveTimeMs: Int, val depth: Int) {
    BEGINNER("Beginner", 500, 8),
    EASY("Easy", 1500, 12),
    MEDIUM("Medium", 3000, 18),
    HARD("Hard", 5000, 25),
    MASTER("Master", 10000, 30)
}
