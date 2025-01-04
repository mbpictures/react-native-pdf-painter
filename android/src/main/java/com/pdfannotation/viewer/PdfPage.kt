package com.pdfannotation.viewer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pdfannotation.canvas.InkCanvas
import com.pdfannotation.canvas.StrokeAction
import com.pdfannotation.canvas.StrokeAuthoringState
import com.pdfannotation.canvas.rememberInProgressStrokesView
import com.pdfannotation.canvas.rememberStrokeAuthoringState
import net.engawapg.lib.zoomable.ZoomState
import net.engawapg.lib.zoomable.zoomable

@Composable
fun PdfPage(
    page: PdfRender.Page,
    brushSettings: BrushSettings?,
    viewModel: SharedPdfPageViewModel,
    onChangePage: (Int) -> Unit = {}
) {
    DisposableEffect(key1 = page.hash) {
        page.load()
        onDispose {
            page.recycle()
        }
    }

    page.pageContent.collectAsState().value?.let { bitmap ->
        var size by remember { mutableStateOf(IntSize.Zero) }
        val baseScale = remember (size) {
            if (size.width == 0 || size.height == 0) return@remember 1f
            val imageSize = calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            maxOf(size.width.toFloat() / imageSize.width, 1.0f)
        }
        var zoomState by remember { mutableStateOf(ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()), maxScale = baseScale * 5.0f)) }
        val context = LocalContext.current
        val imageModel = remember(context, bitmap) {
            ImageRequest.Builder(context)
                .data(bitmap)
                .build()
        }
        val inProgressStrokesView: InProgressStrokesView = rememberInProgressStrokesView()
        val strokeAuthoringState: StrokeAuthoringState = rememberStrokeAuthoringState(inProgressStrokesView)

        // Save finished strokes to ViewModel
        LaunchedEffect(strokeAuthoringState.finishedStrokes.value) {
            viewModel.setStrokesPerPage(
                page.index,
                strokeAuthoringState.finishedStrokes.value,
                calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            )
        }

        // Restore strokes from ViewModel
        LaunchedEffect(page, size) {
            strokeAuthoringState.finishedStrokes.value = viewModel.getStrokes(
                page.index,
                calculateChildSize(size.toSize(), Size(bitmap.width.toFloat(), bitmap.height.toFloat()))
            )
        }

        LaunchedEffect(baseScale) {
            zoomState = ZoomState(contentSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()), maxScale = baseScale * 5.0f)
        }

        val originalViewConfiguration = LocalViewConfiguration.current
        val viewConfiguration = object : ViewConfiguration {
            override val doubleTapMinTimeMillis: Long
                get() = originalViewConfiguration.doubleTapMinTimeMillis
            override val doubleTapTimeoutMillis: Long
                get() = 100L
            override val longPressTimeoutMillis: Long
                get() = originalViewConfiguration.longPressTimeoutMillis
            override val touchSlop: Float
                get() = originalViewConfiguration.touchSlop // set this to any value you want

        }

        CompositionLocalProvider(LocalViewConfiguration provides viewConfiguration) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .zoomable(
                        zoomState,
                        onTap = { offset ->
                            if (offset.x > size.width * 0.8) {
                                onChangePage(1)
                            } else if (offset.x < size.width * 0.2) {
                                onChangePage(-1)
                            }
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
                    family = brushSettings?.family,
                    size = brushSettings?.size ?: 4f,
                    color = brushSettings?.color ?: Color.Black,
                    strokeActionInferer = { _ -> StrokeAction.Update },
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .aspectRatio(Size(bitmap.width.toFloat(), bitmap.height.toFloat()).aspectRatio(), matchHeightConstraintsFirst = size.width > size.height),
                    inProgressStrokesView = inProgressStrokesView,
                    strokeAuthoringState = strokeAuthoringState
                )
            }
        }
    }
}


class SharedPdfPageViewModel : ViewModel() {
    private val _strokesMap = mutableStateOf<Map<Int, Set<Stroke>>>(emptyMap())

    val strokes get() = _strokesMap.value

    fun setStrokesPerPage(page: Int, newStrokes: Set<Stroke>, size: Size) {
        if (size.width == 0f || size.height == 0f || newStrokes.isEmpty()) return
        _strokesMap.value = _strokesMap.value.toMutableMap().apply {
            this[page] = newStrokes.map { drawStroke ->
                val batch = MutableStrokeInputBatch()

                for (i in 0..<drawStroke.inputs.size) {
                    val strokeInput = drawStroke.inputs[i]
                    // normalize x and y to [0, 1], so we are independent of the size of the image
                    batch.addOrThrow(
                        x = strokeInput.x / size.width,
                        y = strokeInput.y / size.height,
                        pressure = strokeInput.pressure,
                        elapsedTimeMillis = strokeInput.elapsedTimeMillis,
                        tiltRadians = strokeInput.tiltRadians,
                        orientationRadians = strokeInput.orientationRadians,
                        type = strokeInput.toolType
                    )
                }

                Stroke(
                    brush = drawStroke.brush.copy(size = drawStroke.brush.size / size.width, epsilon = drawStroke.brush.epsilon / size.width),
                    inputs = batch
                )
            }.toSet()
        }
    }

    fun getStrokes(page: Int, size: Size): Set<Stroke> {
        return (_strokesMap.value[page] ?: emptySet()).map { stroke ->
            val batch = MutableStrokeInputBatch()

            for (i in 0..<stroke.inputs.size) {
                val drawStroke = stroke.inputs[i]
                // denormalize x and y to the size of the image
                batch.addOrThrow(
                    x = drawStroke.x * maxOf(size.width, 1.0f),
                    y = drawStroke.y * maxOf(size.height, 1.0f),
                    pressure = drawStroke.pressure,
                    elapsedTimeMillis = drawStroke.elapsedTimeMillis,
                    tiltRadians = drawStroke.tiltRadians,
                    orientationRadians = drawStroke.orientationRadians,
                    type = drawStroke.toolType
                )
            }

            Stroke(
                brush = stroke.brush.copy(size = stroke.brush.size * maxOf(size.width, 1.0f), epsilon = stroke.brush.epsilon * maxOf(size.width, 1.0f)),
                inputs = batch
            )
        }.toSet()
    }

    fun setStrokes(strokes: Map<Int, Set<Stroke>>, size: Size = Size(1f, 1f)) {
        strokes.forEach { (page, newStrokes) ->
            setStrokesPerPage(page, newStrokes, size)
        }
    }
}
