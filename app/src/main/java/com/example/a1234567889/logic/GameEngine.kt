package com.example.a1234567889.logic

import androidx.compose.ui.graphics.Color
import com.example.a1234567889.models.*
import kotlin.random.Random

object GameEngine {

    fun createTargetBoard(size: Int, challenge: ChallengeMode, mode: GameMode, paletteType: ColorPaletteType? = null, patternType: ColorPattern? = null, customPalette: List<Color> = emptyList(), seed: Long? = null): Board {
        if (mode == GameMode.COLORS && paletteType != null && patternType != null) {
            return createColorTarget(size, paletteType, patternType, customPalette)
        }
        return when (challenge) {
            ChallengeMode.NORMAL -> createNormalTarget(size, mode)
            ChallengeMode.REVERSE -> createReverseTarget(size, mode)
            ChallengeMode.SPIRAL_IN -> createSpiralTarget(size, mode, insideOut = false)
            ChallengeMode.SPIRAL_OUT -> createSpiralOutTarget(size, mode)
            ChallengeMode.MIRROR_V -> createMirrorTarget(size, mode, vertical = true)
            ChallengeMode.MIRROR_H -> createMirrorTarget(size, mode, vertical = false)
            ChallengeMode.TRANSPOSE -> createTransposeTarget(size, mode)
            ChallengeMode.CHESSBOARD -> createChessboardTarget(size, mode)
            ChallengeMode.MAGIC_SQUARE -> createMagicSquareTarget(size, mode)
            ChallengeMode.DIAGONAL -> createDiagonalTarget(size, mode)
            ChallengeMode.MODULO -> createModuloTarget(size, mode)
            ChallengeMode.SNAKE -> createSnakeTarget(size, mode)
            ChallengeMode.RANDOM -> createRandomTarget(size, mode, seed)
        }
    }

    private fun createColorTarget(size: Int, paletteType: ColorPaletteType, patternType: ColorPattern, customPalette: List<Color>): Board {
        val totalTiles = size * size
        val colors = if (customPalette.isNotEmpty()) {
            // Ensure custom palette has enough colors, pad with random if needed
            if (customPalette.size >= totalTiles) customPalette 
            else customPalette + generatePalette(totalTiles - customPalette.size, ColorPaletteType.RANDOM)
        } else {
            generatePalette(totalTiles, paletteType)
        }
        val matrix = Array(size) { arrayOfNulls<Tile>(size) }

        val positions = when (patternType) {
            ColorPattern.SPECTRUM -> (0 until totalTiles).map { Position(it / size, it % size) }
            ColorPattern.LINEAR -> (0 until totalTiles).map { Position(it % size, it / size) } // Column-major for contrast
            ColorPattern.CHESSBOARD -> generateChessboardPositions(size)
            ColorPattern.SPIRAL -> generateSpiralPositions(size)
            ColorPattern.DIAGONAL -> generateDiagonalPositions(size)
            ColorPattern.CONCENTRIC -> generateConcentricPositions(size) // "Rings" in guide
            ColorPattern.RANDOM -> (0 until totalTiles).map { Position(it / size, it % size) }.shuffled(Random(System.currentTimeMillis()))
        }

        for (i in 0 until totalTiles - 1) {
            val pos = positions[i]
            val num = i + 1
            matrix[pos.row][pos.col] = Tile(num, num, false, colors[i])
        }
        
        val emptyPos = positions.last()
        matrix[emptyPos.row][emptyPos.col] = Tile(0, 0, true, null)

        val tiles = matrix.map { row -> row.map { it!! } }
        return Board(size, tiles)
    }

