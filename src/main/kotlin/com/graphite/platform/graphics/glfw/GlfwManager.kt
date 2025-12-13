package com.graphite.platform.graphics.glfw

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback

object GlfwManager {
    private lateinit var errorCallback: GLFWErrorCallback

    fun initializeGlfw() {
        this.errorCallback = GLFWErrorCallback.createPrint()
        GLFW.glfwSetErrorCallback(this.errorCallback)

        if (!GLFW.glfwInit()) {
            throw GlfwException("Failed to initialize GLFW")
        }
    }
}