package com.pdfannotation.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun PdfAnnotationViewer(viewModel: PdfAnnotationViewModel) {
    val thumbnail by viewModel.thumbnailMode.collectAsState()

    if (thumbnail) {
        PdfThumbnail(viewModel)
    } else {
        PdfHorizontalPager(viewModel)
    }
}