    fun generatePalette(count: Int, type: ColorPaletteType): List<Color> {
        val randomSeed = Random.nextInt()
        return List(count) { i ->
            val ratio = i.toFloat() / (count - 1).coerceAtLeast(1)
            when (type) {
                ColorPaletteType.RAINBOW -> Color.hsv(ratio * 300f, 0.8f, 0.9f)
                ColorPaletteType.GRADIENT -> {
                    // Transition between two colors
                    Color(
                        red = 0.1f + ratio * 0.8f,
                        green = 0.2f + ratio * 0.6f,
                        blue = 0.9f - ratio * 0.4f
                    )
                }
                ColorPaletteType.MONOCHROME -> Color.hsv(210f, 0.1f + ratio * 0.8f, 0.4f + ratio * 0.5f)
                ColorPaletteType.PASTEL -> Color.hsv(ratio * 360f, 0.3f, 0.95f)
                ColorPaletteType.BRIGHT -> Color.hsv(ratio * 360f, 0.9f, 1.0f)
                ColorPaletteType.CHESSBOARD -> if (i % 2 == 0) Color(0xFF2C3E50) else Color(0xFFECF0F1)
                ColorPaletteType.RANDOM -> {
                    val hue = (ratio * 360f + randomSeed) % 360f
                    Color.hsv(hue, 0.6f + Random(i + randomSeed).nextFloat() * 0.4f, 0.7f + Random(i + randomSeed).nextFloat() * 0.3f)
                }
            }
        }
    }

    private fun Color.toHsv(): FloatArray {
        val hsv = FloatArray(3)
        val max = maxOf(red, maxOf(green, blue))
        val min = minOf(red, minOf(green, blue))
        val delta = max - min
        
        // Hue
        hsv[0] = when {
            delta == 0f -> 0f
            max == red -> 60f * (((green - blue) / delta) % 6f)
            max == green -> 60f * (((blue - red) / delta) + 2f)
            else -> 60f * (((red - green) / delta) + 4f)
        }
        if (hsv[0] < 0f) hsv[0] += 360f
        
        // Saturation
        hsv[1] = if (max == 0f) 0f else delta / max
        
        // Value
        hsv[2] = max

        return hsv
    }

    private fun generateChessboardPositions(size: Int): List<Position> {
        val all = (0 until size * size).map { Position(it / size, it % size) }
        val evens = all.filter { (it.row + it.col) % 2 == 0 }
        val odds = all.filter { (it.row + it.col) % 2 != 0 }
        return evens + odds
    }

    private fun generateSpiralPositions(size: Int): List<Position> {
        val positions = mutableListOf<Position>()
        var top = 0
        var bottom = size - 1
        var left = 0
        var right = size - 1
        while (top <= bottom && left <= right) {
            for (i in left..right) positions.add(Position(top, i))
            top++
            for (i in top..bottom) positions.add(Position(i, right))
            right--
            if (top <= bottom) {
                for (i in right downTo left) positions.add(Position(bottom, i))
                bottom--
            }
            if (left <= right) {
                for (i in bottom downTo top) positions.add(Position(i, left))
                left++
            }
        }
        return positions
    }

    private fun generateDiagonalPositions(size: Int): List<Position> {
        val positions = mutableListOf<Position>()
        for (sum in 0 until 2 * size - 1) {
            for (r in 0 until size) {
                val c = sum - r
                if (c in 0 until size) positions.add(Position(r, c))
            }
        }
        return positions
    }

    private fun generateConcentricPositions(size: Int): List<Position> {
        val positions = mutableListOf<Position>()
        val layers = (size + 1) / 2
        for (layer in 0 until layers) {
            val top = layer
            val bottom = size - 1 - layer
            val left = layer
            val right = size - 1 - layer
            if (top == bottom && left == right) {
                positions.add(Position(top, left))
            } else {
                for (i in left until right) positions.add(Position(top, i))
                for (i in top until bottom) positions.add(Position(i, right))
                for (i in right downTo left + 1) positions.add(Position(bottom, i))
                for (i in bottom downTo top + 1) positions.add(Position(i, left))
            }
        }
        return positions
    }

    private fun createNormalTarget(size: Int, mode: GameMode): Board {
        val tiles = mutableListOf<MutableList<Tile>>()
        var counter = 1
        for (r in 0 until size) {
            val row = mutableListOf<Tile>()
            for (c in 0 until size) {
                if (r == size - 1 && c == size - 1) {
                    row.add(Tile(id = 0, number = 0, isEmpty = true, color = getTileColor(0, size * size, mode)))
                } else {
                    row.add(Tile(id = counter, number = counter, color = getTileColor(counter, size * size, mode)))
                    counter++
                }
            }
            tiles.add(row)
        }
        return Board(size, tiles)
    }

