package com.pdfannotation.canvas

import android.graphics.Matrix
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.PartitionedMesh
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

@Stable
class StrokeAuthoringState(
    internal val inProgressStrokesView: InProgressStrokesView,
    private var strokesFinishedListener: ((Set<Stroke>) -> Unit)? = null
) : InProgressStrokesFinishedListener {
    var moveEventCount: Int = 0
    var currentStrokeId: InProgressStrokeId? = null
    var currentPointerId: Int? = null
    val finishedStrokes = mutableStateOf(emptySet<Stroke>())
    internal val motionEventPredictor: MotionEventPredictor = MotionEventPredictor.newInstance(inProgressStrokesView)
    var transformMatrix = Matrix()

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        val matrixValues = FloatArray(9)
        transformMatrix.getValues(matrixValues)
        val scaleX = matrixValues[Matrix.MSCALE_X]
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
                brush = stroke.brush.copy(size = stroke.brush.size * scaleX, epsilon = stroke.brush.epsilon),
                inputs = batch
            )
        }
        finishedStrokes.value += transformedStrokes
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
        strokesFinishedListener?.invoke(finishedStrokes.value)
    }

    fun eraseWholeStrokes(shape: PartitionedMesh) {
        val strokesToErase = finishedStrokes.value.filter { stroke ->
            stroke.shape.computeCoverageIsGreaterThan(
                other = shape,
                coverageThreshold = 0f
            )
        }
        if (strokesToErase.isNotEmpty()) {
            Snapshot.withMutableSnapshot {
                finishedStrokes.value -= strokesToErase
                strokesFinishedListener?.invoke(finishedStrokes.value)
            }
        }
    }
}

@Composable
fun rememberStrokeAuthoringState(
    inProgressStrokesView: InProgressStrokesView,
    transformMatrix: Matrix,
    strokesFinishedListener: ((Set<Stroke>) -> Unit)? = null
): StrokeAuthoringState {
    val state = remember(inProgressStrokesView) {
        StrokeAuthoringState(inProgressStrokesView, strokesFinishedListener).also { listener: InProgressStrokesFinishedListener ->
            inProgressStrokesView.addFinishedStrokesListener(listener)
        }
    }

    LaunchedEffect(transformMatrix) {
        state.transformMatrix = transformMatrix
    }

    return state
}
