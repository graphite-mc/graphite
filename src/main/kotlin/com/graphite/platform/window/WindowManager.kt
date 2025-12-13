package com.graphite.platform.window

object WindowManager {
    var k: Window? = null

    fun setActiveWindow(window: Window) {
        k = window
    }
}