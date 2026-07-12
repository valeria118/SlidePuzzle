package com.example.a1234567889.logic

import com.example.a1234567889.models.*
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {

    @Test
    fun testSolvedBoardNormal() {
        val target = GameEngine.createTargetBoard(3, ChallengeMode.NORMAL, GameMode.NUMBERS)
        assertTrue(GameEngine.isSolved(target, target))
        assertEquals(0, target.tiles[2][2].number)
        assertTrue(target.tiles[2][2].isEmpty)
        assertEquals(1, target.tiles[0][0].number)
    }

    @Test
    fun testSolvedBoardReverse() {
        val target = GameEngine.createTargetBoard(3, ChallengeMode.REVERSE, GameMode.NUMBERS)
        // Reverse 3x3: empty is at [2,2] based on createReverseTarget implementation
        assertEquals(0, target.tiles[2][2].number)
        assertTrue(target.tiles[2][2].isEmpty)
        assertEquals(8, target.tiles[0][0].number)
        assertEquals(1, target.tiles[2][1].number)
    }

    @Test
    fun testSolvedBoardSpiral() {
        val target = GameEngine.createTargetBoard(3, ChallengeMode.SPIRAL_IN, GameMode.NUMBERS)
        // Spiral In 3x3:
        // 1 2 3
        // 8 0 4
        // 7 6 5
        assertEquals(1, target.tiles[0][0].number)
        assertEquals(2, target.tiles[0][1].number)
        assertEquals(3, target.tiles[0][2].number)
        assertEquals(4, target.tiles[1][2].number)
        assertEquals(5, target.tiles[2][2].number)
        assertEquals(6, target.tiles[2][1].number)
        assertEquals(7, target.tiles[2][0].number)
        assertEquals(8, target.tiles[1][0].number)
        assertEquals(0, target.tiles[1][1].number)
    }

    @Test
    fun testMoveLogic() {
        val target = GameEngine.createTargetBoard(3, ChallengeMode.NORMAL, GameMode.NUMBERS)
        val board = target
        // Empty is at (2,2). Adjacent are (1,2) and (2,1)
        val newBoard = GameEngine.moveTile(board, Position(1, 2))
        assertNotNull(newBoard)
        assertTrue(newBoard!!.tiles[1][2].isEmpty)
        assertFalse(newBoard.tiles[2][2].isEmpty)
        assertEquals(6, newBoard.tiles[2][2].number)
        
        // Non-adjacent move should return null
        val invalidBoard = GameEngine.moveTile(board, Position(0, 0))
        assertNull(invalidBoard)
    }

    @Test
    fun testShuffleSolvability() {
        val target = GameEngine.createTargetBoard(3, ChallengeMode.NORMAL, GameMode.NUMBERS)
        val shuffled = GameEngine.shuffle(3, ChallengeMode.NORMAL, GameMode.NUMBERS)
        assertFalse(GameEngine.isSolved(shuffled, target))
        
        // Check that all numbers 0-8 are present exactly once
        val numbers = shuffled.tiles.flatten().map { it.number }.sorted()
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), numbers)
    }

    @Test
    fun testDerangementGuarantee() {
        val size = 3
        val target = GameEngine.createTargetBoard(size, ChallengeMode.NORMAL, GameMode.NUMBERS)
        
        // Run multiple times to ensure it's not just luck
        repeat(10) {
            val shuffled = GameEngine.shuffle(
                size = size,
                challenge = ChallengeMode.NORMAL,
                mode = GameMode.NUMBERS,
                allowMatches = false
            )
            assertTrue("Should be deranged", GameEngine.isDeranged(shuffled, target))
        }
    }
}
