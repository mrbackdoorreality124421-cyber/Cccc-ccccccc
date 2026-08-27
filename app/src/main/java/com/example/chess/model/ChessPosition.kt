package com.example.chess.model

import kotlin.math.abs

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFen(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        val ALL = CastlingRights(true, true, true, true)
        val NONE = CastlingRights(false, false, false, false)

        fun fromFen(s: String): CastlingRights {
            return CastlingRights(
                whiteKingside = s.contains('K'),
                whiteQueenside = s.contains('Q'),
                blackKingside = s.contains('k'),
                blackQueenside = s.contains('q')
            )
        }
    }
}

enum class GameStatus {
    IN_PROGRESS,
    CHECKMATE,
    STALEMATE,
    DRAW_FIFTY_MOVE,
    DRAW_REPETITION,
    DRAW_INSUFFICIENT_MATERIAL
}

data class ChessPosition(
    val board: List<ChessPiece?>, // 64 entries
    val activeColor: PieceColor,
    val castlingRights: CastlingRights,
    val enPassantTarget: Square?,
    val halfmoveClock: Int,
    val fullmoveNumber: Int
) {
    fun pieceAt(square: Square): ChessPiece? {
        if (!square.isValid) return null
        return board[square.index]
    }

    fun pieceAtIndex(index: Int): ChessPiece? {
        if (index !in 0..63) return null
        return board[index]
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            for (file in 0..7) {
                val piece = board[rank * 8 + file]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(piece.fenChar)
                }
            }
            if (emptyCount > 0) {
                sb.append(emptyCount)
            }
            if (rank > 0) sb.append('/')
        }

        sb.append(' ')
        sb.append(if (activeColor == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ')
        sb.append(castlingRights.toFen())
        sb.append(' ')
        sb.append(enPassantTarget?.algebraic ?: "-")
        sb.append(' ')
        sb.append(halfmoveClock)
        sb.append(' ')
        sb.append(fullmoveNumber)

        return sb.toString()
    }

    fun generateLegalMoves(): List<ChessMove> {
        val pseudoMoves = generatePseudoLegalMoves()
        val legalMoves = ArrayList<ChessMove>(pseudoMoves.size)

        for (move in pseudoMoves) {
            val nextPos = makeMoveRaw(move)
            if (!nextPos.isKingInCheck(activeColor)) {
                val san = computeSan(move, pseudoMoves)
                legalMoves.add(move.copy(san = san))
            }
        }

        return legalMoves
    }

    fun isKingInCheck(kingColor: PieceColor): Boolean {
        var kingSquare: Square? = null
        for (i in 0..63) {
            val p = board[i]
            if (p != null && p.type == PieceType.KING && p.color == kingColor) {
                kingSquare = Square.fromIndex(i)
                break
            }
        }
        if (kingSquare == null) return false
        return isSquareAttackedBy(kingSquare, kingColor.opponent)
    }

    fun isSquareAttackedBy(sq: Square, attackerColor: PieceColor): Boolean {
        // Pawn attacks
        val pawnDir = if (attackerColor == PieceColor.WHITE) -1 else 1
        for (df in listOf(-1, 1)) {
            val pSq = Square(sq.file + df, sq.rank + pawnDir)
            if (pSq.isValid) {
                val p = pieceAt(pSq)
                if (p != null && p.color == attackerColor && p.type == PieceType.PAWN) return true
            }
        }

        // Knight attacks
        val knightOffsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (off in knightOffsets) {
            val nSq = Square(sq.file + off.first, sq.rank + off.second)
            if (nSq.isValid) {
                val p = pieceAt(nSq)
                if (p != null && p.color == attackerColor && p.type == PieceType.KNIGHT) return true
            }
        }

        // King attacks
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val kSq = Square(sq.file + df, sq.rank + dr)
                if (kSq.isValid) {
                    val p = pieceAt(kSq)
                    if (p != null && p.color == attackerColor && p.type == PieceType.KING) return true
                }
            }
        }

        // Diagonal rays (Bishop, Queen)
        val bishopRays = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        for (ray in bishopRays) {
            var cur = Square(sq.file + ray.first, sq.rank + ray.second)
            while (cur.isValid) {
                val p = pieceAt(cur)
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                cur = Square(cur.file + ray.first, cur.rank + ray.second)
            }
        }

        // Straight rays (Rook, Queen)
        val rookRays = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        for (ray in rookRays) {
            var cur = Square(sq.file + ray.first, sq.rank + ray.second)
            while (cur.isValid) {
                val p = pieceAt(cur)
                if (p != null) {
                    if (p.color == attackerColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                cur = Square(cur.file + ray.first, cur.rank + ray.second)
            }
        }

        return false
    }

    private fun generatePseudoLegalMoves(): List<ChessMove> {
        val moves = ArrayList<ChessMove>(40)
        for (i in 0..63) {
            val piece = board[i] ?: continue
            if (piece.color != activeColor) continue
            val from = Square.fromIndex(i)

            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(from, piece, moves)
                PieceType.KNIGHT -> generateKnightMoves(from, piece, moves)
                PieceType.BISHOP -> generateRayMoves(from, piece, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
                PieceType.ROOK -> generateRayMoves(from, piece, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
                PieceType.QUEEN -> generateRayMoves(from, piece, listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                ), moves)
                PieceType.KING -> generateKingMoves(from, piece, moves)
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Square, piece: ChessPiece, moves: MutableList<ChessMove>) {
        val dir = piece.color.direction
        val startRank = piece.color.pawnStartRank
        val promoRank = piece.color.promotionRank

        // 1 step forward
        val oneStep = Square(from.file, from.rank + dir)
        if (oneStep.isValid && pieceAt(oneStep) == null) {
            if (oneStep.rank == promoRank) {
                for (pType in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    moves.add(ChessMove(from = from, to = oneStep, pieceMoved = PieceType.PAWN, promotion = pType))
                }
            } else {
                moves.add(ChessMove(from = from, to = oneStep, pieceMoved = PieceType.PAWN))
                // 2 steps forward
                if (from.rank == startRank) {
                    val twoStep = Square(from.file, from.rank + dir * 2)
                    if (pieceAt(twoStep) == null) {
                        moves.add(ChessMove(from = from, to = twoStep, pieceMoved = PieceType.PAWN))
                    }
                }
            }
        }

        // Captures
        for (df in listOf(-1, 1)) {
            val target = Square(from.file + df, from.rank + dir)
            if (target.isValid) {
                val targetPiece = pieceAt(target)
                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (target.rank == promoRank) {
                        for (pType in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(ChessMove(
                                from = from,
                                to = target,
                                pieceMoved = PieceType.PAWN,
                                promotion = pType,
                                isCapture = true,
                                capturedPiece = targetPiece.type
                            ))
                        }
                    } else {
                        moves.add(ChessMove(
                            from = from,
                            to = target,
                            pieceMoved = PieceType.PAWN,
                            isCapture = true,
                            capturedPiece = targetPiece.type
                        ))
                    }
                } else if (enPassantTarget != null && target == enPassantTarget) {
                    moves.add(ChessMove(
                        from = from,
                        to = target,
                        pieceMoved = PieceType.PAWN,
                        isCapture = true,
                        isEnPassant = true,
                        capturedPiece = PieceType.PAWN
                    ))
                }
            }
        }
    }

    private fun generateKnightMoves(from: Square, piece: ChessPiece, moves: MutableList<ChessMove>) {
        val offsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (off in offsets) {
            val to = Square(from.file + off.first, from.rank + off.second)
            if (to.isValid) {
                val target = pieceAt(to)
                if (target == null) {
                    moves.add(ChessMove(from = from, to = to, pieceMoved = PieceType.KNIGHT))
                } else if (target.color != piece.color) {
                    moves.add(ChessMove(
                        from = from,
                        to = to,
                        pieceMoved = PieceType.KNIGHT,
                        isCapture = true,
                        capturedPiece = target.type
                    ))
                }
            }
        }
    }

    private fun generateRayMoves(from: Square, piece: ChessPiece, rays: List<Pair<Int, Int>>, moves: MutableList<ChessMove>) {
        for (ray in rays) {
            var cur = Square(from.file + ray.first, from.rank + ray.second)
            while (cur.isValid) {
                val target = pieceAt(cur)
                if (target == null) {
                    moves.add(ChessMove(from = from, to = cur, pieceMoved = piece.type))
                } else {
                    if (target.color != piece.color) {
                        moves.add(ChessMove(
                            from = from,
                            to = cur,
                            pieceMoved = piece.type,
                            isCapture = true,
                            capturedPiece = target.type
                        ))
                    }
                    break
                }
                cur = Square(cur.file + ray.first, cur.rank + ray.second)
            }
        }
    }

    private fun generateKingMoves(from: Square, piece: ChessPiece, moves: MutableList<ChessMove>) {
        for (df in -1..1) {
            for (dr in -1..1) {
                if (df == 0 && dr == 0) continue
                val to = Square(from.file + df, from.rank + dr)
                if (to.isValid) {
                    val target = pieceAt(to)
                    if (target == null) {
                        moves.add(ChessMove(from = from, to = to, pieceMoved = PieceType.KING))
                    } else if (target.color != piece.color) {
                        moves.add(ChessMove(
                            from = from,
                            to = to,
                            pieceMoved = PieceType.KING,
                            isCapture = true,
                            capturedPiece = target.type
                        ))
                    }
                }
            }
        }

        // Castling
        val isWhite = piece.color == PieceColor.WHITE
        val rank = piece.color.backRank
        val opp = piece.color.opponent

        if (from.file == 4 && from.rank == rank && !isKingInCheck(piece.color)) {
            // Kingside (O-O)
            val canKingside = if (isWhite) castlingRights.whiteKingside else castlingRights.blackKingside
            if (canKingside) {
                val f = Square(5, rank)
                val g = Square(6, rank)
                val rookSq = Square(7, rank)
                val rook = pieceAt(rookSq)
                if (rook?.type == PieceType.ROOK && rook.color == piece.color &&
                    pieceAt(f) == null && pieceAt(g) == null &&
                    !isSquareAttackedBy(f, opp) && !isSquareAttackedBy(g, opp)
                ) {
                    moves.add(ChessMove(
                        from = from,
                        to = g,
                        pieceMoved = PieceType.KING,
                        isCastleKingside = true
                    ))
                }
            }

            // Queenside (O-O-O)
            val canQueenside = if (isWhite) castlingRights.whiteQueenside else castlingRights.blackQueenside
            if (canQueenside) {
                val d = Square(3, rank)
                val c = Square(2, rank)
                val b = Square(1, rank)
                val rookSq = Square(0, rank)
                val rook = pieceAt(rookSq)
                if (rook?.type == PieceType.ROOK && rook.color == piece.color &&
                    pieceAt(d) == null && pieceAt(c) == null && pieceAt(b) == null &&
                    !isSquareAttackedBy(d, opp) && !isSquareAttackedBy(c, opp)
                ) {
                    moves.add(ChessMove(
                        from = from,
                        to = c,
                        pieceMoved = PieceType.KING,
                        isCastleQueenside = true
                    ))
                }
            }
        }
    }

    private fun makeMoveRaw(move: ChessMove): ChessPosition {
        val nextBoard = ArrayList(board)
        val movingPiece = nextBoard[move.from.index]
        nextBoard[move.from.index] = null

        when {
            move.isCastleKingside -> {
                nextBoard[move.to.index] = movingPiece
                val rank = move.from.rank
                val rook = nextBoard[rank * 8 + 7]
                nextBoard[rank * 8 + 7] = null
                nextBoard[rank * 8 + 5] = rook
            }
            move.isCastleQueenside -> {
                nextBoard[move.to.index] = movingPiece
                val rank = move.from.rank
                val rook = nextBoard[rank * 8 + 0]
                nextBoard[rank * 8 + 0] = null
                nextBoard[rank * 8 + 3] = rook
            }
            move.isEnPassant -> {
                nextBoard[move.to.index] = movingPiece
                val epCapturedRank = move.from.rank
                nextBoard[epCapturedRank * 8 + move.to.file] = null
            }
            move.promotion != null -> {
                nextBoard[move.to.index] = ChessPiece(move.promotion, activeColor)
            }
            else -> {
                nextBoard[move.to.index] = movingPiece
            }
        }

        // Update castling rights
        var cRights = castlingRights
        if (movingPiece?.type == PieceType.KING) {
            cRights = if (activeColor == PieceColor.WHITE) {
                cRights.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                cRights.copy(blackKingside = false, blackQueenside = false)
            }
        }
        if (move.from.index == 0 || move.to.index == 0) cRights = cRights.copy(whiteQueenside = false)
        if (move.from.index == 7 || move.to.index == 7) cRights = cRights.copy(whiteKingside = false)
        if (move.from.index == 56 || move.to.index == 56) cRights = cRights.copy(blackQueenside = false)
        if (move.from.index == 63 || move.to.index == 63) cRights = cRights.copy(blackKingside = false)

        // En passant target
        val newEpTarget = if (movingPiece?.type == PieceType.PAWN && abs(move.to.rank - move.from.rank) == 2) {
            Square(move.from.file, (move.from.rank + move.to.rank) / 2)
        } else null

        // Halfmove clock (50-move rule)
        val newHalfmove = if (movingPiece?.type == PieceType.PAWN || move.isCapture) 0 else halfmoveClock + 1

        val nextColor = activeColor.opponent
        val nextFullmove = if (activeColor == PieceColor.BLACK) fullmoveNumber + 1 else fullmoveNumber

        return ChessPosition(
            board = nextBoard,
            activeColor = nextColor,
            castlingRights = cRights,
            enPassantTarget = newEpTarget,
            halfmoveClock = newHalfmove,
            fullmoveNumber = nextFullmove
        )
    }

    fun makeMove(move: ChessMove): ChessPosition = makeMoveRaw(move)

    private fun computeSan(move: ChessMove, pseudoMoves: List<ChessMove>): String {
        if (move.isCastleKingside) return "O-O"
        if (move.isCastleQueenside) return "O-O-O"

        val sb = StringBuilder()
        if (move.pieceMoved != PieceType.PAWN) {
            sb.append(move.pieceMoved.sanLetter)

            // Disambiguation
            val duplicates = pseudoMoves.filter {
                it.pieceMoved == move.pieceMoved &&
                it.to == move.to &&
                it.from != move.from
            }
            if (duplicates.isNotEmpty()) {
                val sameFile = duplicates.any { it.from.file == move.from.file }
                val sameRank = duplicates.any { it.from.rank == move.from.rank }
                if (!sameFile) {
                    sb.append(('a'.code + move.from.file).toChar())
                } else if (!sameRank) {
                    sb.append((move.from.rank + 1).toString())
                } else {
                    sb.append(move.from.algebraic)
                }
            }
        } else if (move.isCapture) {
            sb.append(('a'.code + move.from.file).toChar())
        }

        if (move.isCapture) {
            sb.append('x')
        }

        sb.append(move.to.algebraic)

        if (move.promotion != null) {
            sb.append('=').append(move.promotion.sanLetter)
        }

        val nextPos = makeMoveRaw(move)
        val opp = activeColor.opponent
        if (nextPos.isKingInCheck(opp)) {
            val replies = nextPos.generateLegalMoves()
            if (replies.isEmpty()) {
                sb.append('#')
            } else {
                sb.append('+')
            }
        }

        return sb.toString()
    }

    fun isInsufficientMaterial(): Boolean {
        var whiteKnights = 0
        var blackKnights = 0
        var whiteBishops = 0
        var blackBishops = 0
        var otherPieces = 0

        for (p in board) {
            if (p == null) continue
            when (p.type) {
                PieceType.PAWN, PieceType.ROOK, PieceType.QUEEN -> return false
                PieceType.KNIGHT -> if (p.color == PieceColor.WHITE) whiteKnights++ else blackKnights++
                PieceType.BISHOP -> if (p.color == PieceColor.WHITE) whiteBishops++ else blackBishops++
                PieceType.KING -> {}
            }
        }

        val whiteMinors = whiteKnights + whiteBishops
        val blackMinors = blackKnights + blackBishops

        // King vs King
        if (whiteMinors == 0 && blackMinors == 0) return true
        // King + Minor vs King
        if ((whiteMinors == 1 && blackMinors == 0) || (blackMinors == 1 && whiteMinors == 0)) return true

        return false
    }

    companion object {
        fun initial(): ChessPosition {
            return fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1")!!
        }

        fun fromFen(fen: String): ChessPosition? {
            try {
                val parts = fen.trim().split(Regex("\\s+"))
                if (parts.size < 4) return null

                val rows = parts[0].split('/')
                if (rows.size != 8) return null

                val newBoard = MutableList<ChessPiece?>(64) { null }

                for (rankIdx in 0..7) {
                    val rank = 7 - rankIdx
                    val rowStr = rows[rankIdx]
                    var file = 0
                    for (ch in rowStr) {
                        if (ch.isDigit()) {
                            file += ch.digitToInt()
                        } else {
                            val piece = ChessPiece.fromFenChar(ch)
                            if (piece != null && file in 0..7) {
                                newBoard[rank * 8 + file] = piece
                                file++
                            }
                        }
                    }
                }

                val active = if (parts[1].lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE
                val castling = CastlingRights.fromFen(parts[2])
                val epTarget = if (parts[3] != "-") Square.fromAlgebraic(parts[3]) else null
                val halfmove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
                val fullmove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

                return ChessPosition(
                    board = newBoard,
                    activeColor = active,
                    castlingRights = castling,
                    enPassantTarget = epTarget,
                    halfmoveClock = halfmove,
                    fullmoveNumber = fullmove
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}
