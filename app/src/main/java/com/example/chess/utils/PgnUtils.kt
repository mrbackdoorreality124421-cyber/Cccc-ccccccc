package com.example.chess.utils

import com.example.chess.model.ChessGameState
import com.example.chess.model.GameStatus
import com.example.chess.model.PieceColor
import java.text.SimpleDateFormat
import java.util.*

object PgnUtils {
    fun exportToPgn(
        state: ChessGameState,
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
            else -> "*"
        }

        val sb = StringBuilder()
        sb.append("[Event \"Chess Master Pro Match\"]\n")
        sb.append("[Site \"Android Device\"]\n")
        sb.append("[Date \"$dateStr\"]\n")
        sb.append("[White \"$whitePlayer\"]\n")
        sb.append("[Black \"$blackPlayer\"]\n")
        sb.append("[Result \"$resultStr\"]\n")
        sb.append("\n")

        val moves = state.moveHistory
        for (i in moves.indices) {
            if (i % 2 == 0) {
                val moveNum = (i / 2) + 1
                sb.append("$moveNum. ")
            }
            val san = moves[i].san.ifEmpty { moves[i].uci }
            sb.append("$san ")
        }
        sb.append(resultStr)

        return sb.toString().trim()
    }
}