    private fun createReverseTarget(size: Int, mode: GameMode): Board {
        val totalTiles = size * size
        val tiles = mutableListOf<MutableList<Tile>>()
        var counter = totalTiles - 1
        for (r in 0 until size) {
            val row = mutableListOf<Tile>()
            for (c in 0 until size) {
                if (r == size - 1 && c == size - 1) {
                    row.add(Tile(id = 0, number = 0, isEmpty = true, color = getTileColor(0, totalTiles, mode)))
                } else {
                    row.add(Tile(id = counter, number = counter, color = getTileColor(counter, totalTiles, mode)))
                    counter--
                }
            }
            tiles.add(row)
        }
        return Board(size, tiles)
    }

    private fun createSpiralTarget(size: Int, mode: GameMode, insideOut: Boolean): Board {
        val totalTiles = size * size
        val positions = generateSpiralPositions(size)
        val matrix = Array(size) { arrayOfNulls<Tile>(size) }

        for (i in 0 until totalTiles - 1) {
            val pos = positions[i]
            val num = if (insideOut) totalTiles - 1 - i else i + 1
            matrix[pos.row][pos.col] = Tile(num, num, false, getTileColor(num, totalTiles, mode))
        }
        
        val emptyPos = positions.last()
        matrix[emptyPos.row][emptyPos.col] = Tile(0, 0, true, getTileColor(0, totalTiles, mode))

        val tiles = matrix.map { row -> row.map { it!! } }
        return Board(size, tiles)
    }

    private fun createSpiralOutTarget(size: Int, mode: GameMode): Board {
        val totalTiles = size * size
        val tiles = MutableList(size) { MutableList(size) { Tile(0, 0, true) } }
        val center = (size - 1) / 2
        var r = center
        var c = center
        
        var stepSize = 1
        var stepsInDir = 0
        var dirIndex = 0 // 0: Right, 1: Down, 2: Left, 3: Up
        
        val dr = intArrayOf(0, 1, 0, -1)
        val dc = intArrayOf(1, 0, -1, 0)
        
        for (i in 1 until totalTiles) {
            if (r in 0 until size && c in 0 until size) {
                tiles[r][c] = Tile(i, i, false, getTileColor(i, totalTiles, mode))
            }
            
            r += dr[dirIndex]
            c += dc[dirIndex]
            stepsInDir++
            
            if (stepsInDir == stepSize) {
                stepsInDir = 0
                dirIndex = (dirIndex + 1) % 4
                if (dirIndex % 2 == 0) stepSize++
            }
        }
        
        return Board(size, tiles)
    }

    private fun createSnakeTarget(size: Int, mode: GameMode): Board {
        val totalTiles = size * size
        val tiles = MutableList(size) { MutableList(size) { Tile(0, 0, true) } }
        var current = 1
        for (r in 0 until size) {
            if (r % 2 == 0) {
                for (c in 0 until size) {
                    if (current < totalTiles) {
                        tiles[r][c] = Tile(current, current, false, getTileColor(current, totalTiles, mode))
                        current++
                    }
                }
            } else {
                for (c in size - 1 downTo 0) {
                    if (current < totalTiles) {
                        tiles[r][c] = Tile(current, current, false, getTileColor(current, totalTiles, mode))
                        current++
                    }
                }
            }
        }
        return Board(size, tiles)
    }

    private fun createMirrorTarget(size: Int, mode: GameMode, vertical: Boolean): Board {
        val normal = createNormalTarget(size, mode)
        val tiles = List(size) { r ->
            List(size) { c ->
                if (vertical) normal.tiles[r][size - 1 - c]
                else normal.tiles[size - 1 - r][c]
            }
        }
        return Board(size, tiles)
    }

    private fun createTransposeTarget(size: Int, mode: GameMode): Board {
        val normal = createNormalTarget(size, mode)
        val tiles = List(size) { r ->
            List(size) { c -> normal.tiles[c][r] }
        }
        return Board(size, tiles)
    }

    private fun createChessboardTarget(size: Int, mode: GameMode): Board {
        val tiles = mutableListOf<MutableList<Tile>>()
        var evens = (1 until size * size).filter { it % 2 == 0 }.toMutableList()
        var odds = (1 until size * size).filter { it % 2 != 0 }.toMutableList()
        
        for (r in 0 until size) {
            val row = mutableListOf<Tile>()
            for (c in 0 until size) {
                if (r == size - 1 && c == size - 1) {
                    row.add(Tile(0, 0, true, getTileColor(0, size * size, mode)))
                } else {
                    val num = if ((r + c) % 2 == 0) {
                        if (odds.isNotEmpty()) odds.removeAt(0) else evens.removeAt(0)
                    } else {
                        if (evens.isNotEmpty()) evens.removeAt(0) else odds.removeAt(0)
                    }
                    row.add(Tile(num, num, false, getTileColor(num, size * size, mode)))
                }
            }
            tiles.add(row)
        }
        return Board(size, tiles)
    }

