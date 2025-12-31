package com.graphite.platform.window.input

import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW

object MouseInputHandler {
    var x: Float = 0.0f
        private set
    var y: Float = 0.0f
        private set

    fun onClick(x: Float, y: Float, button: Int) {
        this.x = x
        this.y = y
        MinecraftClient.getInstance().currentScreen?.mouseClicked(x.toInt(), y.toInt(), button)
    }

    fun setupForWindow(nativeHandle: Long) {
        GLFW.glfwSetCursorPosCallback(nativeHandle) { window, x, y ->
            this.x = x.toFloat()
            this.y = y.toFloat()
        }

        GLFW.glfwSetMouseButtonCallback(nativeHandle) { window, button, action, mods ->
            onClick(x, y, button)
        }
    }
}