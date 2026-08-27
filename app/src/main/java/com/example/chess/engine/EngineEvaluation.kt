package com.example.chess.engine

import com.example.chess.model.ChessMove

data class EngineEvaluation(
    val bestMove: ChessMove?,
    val scoreCp: Int,
    val mateIn: Int? = null,
    val depth: Int = 0,
    val pv: List<String> = emptyList(),
    val nodesEvaluated: Long = 0,
    val timeElapsedMs: Long = 0
)
