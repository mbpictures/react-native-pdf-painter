package com.pdfannotation.viewer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.pdfannotation.model.PdfAnnotationViewModel

@Composable
fun PdfThumbnail(viewModel: PdfAnnotationViewModel) {
    val file by viewModel.pdfFile.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val renderer = remember(file, backgroundColor) { file?.let {PdfRender(it, backgroundColor = backgroundColor) }}

    renderer?.pageLists?.getOrNull(0)?.let { page ->
        DisposableEffect(key1 = page.hash) {
            page.load()
            onDispose {
                page.recycle()
            }
        }

        page.pageContent.collectAsState().value.let { bitmap ->
            when (bitmap) {
                null ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(backgroundColor ?: 0xFFFFFFFF.toInt()))
                    )
                else -> {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}