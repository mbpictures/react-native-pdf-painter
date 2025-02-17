package com.pdfannotation.model

import androidx.ink.brush.BrushFamily
import androidx.lifecycle.ViewModel
import com.facebook.react.bridge.ReadableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.graphics.Color as GraphicsColor
import androidx.compose.ui.graphics.Color
import androidx.ink.brush.StockBrushes
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
    private val onTap: ((x: Float, y: Float) -> Unit)? = null,
    onLinkCompleted: ((from: Int, to: Int) -> Unit)? = null
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
    private val _beyondViewportPageCount = MutableStateFlow<Int?>(null)


    val backgroundColor: StateFlow<Int?> get() = _backgroundColor
    val pdfFile: StateFlow<File?> get() = _pdfFile
    val thumbnailMode: StateFlow<Boolean> get() = _thumbnailMode
    val annotationFile: StateFlow<File?> get() = _annotationFile
    val brushSettings: StateFlow<BrushSettings?> get() = _brushSettings
    val strokes: StateFlow<Strokes> get() = _strokes
    val currentPage: StateFlow<Int> get() = _currentPage
    val links: Links = Links(
        onLinkCompleted = {from, to ->
            onLinkCompleted?.invoke(from, to)
            if (_autoSave) saveAnnotations()
        }
    )
    val beyondViewportPageCount: StateFlow<Int?> get() = _beyondViewportPageCount

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
        if (newBrushSettings?.hasKey("type") == true && newBrushSettings.getString("type") == "link") {
            links.canCreateLinks = true
            links.size = newBrushSettings.getDouble("size").toFloat()
            links.color = Color(GraphicsColor.parseColor(newBrushSettings.getString("color")))
            return
        }

        links.canCreateLinks = false
        _brushSettings.value = newBrushSettings?.let { makeBrushSettings(it) }
    }

    fun updateBeyondViewportPageCount(count: Int) {
        _beyondViewportPageCount.value = count
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
            _serializer.storeAnnotations(
                _strokes.value.strokes,
                links.links.value,
                it
            )
        }
    }

    fun loadAnnotations(path: String? = null) {
        (constructFile(path) ?: annotationFile.value)?.let {
            if (it.absolutePath == _loadedAnnotationPath) return
            val result = _serializer.loadAnnotations(it)
            _strokes.value = makeStrokes()
            _strokes.value.setStrokes(result.first, initial = true)
            links.initialLinks(result.second ?: emptyMap())
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

