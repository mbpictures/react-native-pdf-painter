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
    private val pdfRenderer = try {
        PdfRenderer(fileDescriptor)
    } catch (e: Exception) {
        null
    }
    val pageCount get() = pdfRenderer?.pageCount ?: 0
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val pageLists: List<Page> = List(pageCount) {
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
        pdfRenderer?.close()
        fileDescriptor.close()
    }

    class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer?,
        val coroutineScope: CoroutineScope,
        val scale: Float,
        val fileDescriptor: ParcelFileDescriptor,
        val backgroundColor: Int?
    ) {
        val hash get() = fileDescriptor.hashCode() + index
        private var isLoaded = false
        private var job: Job? = null
        val pageContent = MutableStateFlow<Bitmap?>(null)
        var currentPage: PdfRenderer.Page? = null

        private fun createBitmap(width: Int, height: Int): Bitmap {
            return Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
        }

        fun load() {
            if (!isLoaded && job?.isActive != true) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        if (isLoaded) return@withLock // Double-check nach dem Lock

                        var newBitmap: Bitmap? = null
                        var page: PdfRenderer.Page? = null

                        try {
                            // Validiere pdfRenderer
                            val renderer = pdfRenderer ?: return@withLock

                            page = renderer.openPage(index)
                            currentPage = page

                            if (page.width <= 0 || page.height <= 0) {
                                return@withLock
                            }

                            val bitmapWidth = (page.width * scale).toInt()
                            val bitmapHeight = (page.height * scale).toInt()

                            // check for max size (Android Limit)
                            if (bitmapWidth <= 0 || bitmapHeight <= 0 ||
                                bitmapWidth > 8192 || bitmapHeight > 8192) {
                                return@withLock
                            }

                            newBitmap = createBlankBitmap(
                                width = bitmapWidth.toFloat(),
                                height = bitmapHeight.toFloat()
                            )

                            if (newBitmap != null && !newBitmap.isRecycled) {
                                page.render(
                                    newBitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PdfRender", "Error rendering page $index", e)
                            newBitmap?.recycle()
                            newBitmap = null
                        } finally {
                            try {
                                page?.close()
                            } catch (_: Exception) {}
                            currentPage = null
                        }

                        if (newBitmap != null && !newBitmap.isRecycled) {
                            pageContent.emit(newBitmap)
                            isLoaded = true
                        }
                    }
                }
            }
        }

        fun recycle() {
            job?.cancel()
            try {
                currentPage?.close()
            } catch (_: Exception) {}
            isLoaded = false
            val oldBitmap = pageContent.value
            pageContent.tryEmit(null)
            oldBitmap?.recycle()
        }

        private fun createBlankBitmap(
            width: Float,
            height: Float
        ): Bitmap? {
            return try {
                val w = width.toInt().coerceAtLeast(1)
                val h = height.toInt().coerceAtLeast(1)

                createBitmap(w, h).apply {
                    val canvas = Canvas(this)
                    if (backgroundColor != null) {
                        canvas.drawColor(backgroundColor)
                    }
                    // Entferne die problematische Zeile - sie zeichnet das Bitmap auf sich selbst
                    // canvas.drawBitmap(this, 0f, 0f, null)
                }
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("PdfRender", "Out of memory creating bitmap ${width}x${height}", e)
                null
            } catch (e: Exception) {
                android.util.Log.e("PdfRender", "Error creating bitmap ${width}x${height}", e)
                null
            }
        }
    }

    data class Dimension(
        val width: Int,
        val height: Int
    )
}
