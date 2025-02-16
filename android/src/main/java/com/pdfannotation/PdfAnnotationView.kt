package com.pdfannotation

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.events.Event
import com.pdfannotation.model.PdfAnnotationViewModel
import com.pdfannotation.viewer.PdfAnnotationViewer

class PdfAnnotationView : LinearLayout {
  internal lateinit var viewModel: PdfAnnotationViewModel

  constructor(context: Context) : super(context) {
    configureComponent(context)
  }
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    configureComponent(context)
  }
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  ) {
    configureComponent(context)
  }

  private fun configureComponent(context: Context) {
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      LayoutParams.WRAP_CONTENT
    )

    viewModel = PdfAnnotationViewModel(
      onPageCount = { pageCount -> emitPageCount(pageCount) },
      onPageChange = { currentPage -> emitPageChange(currentPage) },
      onDocumentFinished = { next -> emitDocumentFinished(next) },
      onTap = { x, y -> emitTap(x, y) }
    )
    ComposeView(context).also {
      it.layoutParams = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT
      )

      it.setContent {
        PdfAnnotationViewer(viewModel)
      }

      addView(it)
    }
  }

  private fun emitPageCount(pageCount: Int) {
    val reactContext = context as ReactContext
    val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
    val eventDispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, id)
    val payload = Arguments.createMap().apply {
      putInt("pageCount", pageCount)
    }
    val event = OnPageCountEvent(surfaceId, id, payload)

    eventDispatcher?.dispatchEvent(event)
  }

  private fun emitPageChange(currentPage: Int) {
    val reactContext = context as ReactContext
    val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
    val eventDispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, id)
    val payload = Arguments.createMap().apply {
      putInt("currentPage", currentPage)
    }
    val event = OnPageChangeEvent(surfaceId, id, payload)

    eventDispatcher?.dispatchEvent(event)
  }

  private fun emitDocumentFinished(next: Boolean) {
    val reactContext = context as ReactContext
    val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
    val eventDispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, id)
    val payload = Arguments.createMap().apply {
      putBoolean("next", next)
    }
    val event = OnDocumentFinishedEvent(surfaceId, id, payload)

    eventDispatcher?.dispatchEvent(event)
  }

  private fun emitTap(x: Float, y: Float) {
    val reactContext = context as ReactContext
    val surfaceId = UIManagerHelper.getSurfaceId(reactContext)
    val eventDispatcher = UIManagerHelper.getEventDispatcherForReactTag(reactContext, id)
    val payload = Arguments.createMap().apply {
      putDouble("x", x.toDouble())
      putDouble("y", y.toDouble())
    }
    val event = OnTapEvent(surfaceId, id, payload)

    eventDispatcher?.dispatchEvent(event)
  }

  inner class OnPageCountEvent(
    surfaceId: Int,
    viewId: Int,
    private val payload: WritableMap
  ) : Event<OnPageCountEvent>(surfaceId, viewId) {
    override fun getEventName() = "onPageCount"

    override fun getEventData() = payload
  }

  inner class OnPageChangeEvent(
    surfaceId: Int,
    viewId: Int,
    private val payload: WritableMap
  ) : Event<OnPageChangeEvent>(surfaceId, viewId) {
    override fun getEventName() = "onPageChange"

    override fun getEventData() = payload
  }

  inner class OnDocumentFinishedEvent(
    surfaceId: Int,
    viewId: Int,
    private val payload: WritableMap
  ) : Event<OnDocumentFinishedEvent>(surfaceId, viewId) {
    override fun getEventName() = "onDocumentFinished"

    override fun getEventData() = payload
  }

  inner class OnTapEvent(
    surfaceId: Int,
    viewId: Int,
    private val payload: WritableMap
  ) : Event<OnDocumentFinishedEvent>(surfaceId, viewId) {
    override fun getEventName() = "onTap"

    override fun getEventData() = payload
  }
}
