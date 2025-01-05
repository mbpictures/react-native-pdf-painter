package com.pdfannotation.serialization

data class SerializedStroke(
    val inputs: SerializedStrokeInputBatch,
    val brush: SerializedBrush
)

data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush
)

data class StrokeEntity(
    val brushSize: Float,
    val brushColor: Long,
    val brushEpsilon: Float,
    val stockBrush: SerializedStockBrush,
    val strokeInputs: String?
)

enum class SerializedStockBrush {
    MARKER_V1,
    PRESSURE_PEN_V1,
    HIGHLIGHTER_V1
}

data class SerializedStrokeInputBatch(
    val toolType: SerializedToolType,
    val strokeUnitLengthCm: Float,
    val inputs: List<SerializedStrokeInput>
)

data class SerializedStrokeInput(
    val x: Float,
    val y: Float,
    val timeMillis: Float,
    val pressure: Float,
    val tiltRadians: Float,
    val orientationRadians: Float,
    val strokeUnitLengthCm: Float
)

enum class SerializedToolType {
    STYLUS,
    TOUCH,
    MOUSE,
    UNKNOWN
}
