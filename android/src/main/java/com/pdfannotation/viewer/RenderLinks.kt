package com.pdfannotation.viewer

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import com.pdfannotation.model.Link

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RenderLinks(
    links: Set<Link>,
    onLinkClick: (Link) -> Unit,
    onLinkRemove: (Link) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    Log.d("RenderLinks", "RenderLinks: ${links.size}")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        links.forEach { link ->
            val widthDp = with(density) { link.width.toDp() }
            val heightDp = with(density) { link.height.toDp() }
            Box(
                modifier = Modifier
                    .size(widthDp, heightDp)
                    .graphicsLayer(
                        translationX = link.x * size.width,
                        translationY = link.y * size.height
                    )
                    .background(link.color)
                    .combinedClickable(
                        onClick = { onLinkClick(link) },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLinkRemove(link)
                        }
                    )
            )
        }
    }
}