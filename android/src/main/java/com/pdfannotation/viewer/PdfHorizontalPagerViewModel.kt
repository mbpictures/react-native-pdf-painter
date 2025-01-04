package com.pdfannotation.viewer

import androidx.ink.brush.BrushFamily
import androidx.lifecycle.ViewModel
import com.facebook.react.bridge.ReadableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.graphics.Color as GraphicsColor
import androidx.compose.ui.graphics.Color
import androidx.ink.brush.StockBrushes
import java.io.File

data class BrushSettings(
    val size: Float,
    val color: Color,
    val family: BrushFamily
)

class PdfHorizontalPagerViewModel : ViewModel() {
    private val _backgroundColor = MutableStateFlow("#FFFFFF")
    private val _pdfFile = MutableStateFlow<File?>(null)
    private val _thumbnailMode = MutableStateFlow(false)
    private val _annotationFile = MutableStateFlow<File?>(null)
    private val _autoSave = MutableStateFlow(true)
    private val _brushSettings = MutableStateFlow<BrushSettings?>(null)


    val backgroundColor: StateFlow<String> get() = _backgroundColor
    val pdfFile: StateFlow<File?> get() = _pdfFile
    val thumbnailMode: StateFlow<Boolean> get() = _thumbnailMode
    val annotationFile: StateFlow<File?> get() = _annotationFile
    val autoSave: StateFlow<Boolean> get() = _autoSave
    val brushSettings: StateFlow<BrushSettings?> get() = _brushSettings

    fun updateBackgroundColor(newColor: String) {
        _backgroundColor.value = newColor
    }

    fun updatePdfFile(newPdf: String?) {
        _pdfFile.value = newPdf?.let { File(it) }
    }

    fun updateThumbnailMode(newMode: Boolean) {
        _thumbnailMode.value = newMode
    }

    fun updateAnnotationFile(newAnnotationFile: String?) {
        _annotationFile.value = newAnnotationFile?.let { File(it) }
    }

    fun updateAutoSave(newAutoSave: Boolean) {
        _autoSave.value = newAutoSave
    }

    fun updateBrushSettings(newBrushSettings: ReadableMap?) {
        _brushSettings.value = newBrushSettings?.let { makeBrushSettings(it) }
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
}