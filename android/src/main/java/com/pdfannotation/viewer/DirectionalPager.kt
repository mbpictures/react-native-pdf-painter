package com.pdfannotation.viewer

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object NoSnapFlingBehavior : TargetedFlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return 0f // Keine Fling-Animation, direkter Stopp
    }

    override suspend fun ScrollScope.performFling(
        initialVelocity: Float,
        onRemainingDistanceUpdated: (Float) -> Unit
    ): Float {
        return 0f
    }
}

@Composable
fun DirectionalPager(
    direction: String,
    state: PagerState,
    userScrollEnabled: Boolean,
    beyondViewportPageCount: Int,
    modifier: Modifier = Modifier,
    enableSnapping: Boolean = false,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
) {
    val flingBehavior = if (enableSnapping) {
        PagerDefaults.flingBehavior(state = state) // Verwendet das Standard-Snapping-Verhalten
    } else {
        NoSnapFlingBehavior // Deaktiviert das Snapping
    }

    when (direction) {
        "vertical" -> VerticalPager(state=state, userScrollEnabled = userScrollEnabled, pageContent = pageContent, modifier = modifier, flingBehavior = flingBehavior)
        else -> HorizontalPager(state=state, userScrollEnabled = userScrollEnabled, beyondViewportPageCount = beyondViewportPageCount, pageContent = pageContent, modifier = modifier, flingBehavior = flingBehavior)
    }
}