    private fun createMagicSquareTarget(size: Int, mode: GameMode): Board {
        // Only supports 3x3 for now for simplicity, as higher sizes are complex to generate statically
        if (size != 3) return createNormalTarget(size, mode)
        
        val square = arrayOf(
            intArrayOf(8, 1, 6),
            intArrayOf(3, 5, 7),
            intArrayOf(4, 9, 2)
        )
        // Adjust for 0-8 range (empty tile is 0)
        val tiles = List(3) { r ->
            List(3) { c ->
                val num = square[r][c] - 1 // 1..9 -> 0..8
                Tile(num, num, num == 0, getTileColor(num, 9, mode))
            }
        }
        return Board(3, tiles)
    }

    private fun createDiagonalTarget(size: Int, mode: GameMode): Board {
        val totalTiles = size * size
        val positions = generateDiagonalPositions(size)
        val matrix = Array(size) { arrayOfNulls<Tile>(size) }

        for (i in 0 until totalTiles - 1) {
            val pos = positions[i]
            val num = i + 1
            matrix[pos.row][pos.col] = Tile(num, num, false, getTileColor(num, totalTiles, mode))
        }
        
        val emptyPos = positions.last()
        matrix[emptyPos.row][emptyPos.col] = Tile(0, 0, true, getTileColor(0, totalTiles, mode))

        val tiles = matrix.map { row -> row.map { it!! } }
        return Board(size, tiles)
    }

    private fun createModuloTarget(size: Int, mode: GameMode): Board {
        val max = size * size
        val mod3 = (1 until max).filter { it % 3 == 0 }
        val rest = (1 until max).filter { it % 3 != 0 }
        val all = mod3 + rest
        
        var idx = 0
        val tiles = List(size) { r ->
            List(size) { c ->
                if (r == size - 1 && c == size - 1) {
                    Tile(0, 0, true, getTileColor(0, max, mode))
                } else {
                    val num = all[idx++]
                    Tile(num, num, false, getTileColor(num, max, mode))
                }
            }
        }
        return Board(size, tiles)
    }

