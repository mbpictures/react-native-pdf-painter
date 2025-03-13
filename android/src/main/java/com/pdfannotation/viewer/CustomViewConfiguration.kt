package com.pdfannotation.viewer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration


@Composable
fun CustomViewConfiguration(
    longPressTimeoutMillis: Long? = null,
    doubleTapTimeoutMillis: Long? = null,
    doubleTapMinTimeMillis: Long? = null,
    touchSlop: Float? = null,
    content: @Composable () -> Unit
) {
    fun ViewConfiguration.updateViewConfiguration() = object : ViewConfiguration {
        override val longPressTimeoutMillis
            get() = longPressTimeoutMillis ?: this@updateViewConfiguration.longPressTimeoutMillis

        override val doubleTapTimeoutMillis
            get() = doubleTapTimeoutMillis ?: this@updateViewConfiguration.doubleTapTimeoutMillis

        override val doubleTapMinTimeMillis
            get() =
                doubleTapMinTimeMillis ?: this@updateViewConfiguration.doubleTapMinTimeMillis

        override val touchSlop: Float
            get() = touchSlop ?: this@updateViewConfiguration.touchSlop
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides LocalViewConfiguration.current.updateViewConfiguration()
    ) {
        content()
    }
}