package com.pdfannotation.canvas

import android.annotation.SuppressLint
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer

@Composable
@SuppressLint("ClickableViewAccessibility")
fun InkCanvas(
    modifier: Modifier = Modifier,
    strokeAuthoringState: StrokeAuthoringState,
) {
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    Box(modifier = modifier) {
        Canvas(modifier = Modifier) {
            val canvasTransform = Matrix()
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            strokeAuthoringState.finishedStrokes.value.forEach { stroke ->
                try {
                    canvasStrokeRenderer.draw(
                        stroke = stroke,
                        canvas = canvas,
                        strokeToScreenTransform = canvasTransform,
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}



@Composable
fun rememberInProgressStrokesView(): InProgressStrokesView {
    val context = LocalContext.current
    return remember {
        InProgressStrokesView(context).apply {
            motionEventToViewTransform = Matrix()
            visibility = android.view.View.VISIBLE
            alpha = 1.0f
            // Ensure the view is ready to receive touch events
            isEnabled = true
            isFocusable = false
            isClickable = false
        }
    }
}


