package com.example.chess.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FenModePositionTest {
    @Test
    fun whiteToMovePositionPreservesFenState() {
        val fen = "8/8/8/3k4/8/8/4K3/8 w - - 7 23"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals(7, position.halfmoveClock)
        assertEquals(23, position.fullmoveNumber)
        assertEquals(fen, position.toFen())
    }

    @Test
    fun blackToMovePreservesCastlingAndEnPassant() {
        val fen = "r3k2r/8/8/3pP3/8/8/8/R3K2R b KQkq e6 0 17"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.BLACK, position!!.activeColor)
        assertEquals("KQkq", position.castlingRights.toFen())
        assertEquals("e6", position.enPassantTarget?.algebraic)
        assertEquals(fen, position.toFen())
    }

    @Test
    fun partialCastlingRightsPreserved() {
        val fen = "r3k2r/8/8/8/8/8/8/R3K2R w Kq - 2 10"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("Kq", position.castlingRights.toFen())
        assertEquals(fen, position.toFen())
    }

    @Test
    fun noCastlingAndNoEnPassantPreserved() {
        val fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("-", position.castlingRights.toFen())
        assertNull(position.enPassantTarget)
        assertEquals(fen, position.toFen())
    }

    @Test
    fun invalidFenIsRejected() {
        assertNull(ChessPosition.fromFen("8/8/8/8/8/8/8/8 w - - 0 1")) // No kings
        assertNull(ChessPosition.fromFen("4k3/8/8/8/8/8/8/8 w - - 0 1")) // Missing white king
        assertNull(ChessPosition.fromFen("4K3/8/8/8/8/8/8/8 w - - 0 1")) // Missing black king
        assertNull(ChessPosition.fromFen("invalid fen format"))
        assertNull(ChessPosition.fromFen(""))
    }

    @Test
    fun complexMiddlegameFenRoundTrip() {
        val fen = "r1bqk2r/pppp1ppp/2n5/4p3/1b1Pn3/2N2N2/PPP1BPPP/R1BQK2R w KQkq - 0 6"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("KQkq", position.castlingRights.toFen())
        assertEquals(fen, position.toFen())
    }

    @Test
    fun userProblematicFenParsesAndGeneratesValidLegalMoves() {
        val fen = "r1bqk2r/pppp1ppp/2n2n2/8/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 4"
        val position = ChessPosition.fromFen(fen)
        assertNotNull(position)
        assertEquals(PieceColor.WHITE, position!!.activeColor)
        assertEquals("KQkq", position.castlingRights.toFen())
        assertEquals(4, position.halfmoveClock)
        assertEquals(4, position.fullmoveNumber)
        assertEquals(fen, position.toFen())

        val legalMoves = position.generateLegalMoves()
        assertTrue("Position must have legal moves", legalMoves.isNotEmpty())

        // Verify kingside castling is legal (e1g1 / O-O)
        val castleMove = legalMoves.find { it.isCastleKingside }
        assertNotNull("White must be able to castle kingside", castleMove)
        assertEquals("e1g1", castleMove!!.uci)
        assertEquals("O-O", castleMove.san)
    }

    @Test
    fun userProblematicFenConsecutiveMovesSequence() {
        val fen = "r1bqk2r/pppp1ppp/2n2n2/8/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 4"
        val pos0 = ChessPosition.fromFen(fen)!!

        // Move 1: White castles kingside (e1g1)
        val move1 = pos0.generateLegalMoves().first { it.uci == "e1g1" }
        val pos1 = pos0.makeMove(move1)
        assertEquals(PieceColor.BLACK, pos1.activeColor)
        assertEquals("kq", pos1.castlingRights.toFen())
        assertEquals(5, pos1.halfmoveClock)
        assertEquals(4, pos1.fullmoveNumber)
        assertEquals("r1bqk2r/pppp1ppp/2n2n2/8/2B1P3/2N2N2/PPPP1PPP/R1BQ1RK1 b kq - 5 4", pos1.toFen())

        // Move 2: Black plays d7d6
        val move2 = pos1.generateLegalMoves().first { it.uci == "d7d6" }
        val pos2 = pos1.makeMove(move2)
        assertEquals(PieceColor.WHITE, pos2.activeColor)
        assertEquals("kq", pos2.castlingRights.toFen())
        assertEquals(0, pos2.halfmoveClock)
        assertEquals(5, pos2.fullmoveNumber)
        assertEquals("r1bqk2r/ppp2ppp/2np1n2/8/2B1P3/2N2N2/PPPP1PPP/R1BQ1RK1 w kq - 0 5", pos2.toFen())

        // Move 3: White plays d2d4
        val move3 = pos2.generateLegalMoves().first { it.uci == "d2d4" }
        val pos3 = pos2.makeMove(move3)
        assertEquals(PieceColor.BLACK, pos3.activeColor)
        assertEquals(Square.fromAlgebraic("d3"), pos3.enPassantTarget)
        assertEquals(0, pos3.halfmoveClock)
        assertEquals(5, pos3.fullmoveNumber)
        assertEquals("r1bqk2r/ppp2ppp/2np1n2/8/2BPP3/2N2N2/PPP2PPP/R1BQ1RK1 b kq d3 0 5", pos3.toFen())
    }

    @Test
    fun multipleCustomPuzzleFensLegalMoveGeneration() {
        // Tactical mate/pin puzzle
        val puzzle1 = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5N2/PPPP1PPP/RNBQK2R w KQkq - 0 4"
        val posP1 = ChessPosition.fromFen(puzzle1)
        assertNotNull(posP1)
        assertTrue(posP1!!.generateLegalMoves().any { it.uci == "c4f7" }) // Bxf7+ sacrifice

        // Endgame Rook & King position
        val puzzle2 = "8/5k2/8/8/8/8/1K5R/8 w - - 0 1"
        val posP2 = ChessPosition.fromFen(puzzle2)
        assertNotNull(posP2)
        assertEquals(PieceColor.WHITE, posP2!!.activeColor)
        assertTrue(posP2.generateLegalMoves().any { it.uci == "h2f2" })

        // Black to move with queenside castling right
        val puzzle3 = "r3k2r/pbppqppp/1pn2n2/4p3/4P3/2NP1N2/PPPQBPPP/R3K2R b KQkq - 3 8"
        val posP3 = ChessPosition.fromFen(puzzle3)
        assertNotNull(posP3)
        assertEquals(PieceColor.BLACK, posP3!!.activeColor)
        assertTrue(posP3.generateLegalMoves().any { it.isCastleQueenside && it.uci == "e8c8" })
    }
}
