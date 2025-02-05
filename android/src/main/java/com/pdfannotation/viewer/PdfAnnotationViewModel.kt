package com.pdfannotation.viewer

import android.util.Log
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
import kotlinx.coroutines.flow.update
import java.io.File
import java.net.URLDecoder

data class BrushSettings(
    val size: Float,
    val color: Color,
    val family: BrushFamily,
    val isEraser: Boolean = false
)

class PdfAnnotationViewModel(
    private val onPageCount: ((pageCount: Int) -> Unit)? = null,
    private val onPageChange: ((currentPage: Int) -> Unit)? = null,
    private val onDocumentFinished: ((next: Boolean) -> Unit)? = null,
    private val onTap: ((x: Float, y: Float) -> Unit)? = null
) : ViewModel() {
    private val _backgroundColor = MutableStateFlow<Int?>(null)
    private val _pdfFile = MutableStateFlow<File?>(null)
    private val _thumbnailMode = MutableStateFlow(false)
    private val _annotationFile = MutableStateFlow<File?>(null)
    private var _autoSave = false
    private val _brushSettings = MutableStateFlow<BrushSettings?>(null)
    private val _strokes = MutableStateFlow(makeStrokes())
    private val _currentPage = MutableStateFlow(0)
    private var _pageCount = 0
    private val _serializer = Serializer()
    private var _loadedAnnotationPath = ""


    val backgroundColor: StateFlow<Int?> get() = _backgroundColor
    val pdfFile: StateFlow<File?> get() = _pdfFile
    val thumbnailMode: StateFlow<Boolean> get() = _thumbnailMode
    val annotationFile: StateFlow<File?> get() = _annotationFile
    val brushSettings: StateFlow<BrushSettings?> get() = _brushSettings
    val strokes: StateFlow<Strokes> get() = _strokes
    val currentPage: StateFlow<Int> get() = _currentPage

    fun updateBackgroundColor(newColor: String?) {
        _backgroundColor.value = newColor?.let { GraphicsColor.parseColor(it) }
    }

    fun updatePdfFile(newPdf: String?) {
        _pdfFile.value = constructFile(newPdf)
        loadAnnotations()
    }

    fun updateThumbnailMode(newMode: Boolean) {
        _thumbnailMode.value = newMode
    }

    fun updateAnnotationFile(newAnnotationFile: String?) {
        _annotationFile.value = constructFile(newAnnotationFile, true)
        loadAnnotations(newAnnotationFile)
    }

    fun updateAutoSave(newAutoSave: Boolean) {
        _autoSave = newAutoSave
    }

    fun updateBrushSettings(newBrushSettings: ReadableMap?) {
        _brushSettings.value = newBrushSettings?.let { makeBrushSettings(it) }
    }

    private fun makeBrushSettings(settings: ReadableMap): BrushSettings {
        val color = GraphicsColor.parseColor(settings.getString("color"))
        val family = when (settings.getString("type")) {
            "marker" -> StockBrushes.markerLatest
            "pressure-pen" -> StockBrushes.pressurePenLatest
            "highlighter" -> StockBrushes.highlighterLatest
            else -> StockBrushes.markerLatest
        }
        return BrushSettings(settings.getDouble("size").toFloat(), Color(color), family, isEraser = settings.getString("type") == "eraser")
    }

    fun saveAnnotations(path: String? = null) {
        (constructFile(path, true) ?: annotationFile.value)?.let {
            _serializer.storeStrokes(_strokes.value.strokes, it)
        }
    }

    fun loadAnnotations(path: String? = null) {
        (constructFile(path) ?: annotationFile.value)?.let {
            if (it.absolutePath == _loadedAnnotationPath) return
            _strokes.value = makeStrokes()
            _strokes.value.setStrokes(_serializer.loadStrokes(it))
            _loadedAnnotationPath = it.absolutePath
        }
    }

    fun undo() {
        _strokes.update { it.undo(currentPage.value) }
        if (!_autoSave) return
        saveAnnotations()
    }

    fun redo() {
        _strokes.update { it.redo(currentPage.value) }
        if (!_autoSave) return
        saveAnnotations()
    }

    fun clear() {
        _strokes.update {
            it.copy(
                strokes = it.strokes.toMutableMap().apply {
                    this[currentPage.value] = emptySet()
                },
                redoMap = it.redoMap.toMutableMap().apply {
                    this[currentPage.value] = emptySet()
                }
            )
        }
        if (!_autoSave) return
        saveAnnotations()
    }

    fun setPage(page: Int) {
        _currentPage.value = page
        onPageChange?.invoke(page)
    }

    fun setPageCount(pageCount: Int) {
        _pageCount = pageCount
        onPageCount?.invoke(pageCount)
    }

    fun handleDocumentFinished(next: Boolean) {
        onDocumentFinished?.invoke(next)
    }

    fun handleTap(x: Float, y: Float) {
        onTap?.invoke(x, y)
    }

    private fun constructFile(path: String?, ignoreFileDoesNotExist: Boolean = false): File? {
        val result = path?.let {File(URLDecoder.decode(it, "UTF-8").replace("file://", ""))}
        if (result?.exists() == true || ignoreFileDoesNotExist) {
            return result
        }
        return null
    }

    private fun makeStrokes(): Strokes {
        return Strokes(
            onStrokesChange = {
                if (!_autoSave) return@Strokes
                saveAnnotations()
            }
        )
    }
}

data class Strokes(
    var strokes: MutableMap<Int, Set<Stroke>> = mutableMapOf(),
    var redoMap: MutableMap<Int, Set<Stroke>> = mutableMapOf(),
    var onStrokesChange: (() -> Unit)? = null
) {
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
        onStrokesChange?.invoke()
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