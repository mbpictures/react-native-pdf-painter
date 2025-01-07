package com.pdfannotation.viewer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

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
            val context = LocalContext.current
            val imageModel = remember(context, bitmap) {
                ImageRequest.Builder(context)
                    .data(bitmap)
                    .build()
            }

            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}