package com.graphite.platform.window

data class WindowHints(
    val visible: Boolean = true,
    val resizable: Boolean = true,
    val fullscreen: Boolean = false
)