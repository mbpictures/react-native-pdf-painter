package com.pdfannotation.serialization

import androidx.compose.ui.graphics.Color
import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pdfannotation.model.Link
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Serializer {
    private val gson = Gson()

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
            batch.add(
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
            strokeInputs = gson.toJson(serializedInputs),
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

        val serializedInputs = gson.fromJson(entity.strokeInputs, SerializedStrokeInputBatch::class.java)

        val inputs = deserializeStrokeInputBatch(serializedInputs)

        return Stroke(brush = brush, inputs = inputs)
    }

    fun brushToString(brush: Brush): String {
        val serializedBrush = serializeBrush(brush)
        return gson.toJson(serializedBrush)
    }

    fun stringToBrush(jsonString: String): Brush {
        val serializedBrush = gson.fromJson(jsonString, SerializedBrush::class.java)
        return deserializeBrush(serializedBrush)
    }

    private fun serializeLink(link: Link): LinkEntity {
        return LinkEntity(
            id = link.id,
            targetId = link.targetId,
            x = link.x,
            y = link.y,
            width = link.width,
            height = link.height,
            color = link.color.value,
            isFirst = link.isFirst
        )
    }

    private fun deserializeLink(link: LinkEntity): Link {
        return Link(
            id = link.id,
            targetId = link.targetId,
            x = link.x,
            y = link.y,
            width = link.width,
            height = link.height,
            color = Color(link.color),
            isFirst = link.isFirst
        )
    }

    private fun compress(content: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(content) }
        return bos.toByteArray()
    }

    private fun decompress(content: ByteArray): String {
        return try {
            GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
        } catch (e: IOException) {
            "{}" // not a valid compressed input
        }
    }

    fun serializeAnnotations(strokes: Map<Int, Set<Stroke>>, links: Map<Int, Set<Link>>): ByteArray {
        return compress(gson.toJson(
            Annotations(
                strokes = strokes.mapValues { it.value.map {stroke -> serializeStrokeToEntity(stroke) } },
                links = links.mapValues { it.value.map { link -> serializeLink(link) } }
            )
        ))
    }

    fun deserializeAnnotations(jsonString: String): Pair<Map<Int, Set<Stroke>>, Map<Int, Set<Link>>?> {
        var type: Type = object : TypeToken<Annotations>() {}.type
        try {
            val annotations = gson.fromJson<Annotations>(jsonString, type)
            return Pair(
                annotations.strokes.mapValues { it.value.map { stroke -> deserializeEntityToStroke(stroke) }.toSet() },
                annotations.links.mapValues { it.value.map { stroke -> deserializeLink(stroke) }.toSet() }
            )
        } catch (e: Exception) {
            // Fallback to old format if new format fails for backward compatibility
            type = object : TypeToken<Map<Int, Set<StrokeEntity>>>() {}.type
            val strokes = gson.fromJson<Map<Int, Set<StrokeEntity>>>(jsonString, type)
            return Pair(strokes.mapValues { it.value.map { stroke -> deserializeEntityToStroke(stroke) }.toSet() }, emptyMap())
        }
    }

    fun storeAnnotations(strokes: Map<Int, Set<Stroke>>, links: Map<Int, Set<Link>>, file: File) {
        val serializedStrokes = serializeAnnotations(strokes, links)
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        file.writeBytes(serializedStrokes)
    }

    fun loadAnnotations(file: File): Pair<Map<Int, Set<Stroke>>, Map<Int, Set<Link>>?> {
        if (!file.exists()) return Pair(emptyMap(), emptyMap())
        val serializedStrokes = file.readBytes()
        return deserializeAnnotations(decompress(serializedStrokes))
    }
}


