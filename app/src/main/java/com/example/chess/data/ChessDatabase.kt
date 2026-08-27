package com.example.chess.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [GameRecord::class, PuzzleRecord::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {

    abstract fun chessDao(): ChessDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_master_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch(Dispatchers.IO) {
                            getDatabase(context, scope).chessDao().insertPuzzles(getDefaultPuzzles())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        fun getDefaultPuzzles(): List<PuzzleRecord> {
            return listOf(
                PuzzleRecord(
                    id = "puzzle_1",
                    title = "Scholar's Mate Tactic",
                    description = "White has a direct strike on the weak f7 square with Queen and Bishop.",
                    initialFen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 4",
                    playerColor = "WHITE",
                    solutionMovesSan = "Qxf7#",
                    rating = 800,
                    isSolved = false
                ),
                PuzzleRecord(
                    id = "puzzle_2",
                    title = "Back-Rank Checkmate",
                    description = "The enemy King is trapped behind its own pawns. Deliver checkmate on the 8th rank.",
                    initialFen = "6k1/5ppp/8/8/8/8/4rPPP/R5K1 w - - 0 1",
                    playerColor = "WHITE",
                    solutionMovesSan = "Ra8+ Re8 Rxe8#",
                    rating = 1000,
                    isSolved = false
                ),
                PuzzleRecord(
                    id = "puzzle_3",
                    title = "Anastasia's Knight Trap",
                    description = "Sacrifice the Queen to open the h-file for the Rook, assisted by the Knight.",
                    initialFen = "5rk1/1p3ppp/1N6/8/8/8/1q3PPP/4RRK1 w - - 0 1",
                    playerColor = "WHITE",
                    solutionMovesSan = "Nd7 Rd8",
                    rating = 1250,
                    isSolved = false
                ),
                PuzzleRecord(
                    id = "puzzle_4",
                    title = "Opera Game Queen Sacrifice",
                    description = "Morphy's famous combination: sacrifice Queen on b8 leading to Rd8#.",
                    initialFen = "4kb1r/p2rqppp/5n2/1B2p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 0 1",
                    playerColor = "WHITE",
                    solutionMovesSan = "Bxd7+ Nxd7 Qb8+ Nxb8 Rd8#",
                    rating = 1500,
                    isSolved = false
                ),
                PuzzleRecord(
                    id = "puzzle_5",
                    title = "Smothered Mate (Philidor's Legacy)",
                    description = "Sacrifice the Queen with check so Black must capture with the Rook, allowing the Knight to deliver smothered mate.",
                    initialFen = "6k1/5Npp/8/8/8/8/4QPPP/6K1 w - - 0 1",
                    playerColor = "WHITE",
                    solutionMovesSan = "Qe8#",
                    rating = 1100,
                    isSolved = false
                )
            )
        }
    }
}
