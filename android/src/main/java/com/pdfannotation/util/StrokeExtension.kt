package com.pdfannotation.util

import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke

fun Stroke.applyLineal(): Stroke {
    val inputs = this.inputs
    if (inputs.size < 2) return this
    val newInputs = MutableStrokeInputBatch()
    val first = inputs[0]
    val last = inputs[inputs.size - 1]
    newInputs.add(first)
    newInputs.add(last)

    return Stroke(
        inputs = newInputs,
        brush = this.brush,
    )
}
