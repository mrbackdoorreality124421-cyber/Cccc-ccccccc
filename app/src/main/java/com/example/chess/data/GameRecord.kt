package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val eventName: String = "Casual Game",
    val whitePlayer: String = "Player",
    val blackPlayer: String = "AI (Master)",
    val result: String = "*", // "1-0", "0-1", "1/2-1/2", "*"
    val moveCount: Int = 0,
    val pgn: String = "",
    val finalFen: String = "",
    val gameMode: String = "PLAYER_VS_AI",
    val durationSeconds: Long = 0
)

@Entity(tableName = "puzzle_records")
data class PuzzleRecord(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val initialFen: String,
    val playerColor: String, // "WHITE" or "BLACK"
    val solutionMovesSan: String, // Space-separated SAN moves e.g. "Qxh7+ Kxh7 Rh3#
    val rating: Int = 1200,
    val isSolved: Boolean = false
)
