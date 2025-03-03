package com.pdfannotation.viewer

import android.graphics.Matrix
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.ink.authoring.InProgressStrokesView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pdfannotation.canvas.InkCanvas
import com.pdfannotation.canvas.StrokeAuthoringState
import com.pdfannotation.canvas.rememberInProgressStrokesView
import com.pdfannotation.canvas.rememberStrokeAuthoringState
import com.pdfannotation.model.BrushSettings
import com.pdfannotation.model.Link
import com.pdfannotation.model.Strokes
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun PdfPage(
    page: PdfRender.Page?,
    brushSettings: BrushSettings?,
    viewModel: Strokes,
    links: Set<Link>,
    onChangePage: (Int) -> Unit = {},
    onLink: (Link) -> Unit = {},
    onLinkRemove: (Link) -> Unit = {},
    onTap: (Float, Float, Float, Float, Boolean) -> Unit = { _, _, _, _, _ -> },
    findPage: (String) -> Int
) {
    page?.pageContent?.collectAsState()?.value?.let { bitmap ->
        var size by remember { mutableStateOf(IntSize.Zero) }
        val baseScale = remember (size) {
            if (size.width == 0 || size.height == 0) return@remember 1f
            val imageSize = calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            maxOf(size.width.toFloat() / imageSize.width, 1.0f)
        }
        var zoomState by remember { mutableStateOf(ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()), maxScale = baseScale * 5.0f)) }
        val context = LocalContext.current
        val imageModel = remember(context, bitmap) {
            if (bitmap.isRecycled) {
                null
            } else {
                ImageRequest.Builder(context)
                    .data(bitmap)
                    .build()
            }
        }
        val transformMatrix = remember(zoomState.scale, zoomState.offsetY, zoomState.offsetX) {
            Matrix().apply {
                preScale(1 / zoomState.scale, 1 / zoomState.scale)
            }
        }
        val inProgressStrokesView: InProgressStrokesView = rememberInProgressStrokesView()
        val strokeAuthoringState: StrokeAuthoringState = rememberStrokeAuthoringState(inProgressStrokesView, transformMatrix)

        // Save finished strokes to ViewModel
        LaunchedEffect(strokeAuthoringState.finishedStrokes.value) {
            viewModel.setStrokesPerPage(
                page.index,
                strokeAuthoringState.finishedStrokes.value,
                calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            )
        }

        // Restore strokes from ViewModel
        LaunchedEffect(viewModel, page, size) {
            strokeAuthoringState.finishedStrokes.value = viewModel.getStrokes(
                page.index,
                calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            )
        }

        LaunchedEffect(baseScale) {
            zoomState = ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()), maxScale = baseScale * 5.0f)
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = it }
                .aspectRatio(Size(bitmap.width.toFloat(), bitmap.height.toFloat()).aspectRatio(), matchHeightConstraintsFirst = size.width > size.height)
                .zoomable(
                    zoomState,
                    onTap = { offset ->
                        var changePage = false
                        if (offset.x > size.width * 0.8) {
                            onChangePage(1)
                            changePage = true
                        } else if (offset.x < size.width * 0.2) {
                            onChangePage(-1)
                            changePage = true
                        }
                        onTap(offset.x, offset.y, offset.x / size.width, offset.y / size.height, changePage)
                    },
                    onDoubleTap = { position ->
                        val isBitmapPortrait = bitmap.height >= bitmap.width
                        val isPortrait = size.height >= size.width
                        val targetScale = when {
                            isPortrait && isBitmapPortrait -> {
                                if (zoomState.scale == 1f) 2.5f else 1f
                            }
                            else -> {
                                when (zoomState.scale) {
                                    1f -> baseScale
                                    baseScale -> baseScale * 2.5f
                                    else -> 1f
                                }
                            }
                        }

                        zoomState.changeScale(targetScale, position)
                    },
                    zoomEnabled = brushSettings == null
                )
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            InkCanvas(
                brushSettings = brushSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                inProgressStrokesView = inProgressStrokesView,
                strokeAuthoringState = strokeAuthoringState,
                transformMatrix = transformMatrix,
            )
            RenderLinks(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
                links = links,
                onLinkClick = onLink,
                onLinkRemove = onLinkRemove,
                findPage = findPage
            )
        }
    }
}
