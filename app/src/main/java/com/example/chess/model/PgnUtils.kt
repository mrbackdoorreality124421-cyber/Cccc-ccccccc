package com.example.chess.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PgnUtils {

    fun exportToPgn(
        state: ChessGameState,
        eventName: String = "Casual Game",
        site: String = "Android Chess Master Pro",
        whitePlayer: String = "White",
        blackPlayer: String = "Black"
    ): String {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.US)
        val dateStr = dateFormat.format(Date())

        val resultStr = when (state.status) {
            GameStatus.CHECKMATE -> if (state.position.activeColor == PieceColor.WHITE) "0-1" else "1-0"
            GameStatus.STALEMATE,
            GameStatus.DRAW_FIFTY_MOVE,
            GameStatus.DRAW_REPETITION,
            GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "1/2-1/2"
            GameStatus.IN_PROGRESS -> "*"
        }

        val sb = StringBuilder()
        sb.appendLine("[Event \"$eventName\"]")
        sb.appendLine("[Site \"$site\"]")
        sb.appendLine("[Date \"$dateStr\"]")
        sb.appendLine("[Round \"1\"]")
        sb.appendLine("[White \"$whitePlayer\"]")
        sb.appendLine("[Black \"$blackPlayer\"]")
        sb.appendLine("[Result \"$resultStr\"]")
        sb.appendLine()

        for (i in state.moveHistory.indices) {
            if (i % 2 == 0) {
                sb.append("${(i / 2) + 1}. ")
            }
            sb.append("${state.moveHistory[i].san} ")
        }
        sb.append(resultStr)

        return sb.toString()
    }

    fun parsePgnMoves(pgnText: String): List<String> {
        val cleanText = pgnText
            .lines()
            .filterNot { it.trim().startsWith("[") }
            .joinToString(" ")
            .replace(Regex("\\{[^}]*\\}"), "") // remove comments
            .replace(Regex("\\([^)]*\\)"), "") // remove variations
            .replace(Regex("\\$\\d+"), "") // remove NAGs
            .trim()

        val tokens = cleanText.split(Regex("\\s+"))
        val moves = mutableListOf<String>()

        for (token in tokens) {
            val t = token.trim()
            if (t.isEmpty() || t.endsWith(".") || t == "1-0" || t == "0-1" || t == "1/2-1/2" || t == "*") {
                continue
            }
            // Remove leading move number if attached like "1.e4"
            val sanitized = t.replace(Regex("^\\d+\\.*"), "").trim()
            if (sanitized.isNotEmpty()) {
                moves.add(sanitized)
            }
        }
        return moves
    }
}
