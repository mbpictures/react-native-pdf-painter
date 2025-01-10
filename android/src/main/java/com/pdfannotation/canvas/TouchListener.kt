package com.pdfannotation.canvas

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.ink.brush.Brush
import com.pdfannotation.viewer.BrushSettings

class StrokeAuthoringTouchListener(
    private val strokeAuthoringState: StrokeAuthoringState,
    private val brush: Brush,
) : View.OnTouchListener {

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        val predictedEvent = strokeAuthoringState.motionEventPredictor.run {
            record(event)
            predict()
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
        strokeAuthoringState.currentStrokeId = strokeAuthoringState.inProgressStrokesView.startStroke(
            event = event,
            pointerId = pointerId,
            brush = brush,
        )
    }

    private fun handleUpdateStroke(
        event: MotionEvent,
        predictedEvent: MotionEvent?,
    ) {
        val pointerId = checkNotNull(strokeAuthoringState.currentPointerId)
        val strokeId = checkNotNull(strokeAuthoringState.currentStrokeId)

        // TODO: Check if there is a chance to have more than one pointer ID within event pointers
        for (pointerIndex in 0 until event.pointerCount) {
            if (event.getPointerId(pointerIndex) != pointerId) continue
            strokeAuthoringState.inProgressStrokesView.addToStroke(
                event,
                pointerId,
                strokeId,
                predictedEvent,
            )
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
        check(pointerId == strokeAuthoringState.currentPointerId)

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
): StrokeAuthoringTouchListener? =
    remember(brushSettings) {
        brushSettings?.let {
            StrokeAuthoringTouchListener(
                strokeAuthoringState = strokeAuthoringState,
                brush = Brush.createWithColorIntArgb(
                    family = it.family,
                    colorIntArgb = it.color.toArgb(),
                    size = it.size,
                    epsilon = 0.1F
                ),
            )
        }

    }