    private fun createRandomTarget(size: Int, mode: GameMode, seed: Long?): Board {
        val totalTiles = size * size
        val numbers = (1 until totalTiles).toList().shuffled(seed?.let { Random(it) } ?: Random)
        val matrix = Array(size) { arrayOfNulls<Tile>(size) }
        
        var idx = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (r == size - 1 && c == size - 1) {
                    matrix[r][c] = Tile(0, 0, true, getTileColor(0, totalTiles, mode))
                } else {
                    val num = numbers[idx++]
                    matrix[r][c] = Tile(num, num, false, getTileColor(num, totalTiles, mode))
                }
            }
        }
        val tiles = matrix.map { row -> row.map { it!! } }
        return Board(size, tiles)
    }

    private fun getTileColor(number: Int, total: Int, mode: GameMode): Color? {
        if (mode != GameMode.COLORS || number == 0) return null
        val hue = (number.toFloat() / total.toFloat()) * 360f
        return Color.hsv(hue, 0.7f, 0.9f)
    }

    fun isDeranged(board: Board, targetBoard: Board): Boolean {
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                val tile = board.tiles[r][c]
                if (tile.isEmpty) continue
                if (tile.id == targetBoard.tiles[r][c].id) return false
            }
        }
        return true
    }

    fun shuffle(
        size: Int,
        challenge: ChallengeMode,
        mode: GameMode,
        paletteType: ColorPaletteType? = null,
        patternType: ColorPattern? = null,
        customPalette: List<Color> = emptyList(),
        allowMatches: Boolean = false,
        customShuffleMoves: Int? = null,
        seed: Long? = null
    ): Board {
        val targetBoard = createTargetBoard(size, challenge, mode, paletteType, patternType, customPalette, seed)
        var board = targetBoard
        var emptyPos = board.findEmpty()
        
        // Calculate moves based on size and difficulty
        val tilesCount = size * size - 1
        val baseMoves = if (mode == GameMode.CUSTOM && customShuffleMoves != null) {
            customShuffleMoves
        } else {
            tilesCount * 4 // Default multiplier (previously MEDIUM)
        }
        
        val moves = baseMoves.coerceAtLeast(10)
        
        var lastPos = emptyPos
        var count = 0
        while (count < moves) {
            val neighbors = getNeighbors(emptyPos, size).filter { it != lastPos }
            if (neighbors.isNotEmpty()) {
                val nextPos = neighbors[Random.nextInt(neighbors.size)]
                board = swapTiles(board, emptyPos, nextPos)
                lastPos = emptyPos
                emptyPos = nextPos
                count++
            }
        }
        
        // Ensure derangement if not allowed to have matches
        if (!allowMatches && !isDeranged(board, targetBoard)) {
            // Do a few more moves (5-10) and check again, with a limit
            var safetyCounter = 0
            while (!isDeranged(board, targetBoard) && safetyCounter < 20) {
                repeat(5) {
                    val neighbors = getNeighbors(emptyPos, size).filter { it != lastPos }
                    if (neighbors.isNotEmpty()) {
                        val nextPos = neighbors[Random.nextInt(neighbors.size)]
                        board = swapTiles(board, emptyPos, nextPos)
                        lastPos = emptyPos
                        emptyPos = nextPos
                    }
                }
                safetyCounter++
            }
        }

        // Final check for solved state (though derangement usually prevents this)
        if (isSolved(board, targetBoard)) {
            return shuffle(size, challenge, mode, paletteType, patternType, customPalette, allowMatches, customShuffleMoves)
        }

        return board
    }

    fun moveTile(board: Board, tilePos: Position): Board? {
        val emptyPos = board.findEmpty()
        if (isAdjacent(tilePos, emptyPos)) {
            return swapTiles(board, tilePos, emptyPos)
        }
        return null
    }

    fun isSolved(board: Board, targetBoard: Board): Boolean {
        if (board.size != targetBoard.size) return false
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board.tiles[r][c].id != targetBoard.tiles[r][c].id) return false
            }
        }
        return true
    }

    fun countInversions(board: Board): Int {
        val flatList = board.tiles.flatten().filter { !it.isEmpty }.map { it.number }
        var inversions = 0
        for (i in 0 until flatList.size) {
            for (j in i + 1 until flatList.size) {
                if (flatList[i] > flatList[j]) inversions++
            }
        }
        return inversions
    }

    fun getManhattanDistance(board: Board, targetBoard: Board): Int {
        var distance = 0
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                val tile = board.tiles[r][c]
                if (!tile.isEmpty) {
                    val targetPos = findPositionInBoard(targetBoard, tile.id)
                    distance += Math.abs(r - targetPos.row) + Math.abs(c - targetPos.col)
                }
            }
        }
        return distance
    }

    private fun findPositionInBoard(board: Board, tileId: Int): Position {
        for (r in 0 until board.size) {
            for (c in 0 until board.size) {
                if (board.tiles[r][c].id == tileId) return Position(r, c)
            }
        }
        return Position(0, 0)
    }

    private fun swapTiles(board: Board, p1: Position, p2: Position): Board {
        val newTiles = board.tiles.map { it.toMutableList() }.toMutableList()
        val temp = newTiles[p1.row][p1.col]
        newTiles[p1.row][p1.col] = newTiles[p2.row][p2.col]
        newTiles[p2.row][p2.col] = temp
        return Board(board.size, newTiles)
    }

    private fun getNeighbors(pos: Position, size: Int): List<Position> {
        val neighbors = mutableListOf<Position>()
        if (pos.row > 0) neighbors.add(Position(pos.row - 1, pos.col))
        if (pos.row < size - 1) neighbors.add(Position(pos.row + 1, pos.col))
        if (pos.col > 0) neighbors.add(Position(pos.row, pos.col - 1))
        if (pos.col < size - 1) neighbors.add(Position(pos.row, pos.col + 1))
        return neighbors
    }

    private fun isAdjacent(p1: Position, p2: Position): Boolean {
        return (Math.abs(p1.row - p2.row) == 1 && p1.col == p2.col) ||
               (Math.abs(p1.col - p2.col) == 1 && p1.row == p2.row)
    }
}
