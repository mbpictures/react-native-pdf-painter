package com.pdfannotation.model

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class LinkCreationState {
    DISABLED,
    CREATE_FIRST,
    CREATE_SECOND
}

data class Link(
    val id: String,
    var targetId: String?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val color: Color
) {
    constructor(x: Float, y: Float, width: Float, height: Float, color: Color) : this(
        UUID.randomUUID().toString(),
        null,
        x,
        y,
        width,
        height,
        color
    )
}

class Links(
    private var onLinkCompleted: ((from: Int, to: Int) -> Unit)? = null,
    private var lastCreatedLink: Link? = null,
    private var linkCreationState: LinkCreationState = LinkCreationState.DISABLED
) : ViewModel() {
    private val _links = MutableStateFlow<Map<Int, Set<Link>>>(mutableMapOf())
    val links: StateFlow<Map<Int, Set<Link>>> get() = _links
    var size: Float = 100f
    var color: Color = Color.Red

    var canCreateLinks: Boolean
        get() = linkCreationState != LinkCreationState.DISABLED
        set(value) {
            linkCreationState = if (value) {
                LinkCreationState.CREATE_FIRST
            } else {
                LinkCreationState.DISABLED
            }
        }

    fun getLinksForPage(pageNumber: Int): Set<Link> {
        return links.value[pageNumber] ?: emptySet()
    }

    fun getLink(id: String): Link? {
        return links.value.values.flatten().find { it.id == id }
    }

    fun addLink(page: Int, link: Link) {
        _links.update { current ->
            current.toMutableMap().apply {
                this[page] = (this[page] ?: emptySet()) + link
            }
        }
        if (linkCreationState == LinkCreationState.CREATE_FIRST) {
            linkCreationState = LinkCreationState.CREATE_SECOND
            lastCreatedLink = link
        } else if (linkCreationState == LinkCreationState.CREATE_SECOND) {
            linkCreationState = LinkCreationState.DISABLED
            connectLinks(lastCreatedLink!!.id, link.id)
            onLinkCompleted?.invoke(getPageOfLink(lastCreatedLink!!.id)!!, getPageOfLink(link.id)!!)
            lastCreatedLink = null
        }
    }

    private fun connectLinks(mId: String, mTargetId: String) {
        _links.update { current ->
            current.mapValues { (_, links) ->
                links.map { link ->
                    if (link.id == mId) link.copy(targetId = mTargetId)
                    else if (link.id == mTargetId) link.copy(targetId = mId)
                    else link
                }.toSet()
            }.toMutableMap()
        }
    }

    fun removeLink(id: String) {
        _links.value = links.value.toMutableMap().apply {
            forEach { (page, linkSet) ->
                this[page] = linkSet.toMutableSet().apply {
                    removeIf { it.id == id }
                }
            }
        }
    }

    fun getPageOfLink(id: String): Int? {
        return links.value.entries.find { it.value.any { link -> link.id == id } }?.key
    }

    fun initialLinks(links: Map<Int, Set<Link>>) {
        _links.value = links
    }
}