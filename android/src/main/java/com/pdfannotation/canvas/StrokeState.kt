package com.pdfannotation.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

@Stable
class StrokeAuthoringState(
    internal val inProgressStrokesView: InProgressStrokesView,
) : InProgressStrokesFinishedListener {
    var moveEventCount: Int = 0
    var currentStrokeId: InProgressStrokeId? = null
    var currentPointerId: Int? = null
    val finishedStrokes = mutableStateOf(emptySet<Stroke>())
    internal val motionEventPredictor: MotionEventPredictor = MotionEventPredictor.newInstance(inProgressStrokesView)

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finishedStrokes.value += strokes.values
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
    }
}

sealed interface StrokeAction {
    data object Start : StrokeAction
    data object Update : StrokeAction
    data object Finish : StrokeAction
    data object Cancel : StrokeAction
    data object Skip : StrokeAction
}

@Stable
fun interface StrokeActionInferer {
    fun mapStateToAction(strokeAuthoringState: StrokeAuthoringState): StrokeAction
}

@Composable
fun rememberStrokeAuthoringState(
    inProgressStrokesView: InProgressStrokesView,
): StrokeAuthoringState = remember(inProgressStrokesView) {
    StrokeAuthoringState(inProgressStrokesView).also { listener: InProgressStrokesFinishedListener ->
        inProgressStrokesView.addFinishedStrokesListener(listener)
    }
}
