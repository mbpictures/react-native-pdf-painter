package com.pdfannotation.canvas

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import com.pdfannotation.model.BrushSettings


@OptIn(ExperimentalComposeUiApi::class)
@Composable
@SuppressLint("ClickableViewAccessibility")
fun InkCanvas(
    brushSettings: BrushSettings?,
    modifier: Modifier = Modifier,
    inProgressStrokesView: InProgressStrokesView = rememberInProgressStrokesView(),
    transformMatrix: Matrix,
    strokeAuthoringState: StrokeAuthoringState = rememberStrokeAuthoringState(inProgressStrokesView, transformMatrix),
    strokeAuthoringTouchListener: StrokeAuthoringTouchListener? =
        rememberStrokeAuthoringTouchListener(
            strokeAuthoringState = strokeAuthoringState,
            brushSettings = brushSettings,
        )
) {
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    Box(
        modifier = modifier
            .pointerInteropFilter { event ->
                strokeAuthoringTouchListener?.onTouch(inProgressStrokesView, event) ?: false
            }
    ) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .clipToBounds(),
            factory = {
                inProgressStrokesView.apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                }
            },
            update = { inProgressStrokesView ->
                inProgressStrokesView.motionEventToViewTransform = transformMatrix
            }
        )
        Canvas(modifier = Modifier) {
            val canvasTransform = Matrix()
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            strokeAuthoringState.finishedStrokes.value.forEach { stroke ->
                canvasStrokeRenderer.draw(
                    stroke = stroke,
                    canvas = canvas,
                    strokeToScreenTransform = canvasTransform,
                )
            }
        }
    }
}



@Composable
fun rememberInProgressStrokesView(): InProgressStrokesView {
    val context = LocalContext.current
    return remember { InProgressStrokesView(context) }
}


