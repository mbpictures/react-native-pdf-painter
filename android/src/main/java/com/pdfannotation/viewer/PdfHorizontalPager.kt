package com.pdfannotation.viewer

import android.graphics.Matrix
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokesView
import com.pdfannotation.canvas.rememberInProgressStrokesView
import com.pdfannotation.model.Link
import com.pdfannotation.model.PdfAnnotationViewModel
import kotlinx.coroutines.launch

data class Zoom (
    val scale: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f
)

@Composable
fun PdfHorizontalPager(viewModel: PdfAnnotationViewModel) {
    val file by viewModel.pdfFile.collectAsState()
    val brushSettings by viewModel.brushSettings.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val links by viewModel.links.links.collectAsState()
    val beyondViewportPageCount by viewModel.beyondViewportPageCount.collectAsState()
    val direction by viewModel.scrollDirection.collectAsState()

    var size by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val renderer = remember(file, backgroundColor) { file?.let {PdfRender(it, 3f, backgroundColor) }}
    val pagerState = rememberPagerState(pageCount = {renderer?.pageCount ?: 1})
    val canScroll by remember { derivedStateOf { brushSettings == null } }
    val inProgressStrokesView: InProgressStrokesView = rememberInProgressStrokesView()

    var zoom by remember { mutableStateOf(Zoom()) }
    var childSize by remember { mutableStateOf(Size(1f, 1f)) }
    val transformMatrix = remember(zoom.scale) {
        Matrix().apply {
            preScale(1 / zoom.scale, 1 / zoom.scale)
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            renderer?.close()
        }
    }

    LaunchedEffect(pagerState.targetPage) {
        viewModel.setPage(pagerState.targetPage)
    }

    LaunchedEffect(renderer) {
        viewModel.setPageCount(renderer?.pageCount ?: 0)
    }

    LaunchedEffect(currentPage) {
        if (currentPage != pagerState.targetPage && currentPage != pagerState.currentPage) {
            scope.launch {
                pagerState.animateScrollToPage(currentPage)
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        renderer?.pageLists?.forEach { page ->
            if (kotlin.math.abs(page.index - pagerState.currentPage) > (beyondViewportPageCount?: PagerDefaults.BeyondViewportPageCount) + 1) {
                page.recycle()
            } else {
                page.load()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DirectionalPager(
            direction = direction,
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size = it }
                .clipToBounds(),
            userScrollEnabled = canScroll,
            beyondViewportPageCount = beyondViewportPageCount ?: PagerDefaults.BeyondViewportPageCount
        ) { page ->
            PdfPage(
                page = renderer?.let { it.pageLists[page] },
                backgroundColor = backgroundColor,
                brushSettings = brushSettings,
                viewModel = strokes,
                onChangePage = { pageDelta ->
                    if (viewModel.links.canCreateLinks) {
                        return@PdfPage
                    }
                    scope.launch {
                        val nextPage = pagerState.targetPage + pageDelta
                        if (nextPage < 0 || nextPage >= pagerState.pageCount) {
                            viewModel.handleDocumentFinished(pageDelta > 0)
                            return@launch
                        }
                        pagerState.animateScrollToPage(nextPage)
                    }
                },
                onTap = { x, y, normalizedX, normalizedY, changePage ->
                    if (viewModel.links.canCreateLinks) {
                        viewModel.links.addLink(page, Link(normalizedX, normalizedY, viewModel.links.size, viewModel.links.size, viewModel.links.color, null))
                    } else if (!changePage) {
                        viewModel.handleTap(x, y)
                    }
                },
                links = links[page] ?: emptySet(),
                onLink = { link ->
                    link.targetId?.let { id ->
                        viewModel.links.getPageOfLink(id)?.let { page ->
                            scope.launch {
                                pagerState.animateScrollToPage(page)
                            }
                        }
                    }
                },
                onLinkRemove = { link ->
                    viewModel.links.removeLink(link.id)
                },
                findPage = { id ->
                    viewModel.links.getPageOfLink(id) ?: 0
                },
                containerSize = size,
                inProgressStrokesView = inProgressStrokesView,
                setTransformMatrix = { scale, offsetX, offsetY ->
                    zoom = Zoom(scale, offsetX, offsetY)
                },
                setChildSize = { size ->
                    childSize = size
                },
                state = pagerState
            )
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(childSize.aspectRatio(), matchHeightConstraintsFirst = size.width > size.height)
                .graphicsLayer(
                    scaleX = zoom.scale,
                    scaleY = zoom.scale,
                    translationX = zoom.translateX,
                    translationY = zoom.translateY
                )
                .clipToBounds(),
            factory = {
                inProgressStrokesView.apply {
                    layoutParams =
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT,
                        )
                }
            },
            update = { inProgressStrokesView ->
                inProgressStrokesView.motionEventToViewTransform = transformMatrix
            }
        )
    }
}
