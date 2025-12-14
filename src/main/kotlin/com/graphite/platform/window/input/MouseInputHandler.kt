package com.graphite.platform.window.input

import org.lwjgl.glfw.GLFW

object MouseInputHandler {
    var x: Float = 0.0f
        private set
    var y: Float = 0.0f
        private set

    fun setupForWindow(nativeHandle: Long) {
        GLFW.glfwSetCursorPosCallback(nativeHandle) { window, x, y ->
            this.x = x.toFloat()
            this.y = y.toFloat()
        }
    }
}