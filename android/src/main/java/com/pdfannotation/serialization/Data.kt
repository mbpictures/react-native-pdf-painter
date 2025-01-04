package com.pdfannotation.serialization

import kotlinx.serialization.Serializable

@Serializable
data class SerializedStroke(
    val inputs: SerializedStrokeInputBatch,
    val brush: SerializedBrush
)

@Serializable
data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush
)

@Serializable
data class StrokeEntity(
    val brushSize: Float,
    val brushColor: Long,
    val brushEpsilon: Float,
    val stockBrush: SerializedStockBrush,
    val strokeInputs: String?
)

@Serializable
enum class SerializedStockBrush {
    MARKER_V1,
    PRESSURE_PEN_V1,
    HIGHLIGHTER_V1
}

@Serializable
data class SerializedStrokeInputBatch(
    val toolType: SerializedToolType,
    val strokeUnitLengthCm: Float,
    val inputs: List<SerializedStrokeInput>
)

@Serializable
data class SerializedStrokeInput(
    val x: Float,
    val y: Float,
    val timeMillis: Float,
    val pressure: Float,
    val tiltRadians: Float,
    val orientationRadians: Float,
    val strokeUnitLengthCm: Float
)

@Serializable
enum class SerializedToolType {
    STYLUS,
    TOUCH,
    MOUSE,
    UNKNOWN
}
