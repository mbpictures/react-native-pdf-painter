package com.pdfannotation.viewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class PdfRender(
    file: File,
    private val scale: Float = 1f,
    private val backgroundColor: Int?,
) {
    private val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val pageLists: List<Page> = List(pdfRenderer.pageCount) {
        Page(
            index = it,
            pdfRenderer = pdfRenderer,
            coroutineScope = coroutineScope,
            mutex = mutex,
            scale = scale,
            fileDescriptor = fileDescriptor,
            backgroundColor = backgroundColor
        )
    }

    fun close() {
        pageLists.forEach {
            it.recycle()
        }
        coroutineScope.coroutineContext.cancelChildren()
        pdfRenderer.close()
        fileDescriptor.close()
    }

    class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope,
        val scale: Float,
        val fileDescriptor: ParcelFileDescriptor,
        val backgroundColor: Int?
    ) {
        val hash get() = fileDescriptor.hashCode() + index
        private var isLoaded = false
        private var job: Job? = null
        val pageContent = MutableStateFlow<Bitmap?>(null)

        private fun createBitmap(width: Int, height: Int): Bitmap {
            return Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
        }

        fun load() {
            if (!isLoaded) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        val newBitmap: Bitmap
                        val currentPage = pdfRenderer.openPage(index)
                        try {
                            newBitmap = createBlankBitmap(
                                width = currentPage.width * scale,
                                height = currentPage.height * scale
                            )
                            currentPage.render(
                                newBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                        } finally {
                            currentPage.close()
                        }
                        isLoaded = true
                        pageContent.emit(newBitmap)
                    }
                }
            }
        }

        fun recycle() {
            job?.cancel()
            isLoaded = false
            val oldBitmap = pageContent.value
            pageContent.tryEmit(null)
            oldBitmap?.recycle()
        }

        private fun createBlankBitmap(
            width: Float,
            height: Float
        ): Bitmap {
            return createBitmap(
                width.toInt(),
                height.toInt()
            ).apply {
                val canvas = Canvas(this)
                if (backgroundColor != null) {
                    canvas.drawColor(backgroundColor)
                }
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }
    }

    data class Dimension(
        val width: Int,
        val height: Int
    )
}
