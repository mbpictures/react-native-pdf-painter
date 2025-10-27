package com.pdfannotation.viewer

import android.graphics.Matrix
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.ink.authoring.InProgressStrokesView
import com.pdfannotation.canvas.InkCanvas
import com.pdfannotation.canvas.StrokeAuthoringState
import com.pdfannotation.canvas.rememberStrokeAuthoringState
import com.pdfannotation.canvas.rememberStrokeAuthoringTouchListener
import com.pdfannotation.model.BrushSettings
import com.pdfannotation.model.Link
import com.pdfannotation.model.Strokes
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PdfPage(
    inProgressStrokesView: InProgressStrokesView,
    page: PdfRender.Page?,
    backgroundColor: Int?,
    brushSettings: BrushSettings?,
    viewModel: Strokes,
    links: Set<Link>,
    onChangePage: (Int) -> Unit = {},
    onLink: (Link) -> Unit = {},
    onLinkRemove: (Link) -> Unit = {},
    onTap: (Float, Float, Float, Float, Boolean) -> Unit = { _, _, _, _, _ -> },
    findPage: (String) -> Int,
    containerSize: IntSize,
    setTransformMatrix: (Float, Float, Float) -> Unit,
    setChildSize: (Size) -> Unit,
    state: PagerState,
) {
    page?.pageContent?.collectAsState()?.value?.let { bitmap ->
        var size by remember { mutableStateOf(IntSize.Zero) }
        val baseScale = remember (size) {
            if (size.width == 0 || size.height == 0) return@remember 1f
            val imageSize = calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            maxOf(size.width.toFloat() / imageSize.width, 1.0f)
        }
        var zoomState by remember { mutableStateOf(ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()), maxScale = baseScale * 5.0f)) }
        val imageModel = remember(bitmap) {
            if (bitmap.isRecycled) {
                null
            } else {
                bitmap.asImageBitmap()
            }
        }
        val transformMatrix = remember(zoomState.scale) {
            Matrix().apply {
                preScale(1 / zoomState.scale, 1 / zoomState.scale)
            }
        }
        val strokeAuthoringState: StrokeAuthoringState = rememberStrokeAuthoringState(
            inProgressStrokesView,
            transformMatrix,
            brushSettings,
            strokesFinishedListener = { strokes ->
                if (state.currentPage == page.index) {
                    viewModel.setStrokesPerPage(
                        page.index,
                        strokes,
                        calculateChildSize(
                            size.toSize(),
                            Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                        )
                    )
                }
            }
        )
        val strokeAuthoringTouchListener = rememberStrokeAuthoringTouchListener(
            strokeAuthoringState = strokeAuthoringState,
            brushSettings = brushSettings,
            transformMatrix = transformMatrix,
        )

        // Restore strokes from ViewModel
        LaunchedEffect(viewModel, page, size, bitmap.height, bitmap.width) {
            strokeAuthoringState.finishedStrokes.value = viewModel.getStrokes(
                page.index,
                calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            )
        }

        LaunchedEffect(baseScale) {
            zoomState = ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()), maxScale = baseScale * 5.0f)
        }

        LaunchedEffect(zoomState.scale, zoomState.offsetY, zoomState.offsetX) {
            /*setTransformMatrix(Matrix().apply {
                preScale(zoomState.scale, zoomState.scale)
                preTranslate(zoomState.offsetX, zoomState.offsetY)
            })*/
            setTransformMatrix(zoomState.scale, zoomState.offsetX, zoomState.offsetY)
        }

        LaunchedEffect(bitmap.height, bitmap.width) {
            setChildSize(Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
        }


        CustomViewConfiguration(
            doubleTapTimeoutMillis = 100L
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .aspectRatio(Size(bitmap.width.toFloat(), bitmap.height.toFloat()).aspectRatio(), matchHeightConstraintsFirst = containerSize.width > containerSize.height)
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
                when (imageModel) {
                    null ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(backgroundColor ?: 0xFFFFFFFF.toInt()))
                        )
                    else -> {
                        Image(
                            bitmap = imageModel,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        InkCanvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                                .pointerInteropFilter { event ->
                                    strokeAuthoringTouchListener?.onTouch(inProgressStrokesView, event) ?: false
                                },
                            strokeAuthoringState = strokeAuthoringState,
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
        }
    }
}
