package com.pdfannotation.viewer

import androidx.compose.ui.geometry.Size
import androidx.ink.brush.BrushFamily
import androidx.lifecycle.ViewModel
import com.facebook.react.bridge.ReadableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.graphics.Color as GraphicsColor
import androidx.compose.ui.graphics.Color
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import com.pdfannotation.serialization.Serializer
import java.io.File

data class BrushSettings(
    val size: Float,
    val color: Color,
    val family: BrushFamily
)

class PdfAnnotationViewModel : ViewModel() {
    private val _backgroundColor = MutableStateFlow<Int?>(null)
    private val _pdfFile = MutableStateFlow<File?>(null)
    private val _thumbnailMode = MutableStateFlow(false)
    private val _annotationFile = MutableStateFlow<File?>(null)
    private val _autoSave = MutableStateFlow(true)
    private val _brushSettings = MutableStateFlow<BrushSettings?>(null)
    private val _hidePagination = MutableStateFlow(false)
    private val _strokes = MutableStateFlow(Strokes())
    private val _serializer = Serializer()


    val backgroundColor: StateFlow<Int?> get() = _backgroundColor
    val pdfFile: StateFlow<File?> get() = _pdfFile
    val thumbnailMode: StateFlow<Boolean> get() = _thumbnailMode
    val annotationFile: StateFlow<File?> get() = _annotationFile
    val autoSave: StateFlow<Boolean> get() = _autoSave
    val brushSettings: StateFlow<BrushSettings?> get() = _brushSettings
    val hidePagination: StateFlow<Boolean> get() = _hidePagination
    val strokes: StateFlow<Strokes> get() = _strokes

    fun updateBackgroundColor(newColor: String?) {
        _backgroundColor.value = newColor?.let { GraphicsColor.parseColor(it) }
    }

    fun updatePdfFile(newPdf: String?) {
        _pdfFile.value = constructFile(newPdf)
        _strokes.value = Strokes()
        loadAnnotations()
    }

    fun updateThumbnailMode(newMode: Boolean) {
        _thumbnailMode.value = newMode
    }

    fun updateAnnotationFile(newAnnotationFile: String?) {
        _annotationFile.value = constructFile(newAnnotationFile)
        _strokes.value = Strokes()
        loadAnnotations(newAnnotationFile)
    }

    fun updateAutoSave(newAutoSave: Boolean) {
        _autoSave.value = newAutoSave
    }

    fun updateBrushSettings(newBrushSettings: ReadableMap?) {
        _brushSettings.value = newBrushSettings?.let { makeBrushSettings(it) }
    }

    fun updateHidePagination(newPagination: Boolean) {
        _hidePagination.value = newPagination
    }

    private fun makeBrushSettings(settings: ReadableMap): BrushSettings {
        val color = GraphicsColor.parseColor(settings.getString("color"))
        val family = when (settings.getString("family")) {
            "marker" -> StockBrushes.markerLatest
            "pressure-pen" -> StockBrushes.pressurePenLatest
            "highlighter" -> StockBrushes.highlighterLatest
            else -> StockBrushes.markerLatest
        }
        return BrushSettings(settings.getDouble("size").toFloat(), Color(color), family)
    }

    fun saveAnnotations(path: String? = null) {
        (constructFile(path) ?: annotationFile.value)?.let {
            if (_autoSave.value) {
                _serializer.storeStrokes(_strokes.value.strokes, it)
            }
        }
    }

    fun loadAnnotations(path: String? = null) {
        (constructFile(path) ?: annotationFile.value)?.let {
            _strokes.value.setStrokes(_serializer.loadStrokes(it))
        }
    }

    private fun constructFile(path: String?): File? {
        return path?.let {File(it.replace("file://", ""))}
    }
}

data class Strokes(var strokes: MutableMap<Int, Set<Stroke>> = mutableMapOf(), var redoMap: MutableMap<Int, Set<Stroke>> = mutableMapOf()) {
    fun setStrokesPerPage(page: Int, newStrokes: Set<Stroke>, size: Size) {
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
                    brush = drawStroke.brush.copy(size = drawStroke.brush.size / size.width, epsilon = drawStroke.brush.epsilon / size.width),
                    inputs = batch
                )
            }.toSet()
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
                brush = stroke.brush.copy(size = stroke.brush.size * maxOf(size.width, 1.0f), epsilon = stroke.brush.epsilon * maxOf(size.width, 1.0f)),
                inputs = batch
            )
        }.toSet()
    }

    fun setStrokes(strokes: Map<Int, Set<Stroke>>, size: Size = Size(1f, 1f)) {
        strokes.forEach { (page, newStrokes) ->
            setStrokesPerPage(page, newStrokes, size)
        }
    }
}