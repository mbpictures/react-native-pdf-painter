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
  private val mDelegate: PdfAnnotationViewManagerDelegate<PdfAnnotationView, PdfAnnotationViewManager> = PdfAnnotationViewManagerDelegate(this)

  override fun getDelegate(): ViewManagerDelegate<PdfAnnotationView> {
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
    view?.viewModel?.updateBackgroundColor(value)
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

  override fun setAndroidBeyondViewportPageCount(view: PdfAnnotationView?, value: Int) {
    view?.viewModel?.updateBeyondViewportPageCount(value)
  }

  override fun setScrollDirection(
    view: PdfAnnotationView?,
    value: String?
  ) {
    view?.viewModel?.updateScrollDirection(value)
  }

  override fun saveAnnotations(view: PdfAnnotationView?, path: String?) {
    view?.viewModel?.saveAnnotations(path)
  }

  override fun loadAnnotations(view: PdfAnnotationView?, path: String?) {
    view?.viewModel?.loadAnnotations(path)
  }

  override fun setIosToolPickerVisible(view: PdfAnnotationView?, value: Boolean) {
    // NOOP, iOS only
  }

  override fun undo(view: PdfAnnotationView?) {
    view?.viewModel?.undo();
  }

  override fun redo(view: PdfAnnotationView?) {
    view?.viewModel?.redo();
  }

  override fun clear(view: PdfAnnotationView?) {
    view?.viewModel?.clear();
  }

  override fun setPage(view: PdfAnnotationView?, page: Int) {
    view?.viewModel?.setPage(page)
  }
}
