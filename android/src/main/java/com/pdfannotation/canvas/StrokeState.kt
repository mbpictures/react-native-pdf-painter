package com.pdfannotation.canvas

import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

@Stable
class StrokeAuthoringState(
    internal val inProgressStrokesView: InProgressStrokesView
) : InProgressStrokesFinishedListener {
    var moveEventCount: Int = 0
    var currentStrokeId: InProgressStrokeId? = null
    var currentPointerId: Int? = null
    val finishedStrokes = mutableStateOf(emptySet<Stroke>())
    internal val motionEventPredictor: MotionEventPredictor = MotionEventPredictor.newInstance(inProgressStrokesView)
    var transformMatrix = Matrix()

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        val transformedStrokes = strokes.values.map { stroke ->
            val batch = MutableStrokeInputBatch()

            for (i in 0..<stroke.inputs.size) {
                val drawStroke = stroke.inputs[i]
                val transformedPoint = floatArrayOf(drawStroke.x, drawStroke.y).apply {
                    transformMatrix.mapPoints(this)
                }
                batch.addOrThrow(
                    x = transformedPoint[0],
                    y = transformedPoint[1],
                    pressure = drawStroke.pressure,
                    elapsedTimeMillis = drawStroke.elapsedTimeMillis,
                    tiltRadians = drawStroke.tiltRadians,
                    orientationRadians = drawStroke.orientationRadians,
                    type = drawStroke.toolType
                )
            }

            Stroke(
                brush = stroke.brush.copy(size = stroke.brush.size, epsilon = stroke.brush.epsilon),
                inputs = batch
            )
        }
        finishedStrokes.value += transformedStrokes
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
    }
}

@Composable
fun rememberStrokeAuthoringState(
    inProgressStrokesView: InProgressStrokesView,
    transformMatrix: Matrix,
): StrokeAuthoringState {
    val state = remember(inProgressStrokesView) {
        StrokeAuthoringState(inProgressStrokesView).also { listener: InProgressStrokesFinishedListener ->
            inProgressStrokesView.addFinishedStrokesListener(listener)
        }
    }

    LaunchedEffect(transformMatrix) {
        state.transformMatrix = transformMatrix
    }

    return state
}
