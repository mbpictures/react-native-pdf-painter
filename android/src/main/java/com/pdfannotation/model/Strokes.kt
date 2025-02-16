package com.pdfannotation.model

import androidx.compose.ui.geometry.Size
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke

data class Strokes(
    var strokes: MutableMap<Int, Set<Stroke>> = mutableMapOf(),
    var redoMap: MutableMap<Int, Set<Stroke>> = mutableMapOf(),
    var onStrokesChange: (() -> Unit)? = null
) {
    fun setStrokesPerPage(page: Int, newStrokes: Set<Stroke>, size: Size, initial: Boolean = false) {
        if (size.width == 0f || size.height == 0f || newStrokes.isEmpty()) return
        strokes = strokes.toMutableMap().apply {
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
                    brush = drawStroke.brush.copy(
                        size = drawStroke.brush.size / size.width,
                        epsilon = drawStroke.brush.epsilon / size.width
                    ),
                    inputs = batch
                )
            }.toSet()
        }
        if (!initial) {
            onStrokesChange?.invoke()
        }
    }

    fun getStrokes(page: Int, size: Size): Set<Stroke> {
        return (strokes[page] ?: emptySet()).map { stroke ->
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
                brush = stroke.brush.copy(
                    size = stroke.brush.size * maxOf(size.width, 1.0f),
                    epsilon = stroke.brush.epsilon * maxOf(size.width, 1.0f)
                ),
                inputs = batch
            )
        }.toSet()
    }

    fun setStrokes(strokes: Map<Int, Set<Stroke>>, size: Size = Size(1f, 1f), initial: Boolean = false) {
        strokes.forEach { (page, newStrokes) ->
            setStrokesPerPage(page, newStrokes, size, initial)
        }
    }

    fun undo(page: Int): Strokes {
        val lastElement = strokes[page]?.lastOrNull() ?: return this
        val newRedoMap = redoMap.toMutableMap().apply {
            this[page] = (this[page] ?: emptySet()).toMutableSet().apply {
                this.add(lastElement)
            }
        }
        val newStrokes = strokes.toMutableMap().apply {
            this[page] = this[page]?.toMutableList().apply {
                this?.remove(lastElement)
            }?.toSet() ?: emptySet()
        }
        return this.copy(strokes = newStrokes, redoMap = newRedoMap)
    }

    fun redo(page: Int): Strokes {
        val lastElement = redoMap[page]?.lastOrNull() ?: return this
        val newRedoMap = redoMap.toMutableMap().apply {
            this[page] = (this[page] ?: emptySet()).toMutableSet().apply {
                this.remove(lastElement)
            }
        }
        val newStrokes = strokes.toMutableMap().apply {
            this[page] = this[page]?.toMutableList().apply {
                this?.add(lastElement)
            }?.toSet() ?: emptySet()
        }
        return this.copy(strokes = newStrokes, redoMap = newRedoMap)
    }
}