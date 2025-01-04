package com.pdfannotation

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
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

  @ReactProp(name = "color")
  override fun setColor(view: PdfAnnotationView?, color: String?) {
    view?.setBackgroundColor(Color.parseColor(color))
  }

  companion object {
    const val NAME = "PdfAnnotationView"
  }
}
