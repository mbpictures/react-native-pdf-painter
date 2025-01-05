package com.pdfannotation.viewer

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PdfHorizontalPager(viewModel: PdfAnnotationViewModel) {
    val file by viewModel.pdfFile.collectAsState()
    val brushSettings by viewModel.brushSettings.collectAsState()
    val hidePagination by viewModel.hidePagination.collectAsState()
    val strokes by viewModel.strokes.collectAsState()

    val scope = rememberCoroutineScope()
    val renderer = remember(file) { file?.let {PdfRender(it, 3f) }}
    val pagerState = rememberPagerState(pageCount = {renderer?.pageCount ?: 1})
    val canScroll by remember { derivedStateOf { brushSettings == null } }

    DisposableEffect (viewModel) {
        onDispose {
            if (viewModel.autoSave.value) {
                viewModel.saveAnnotations()
            }
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            renderer?.close()
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
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.targetPage + pageDelta)
                    }
                }
            )
        }
        if (!hidePagination) {
            PagerIndicator(
                pageCount = pagerState.pageCount,
                currentPageIndex = pagerState.currentPage,
                onNavigate = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
            )
        }
    }
}

@Composable
fun PagerIndicator(pageCount: Int, currentPageIndex: Int, modifier: Modifier = Modifier, onNavigate: (Int) -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pageCount) { iteration ->
                val color = if (currentPageIndex == iteration) Color.DarkGray else Color.LightGray
                Box(
                    modifier = modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(16.dp)
                        .clickable {
                            onNavigate(iteration)
                        }
                )
            }
        }
    }
}
