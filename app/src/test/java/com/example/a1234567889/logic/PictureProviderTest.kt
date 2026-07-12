package com.example.a1234567889.logic

import org.junit.Assert.*
import org.junit.Test

class PictureProviderTest {

    @Test
    fun testImagesForSize() {
        val images3 = PictureProvider.imagesForSize(3)
        assertEquals(3, images3.size)
        images3.forEach { 
            assertEquals(3, it.boardSize)
            assertTrue(it.uri.contains("puzzle_3_3_"))
        }

        val images8 = PictureProvider.imagesForSize(8)
        assertEquals(3, images8.size)
        images8.forEach {
            assertEquals(8, it.boardSize)
            assertTrue(it.uri.contains("puzzle_8_8_"))
        }
    }

    @Test
    fun testIsValidForSize() {
        val uri3 = "android.resource://com.example.a1234567889/drawable/puzzle_3_3_1"
        assertTrue(PictureProvider.isValidForSize(uri3, 3))
        assertFalse(PictureProvider.isValidForSize(uri3, 4))
        
        val uri8 = "android.resource://com.example.a1234567889/drawable/puzzle_8_8_3"
        assertTrue(PictureProvider.isValidForSize(uri8, 8))
        assertFalse(PictureProvider.isValidForSize(uri8, 7))
    }

    @Test
    fun testDefaultImageForSize() {
        val default3 = PictureProvider.defaultImageForSize(3)
        assertNotNull(default3)
        assertEquals("pazly_3_3_1", default3?.id)
        
        val default8 = PictureProvider.defaultImageForSize(8)
        assertNotNull(default8)
        assertEquals("pazly_8_8_1", default8?.id)
        
        val defaultInvalid = PictureProvider.defaultImageForSize(10)
        assertNull(defaultInvalid)
    }
}
