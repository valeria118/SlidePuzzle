package com.example.a1234567889.logic

import com.example.a1234567889.models.ImageGalleryItem

/**
 * Provides the built-in "Картинка" puzzles. Each board size (3x3 .. 8x8) has its
 * own set of pictures, bundled as drawable resources (puzzle_<size>_<size>_<n>),
 * generated from the project's "pazly/<size>_<size>" folder.
 */
object PictureProvider {

    private const val PACKAGE_NAME = "com.example.a1234567889"

    /** Builds a URI Coil/Android can load directly from a drawable resource name. */
    fun resUri(drawableName: String): String = "android.resource://$PACKAGE_NAME/drawable/$drawableName"

    val availableSizes: List<Int> = (3..8).toList()

    private const val IMAGES_PER_SIZE = 3

    private fun titleFor(size: Int, index: Int): String = "Картинка $size×$size · №$index"

    private fun buildItemsForSize(size: Int): List<ImageGalleryItem> =
        (1..IMAGES_PER_SIZE).map { index ->
            ImageGalleryItem(
                id = "pazly_${size}_${size}_$index",
                title = titleFor(size, index),
                uri = resUri("puzzle_${size}_${size}_$index"),
                category = "$size×$size",
                isCustom = false,
                boardSize = size
            )
        }

    /** size (3..8) -> list of built-in pictures for that exact board size. */
    val builtInBySize: Map<Int, List<ImageGalleryItem>> = availableSizes.associateWith { buildItemsForSize(it) }

    /** Flat list of every built-in picture, across all board sizes. */
    val builtInImages: List<ImageGalleryItem> = builtInBySize.values.flatten()

    fun imagesForSize(size: Int): List<ImageGalleryItem> = builtInBySize[size] ?: emptyList()

    fun isBuiltInUri(uri: String): Boolean = builtInImages.any { it.uri == uri }

    /** Returns true if the image URI belongs to a built-in puzzle of the specified size. */
    fun isValidForSize(uri: String, size: Int): Boolean =
        imagesForSize(size).any { it.uri == uri }

    /** Returns the first available built-in image for a specific board size. */
    fun defaultImageForSize(size: Int): ImageGalleryItem? =
        imagesForSize(size).firstOrNull()
}
