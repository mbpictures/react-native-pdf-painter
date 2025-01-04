package com.pdfannotation.viewer

import androidx.compose.ui.geometry.Size

fun Size.aspectRatio(): Float = width / height

fun calculateChildSize(containerSize: Size, childSize: Size): Size {
    val childAspectRatio = childSize.aspectRatio()
    val containerAspectRatio = containerSize.aspectRatio()

    return if (containerAspectRatio > childAspectRatio) {
        // Fit by height
        val width = containerSize.height * childAspectRatio
        Size(width, containerSize.height)
    } else {
        // Fit by width
        val height = containerSize.width / childAspectRatio
        Size(containerSize.width, height)
    }
}
