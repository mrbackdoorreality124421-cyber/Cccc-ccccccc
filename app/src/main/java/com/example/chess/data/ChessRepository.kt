package com.example.chess.data

import kotlinx.coroutines.flow.Flow

class ChessRepository(private val dao: ChessDao) {

    val allGameRecords: Flow<List<GameRecord>> = dao.getAllGameRecords()
    val allPuzzles: Flow<List<PuzzleRecord>> = dao.getAllPuzzles()

    suspend fun saveGame(record: GameRecord): Long {
        return dao.insertGameRecord(record)
    }

    suspend fun deleteGame(record: GameRecord) {
        dao.deleteGameRecord(record)
    }

    suspend fun clearHistory() {
        dao.deleteAllGameRecords()
    }

    suspend fun markPuzzleSolved(puzzleId: String) {
        dao.markPuzzleSolved(puzzleId)
    }

    suspend fun seedDefaultPuzzlesIfEmpty() {
        dao.insertPuzzles(ChessDatabase.getDefaultPuzzles())
    }
}
