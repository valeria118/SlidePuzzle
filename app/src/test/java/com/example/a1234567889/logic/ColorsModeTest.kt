package com.example.a1234567889.logic

import com.example.a1234567889.models.*
import org.junit.Assert.*
import org.junit.Test

class ColorsModeTest {

    @Test
    fun testRainbowPaletteGeneration() {
        val size = 3
        val board = GameEngine.createTargetBoard(size, ChallengeMode.NORMAL, GameMode.COLORS, ColorPaletteType.RAINBOW, ColorPattern.SPECTRUM)
        
        assertEquals(size, board.size)
        val tiles = board.tiles.flatten()
        val colorTiles = tiles.filter { !it.isEmpty }
        
        assertEquals(8, colorTiles.size)
        // Check that colors are not null
        colorTiles.forEach { assertNotNull(it.color) }
        
        // Rainbow should have varying hues
        val hues = colorTiles.map { tile ->
            val color = tile.color!!
            val max = maxOf(color.red, maxOf(color.green, color.blue))
            val min = minOf(color.red, minOf(color.green, color.blue))
            val delta = max - min
            var h = when {
                delta == 0f -> 0f
                max == color.red -> 60f * (((color.green - color.blue) / delta) % 6f)
                max == color.green -> 60f * (((color.blue - color.red) / delta) + 2f)
                else -> 60f * (((color.red - color.green) / delta) + 4f)
            }
            if (h < 0f) h += 360f
            h
        }
        // First tile hue should be less than last tile hue (roughly)
        assertTrue(hues.first() < hues.last())
    }

    @Test
    fun testSpiralPattern() {
        val size = 3
        val board = GameEngine.createTargetBoard(size, ChallengeMode.NORMAL, GameMode.COLORS, ColorPaletteType.RAINBOW, ColorPattern.SPIRAL)
        
        // For 3x3 Spiral:
        // (0,0) (0,1) (0,2)
        // (1,2) (2,2) (2,1)
        // (2,0) (1,0) (1,1) <- last is empty
        
        assertTrue(board.tiles[1][1].isEmpty)
        assertFalse(board.tiles[0][0].isEmpty)
        assertFalse(board.tiles[1][0].isEmpty)
    }

    @Test
    fun testChessboardPattern() {
        val size = 3
        val board = GameEngine.createTargetBoard(size, ChallengeMode.NORMAL, GameMode.COLORS, ColorPaletteType.CHESSBOARD, ColorPattern.CHESSBOARD)
        
        // Chessboard positions: evens then odds
        // (0,0) (0,2) (1,1) (2,0) (2,2) then (0,1) (1,0) (1,2) (2,1)
        // Last one (2,1) should be empty if it's the 9th position
        
        assertTrue(board.tiles[2][1].isEmpty)
    }
}
