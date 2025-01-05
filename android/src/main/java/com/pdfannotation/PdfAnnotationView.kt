package com.pdfannotation

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import com.pdfannotation.viewer.PdfAnnotationViewModel
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

    viewModel = PdfAnnotationViewModel()
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
}
