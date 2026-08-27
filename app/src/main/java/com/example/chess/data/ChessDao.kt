package com.example.chess.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChessDao {

    @Query("SELECT * FROM game_records ORDER BY dateTimestamp DESC")
    fun getAllGameRecords(): Flow<List<GameRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGameRecord(record: GameRecord): Long

    @Delete
    suspend fun deleteGameRecord(record: GameRecord)

    @Query("DELETE FROM game_records")
    suspend fun deleteAllGameRecords()

    @Query("SELECT * FROM puzzle_records ORDER BY rating ASC")
    fun getAllPuzzles(): Flow<List<PuzzleRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPuzzles(puzzles: List<PuzzleRecord>)

    @Query("UPDATE puzzle_records SET isSolved = 1 WHERE id = :puzzleId")
    suspend fun markPuzzleSolved(puzzleId: String)
}
