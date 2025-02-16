package com.pdfannotation.viewer

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import com.pdfannotation.model.Link
import com.pdfannotation.model.PdfAnnotationViewModel
import kotlinx.coroutines.launch

@Composable
fun PdfHorizontalPager(viewModel: PdfAnnotationViewModel) {
    val file by viewModel.pdfFile.collectAsState()
    val brushSettings by viewModel.brushSettings.collectAsState()
    val strokes by viewModel.strokes.collectAsState()
    val backgroundColor by viewModel.backgroundColor.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val links by viewModel.links.links.collectAsState()

    val scope = rememberCoroutineScope()
    val renderer = remember(file, backgroundColor) { file?.let {PdfRender(it, 3f, backgroundColor) }}
    val pagerState = rememberPagerState(pageCount = {renderer?.pageCount ?: 1})
    val canScroll by remember { derivedStateOf { brushSettings == null } }

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

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            userScrollEnabled = canScroll
        ) { page ->
            PdfPage(
                page = renderer?.let { it.pageLists[page] },
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
                onTap = { x, y, normalizedX, normalizedY ->
                    if (viewModel.links.canCreateLinks) {
                        viewModel.links.addLink(page, Link(normalizedX, normalizedY, 100f, 100f, Color.Red))
                    } else {
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
                }
            )
        }
    }
}
