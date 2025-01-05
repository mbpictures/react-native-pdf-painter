package com.pdfannotation

import com.facebook.react.bridge.ReadableMap
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.viewmanagers.PdfAnnotationViewManagerInterface
import com.facebook.react.viewmanagers.PdfAnnotationViewManagerDelegate

@ReactModule(name = PdfAnnotationViewManager.NAME)
class PdfAnnotationViewManager : SimpleViewManager<PdfAnnotationView>(),
  PdfAnnotationViewManagerInterface<PdfAnnotationView> {
  private val mDelegate: ViewManagerDelegate<PdfAnnotationView>

  init {
    mDelegate = PdfAnnotationViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<PdfAnnotationView>? {
    return mDelegate
  }

  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): PdfAnnotationView {
    return PdfAnnotationView(context)
  }

  companion object {
    const val NAME = "PdfAnnotationView"
  }

  override fun setBackgroundColor(view: PdfAnnotationView?, value: String?) {
    view?.viewModel?.updateBackgroundColor(value ?: "#FFFFFF")
  }

  override fun setPdfUrl(view: PdfAnnotationView?, value: String?) {
    view?.viewModel?.updatePdfFile(value)
  }

  override fun setThumbnailMode(view: PdfAnnotationView?, value: Boolean) {
    view?.viewModel?.updateThumbnailMode(value)
  }

  override fun setAnnotationFile(view: PdfAnnotationView?, value: String?) {
    view?.viewModel?.updateAnnotationFile(value)
  }

  override fun setAutoSave(view: PdfAnnotationView?, value: Boolean) {
    view?.viewModel?.updateAutoSave(value)
  }

  override fun setBrushSettings(view: PdfAnnotationView?, value: ReadableMap?) {
    view?.viewModel?.updateBrushSettings(value)
  }

  override fun setHidePagination(view: PdfAnnotationView?, value: Boolean) {
    view?.viewModel?.updateHidePagination(value)
  }

  override fun saveAnnotations(view: PdfAnnotationView?, path: String?) {
    view?.viewModel?.saveAnnotations(path)
  }

  override fun loadAnnotations(view: PdfAnnotationView?, path: String?) {
    view?.viewModel?.loadAnnotations(path)
  }
}
