package com.pdfannotation.serialization

import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Serializer {

    companion object {
        private val stockBrushToEnumValues =
            mapOf(
                StockBrushes.markerV1 to SerializedStockBrush.MARKER_V1,
                StockBrushes.pressurePenV1 to SerializedStockBrush.PRESSURE_PEN_V1,
                StockBrushes.highlighterV1 to SerializedStockBrush.HIGHLIGHTER_V1,
            )

        private val enumToStockBrush =
            stockBrushToEnumValues.entries.associate { (key, value) -> value to key }
    }

    private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.MARKER_V1,
        )
    }

    private fun serializeStrokeInputBatch(inputs: StrokeInputBatch): SerializedStrokeInputBatch {
        val serializedInputs = mutableListOf<SerializedStrokeInput>()
        val scratchInput = StrokeInput()

        for (i in 0 until inputs.size) {
            inputs.populate(i, scratchInput)
            serializedInputs.add(
                SerializedStrokeInput(
                    x = scratchInput.x,
                    y = scratchInput.y,
                    timeMillis = scratchInput.elapsedTimeMillis.toFloat(),
                    pressure = scratchInput.pressure,
                    tiltRadians = scratchInput.tiltRadians,
                    orientationRadians = scratchInput.orientationRadians,
                    strokeUnitLengthCm = scratchInput.strokeUnitLengthCm,
                )
            )
        }

        val toolType =
            when (inputs.getToolType()) {
                InputToolType.STYLUS -> SerializedToolType.STYLUS
                InputToolType.TOUCH -> SerializedToolType.TOUCH
                InputToolType.MOUSE -> SerializedToolType.MOUSE
                else -> SerializedToolType.UNKNOWN
            }

        return SerializedStrokeInputBatch(
            toolType = toolType,
            strokeUnitLengthCm = inputs.getStrokeUnitLengthCm(),
            inputs = serializedInputs,
        )
    }

    private fun deserializeStroke(serializedStroke: SerializedStroke): Stroke {
        val inputs = deserializeStrokeInputBatch(serializedStroke.inputs)
        val brush = deserializeBrush(serializedStroke.brush)
        return Stroke(brush = brush, inputs = inputs)
    }

    private fun deserializeBrush(serializedBrush: SerializedBrush): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush] ?: StockBrushes.markerV1

        return Brush.createWithColorLong(
            family = stockBrushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }

    private fun deserializeStrokeInputBatch(
        serializedBatch: SerializedStrokeInputBatch
    ): StrokeInputBatch {
        val toolType =
            when (serializedBatch.toolType) {
                SerializedToolType.STYLUS -> InputToolType.STYLUS
                SerializedToolType.TOUCH -> InputToolType.TOUCH
                SerializedToolType.MOUSE -> InputToolType.MOUSE
                else -> InputToolType.UNKNOWN
            }

        val batch = MutableStrokeInputBatch()

        serializedBatch.inputs.forEach { input ->
            batch.addOrThrow(
                type = toolType,
                x = input.x,
                y = input.y,
                elapsedTimeMillis = input.timeMillis.toLong(),
                pressure = input.pressure,
                tiltRadians = input.tiltRadians,
                orientationRadians = input.orientationRadians,
            )
        }

        return batch
    }

    fun serializeStrokeToEntity(stroke: Stroke): StrokeEntity {
        val serializedBrush = serializeBrush(stroke.brush)
        val serializedInputs = serializeStrokeInputBatch(stroke.inputs)
        return StrokeEntity(
            brushSize = serializedBrush.size,
            brushColor = serializedBrush.color,
            brushEpsilon = serializedBrush.epsilon,
            stockBrush = serializedBrush.stockBrush,
            strokeInputs = Json.encodeToString(serializedInputs),
        )
    }

    fun deserializeEntityToStroke(entity: StrokeEntity): Stroke {
        val serializedBrush =
            SerializedBrush(
                size = entity.brushSize,
                color = entity.brushColor,
                epsilon = entity.brushEpsilon,
                stockBrush = entity.stockBrush,
            )
        val brush = deserializeBrush(serializedBrush)
        if (entity.strokeInputs == null) {
            return Stroke(brush = brush, inputs = MutableStrokeInputBatch())
        }

        val serializedInputs = Json.decodeFromString<SerializedStrokeInputBatch>(entity.strokeInputs)

        val inputs = deserializeStrokeInputBatch(serializedInputs)

        return Stroke(brush = brush, inputs = inputs)
    }

    fun brushToString(brush: Brush): String {
        val serializedBrush = serializeBrush(brush)
        return Json.encodeToString(serializedBrush)
    }

    fun stringToBrush(jsonString: String): Brush {
        val serializedBrush = Json.decodeFromString<SerializedBrush>(jsonString)
        return deserializeBrush(serializedBrush)
    }

    private fun compress(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }

    private fun decompress(content: ByteArray): String {
        return GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
    }

    fun serializeStrokes(strokes: Map<Int, Set<Stroke>>): ByteArray {
        return compress(Json.encodeToString(
            strokes.mapValues { it.value.map {stroke -> serializeStrokeToEntity(stroke) } }
        ))
    }

    fun deserializeStrokes(jsonString: String): Map<Int, Set<Stroke>> {
        val strokes = Json.decodeFromString<Map<Int, Set<StrokeEntity>>>(jsonString)
        return strokes.mapValues { it.value.map { stroke -> deserializeEntityToStroke(stroke) }.toSet() }
    }

    fun storeStrokes(strokes: Map<Int, Set<Stroke>>, file: File) {
        val serializedStrokes = serializeStrokes(strokes)
        file.writeBytes(serializedStrokes)
    }

    fun loadStrokes(file: File): Map<Int, Set<Stroke>> {
        if (!file.exists()) return emptyMap()
        val serializedStrokes = file.readBytes()
        return deserializeStrokes(decompress(serializedStrokes))
    }
}


