package com.pdfannotation.canvas

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.ink.brush.Brush
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.ImmutableVec
import androidx.ink.strokes.InProgressStroke
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import com.pdfannotation.model.BrushSettings

class StrokeAuthoringTouchListener(
    private val strokeAuthoringState: StrokeAuthoringState,
    private val brush: Brush,
    private val isEraser: Boolean,
) : View.OnTouchListener {

    private var eraserStroke: MutableStrokeInputBatch = MutableStrokeInputBatch()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        if (view == null) return false

        val predictedEvent = strokeAuthoringState.motionEventPredictor.run {
            record(event)
            predict()
        }
        if (isEraser) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                eraserStroke = MutableStrokeInputBatch()
                return true
            }

            for (i in 0 until event.pointerCount) {
                val pointerIndex = event.findPointerIndex(event.getPointerId(i))
                val pressure = event.getPressure(pointerIndex)
                try {
                    eraserStroke.add(
                        StrokeInput().apply {
                            update(
                                x = event.getX(pointerIndex),
                                y = event.getY(pointerIndex),
                                pressure = if (pressure == -1.0f) -1.0f else pressure.coerceIn(0.0f, 1.0f),
                                elapsedTimeMillis = event.eventTime,
                            )
                        }
                    )
                } catch (_: Exception) {} // possible "INVALID_ARGUMENT: Inputs must not have duplicate `position` and `elapsed_time`" exception
            }

            strokeAuthoringState.eraseWholeStrokes(Stroke(brush, eraserStroke).shape)
            return true
        }

        doPreHandlerAction(event)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleStartStroke(
                    event = event,
                    view = view,
                )
                true
            }

            MotionEvent.ACTION_MOVE -> {
                handleUpdateStroke(
                    event = event,
                    predictedEvent = predictedEvent,
                )
                true
            }

            MotionEvent.ACTION_UP -> {
                handleFinishStroke(
                    event = event,
                )
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                handleCancelStroke(
                    event = event,
                )
                true
            }

            else -> false
        }.also {
            doPostHandlerAction(event, view)
            predictedEvent?.recycle()
        }
    }

    private fun handleStartStroke(
        event: MotionEvent,
        view: View,
    ) {
        view.requestUnbufferedDispatch(event)
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        strokeAuthoringState.currentPointerId = pointerId
        try {
            strokeAuthoringState.currentStrokeId = strokeAuthoringState.inProgressStrokesView.startStroke(
                event = event,
                pointerId = pointerId,
                brush = brush,
            )
        } catch (e: Exception) {
            strokeAuthoringState.currentStrokeId = null
        }
    }

    private fun handleUpdateStroke(
        event: MotionEvent,
        predictedEvent: MotionEvent?,
    ) {
        val pointerId = strokeAuthoringState.currentPointerId
        val strokeId = strokeAuthoringState.currentStrokeId

        if (pointerId == null || strokeId == null) return

        // TODO: Check if there is a chance to have more than one pointer ID within event pointers
        for (pointerIndex in 0 until event.pointerCount) {
            if (event.getPointerId(pointerIndex) != pointerId) continue
            try {
                strokeAuthoringState.inProgressStrokesView.addToStroke(
                    event,
                    pointerId,
                    strokeId,
                    predictedEvent,
                )
            } catch (_: Exception) {} // possible "Stroke with ID was not found" exception
        }
    }

    private fun handleFinishStroke(
        event: MotionEvent,
    ) {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        if (pointerId == strokeAuthoringState.currentPointerId) {
            strokeAuthoringState.inProgressStrokesView.finishStroke(
                event,
                pointerId,
                strokeAuthoringState.currentStrokeId!!
            )
        }
    }

    private fun handleCancelStroke(
        event: MotionEvent,
    ) {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        if (pointerId != strokeAuthoringState.currentPointerId) return

        strokeAuthoringState.inProgressStrokesView.cancelStroke(
            strokeId = strokeAuthoringState.currentStrokeId!!,
            event = event,
        )
    }

    private fun doPreHandlerAction(event: MotionEvent) {
        if (event.actionMasked != MotionEvent.ACTION_MOVE) {
            strokeAuthoringState.moveEventCount = 0
        }
    }

    private fun doPostHandlerAction(event: MotionEvent, view: View) {
        if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            strokeAuthoringState.moveEventCount++
        } else if (event.actionMasked == MotionEvent.ACTION_UP) {
            view.performClick()
        }
    }
}

@Composable
fun rememberStrokeAuthoringTouchListener(
    strokeAuthoringState: StrokeAuthoringState,
    brushSettings: BrushSettings?,
    transformMatrix: Matrix = Matrix.IDENTITY_MATRIX,
): StrokeAuthoringTouchListener? =
    remember(brushSettings) {
        brushSettings?.let {
            val matrixValues = FloatArray(9)
            transformMatrix.getValues(matrixValues)
            val scaleX = matrixValues[Matrix.MSCALE_X]
            StrokeAuthoringTouchListener(
                strokeAuthoringState = strokeAuthoringState,
                isEraser = brushSettings.isEraser,
                brush = Brush.createWithColorIntArgb(
                    family = it.family,
                    colorIntArgb = it.color.toArgb(),
                    size = it.size / scaleX,
                    epsilon = 0.1F
                ),
            )
        }

    }
