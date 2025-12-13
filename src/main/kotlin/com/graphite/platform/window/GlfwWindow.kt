package com.graphite.platform.window

import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.ByteBuffer
import kotlin.properties.Delegates

class GlfwWindow : Window {
    override var nativeHandle by Delegates.notNull<Long>()

    override var width by Delegates.notNull<Int>()
    override var height by Delegates.notNull<Int>()

    override var title: String = ""
        set(value) {
            if (field != value) {
                GLFW.glfwSetWindowTitle(nativeHandle, value)
                field = value
            }
        }

    override val shouldClose: Boolean
        get() = GLFW.glfwWindowShouldClose(nativeHandle)

    override var vsync: Boolean = false
        set(value) {
            field = value
            GLFW.glfwSwapInterval(if (value) 1 else 0)
        }

    private var fullscreenMonitor: Long = NULL

    override fun create(
        width: Int,
        height: Int,
        title: String,
        hints: WindowHints
    ) {
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, if (hints.visible) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, if (hints.resizable) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API)

        if (hints.fullscreen) {
            fullscreenMonitor = GLFW.glfwGetPrimaryMonitor()
            val mode: GLFWVidMode = GLFW.glfwGetVideoMode(fullscreenMonitor)!!
            this.nativeHandle = GLFW.glfwCreateWindow(
                mode.width(),
                mode.height(),
                title,
                fullscreenMonitor,
                NULL
            )
            this.width = mode.width()
            this.height = mode.height()
        } else {
            this.nativeHandle = GLFW.glfwCreateWindow(width, height, title, NULL, NULL)
            this.width = width
            this.height = height
        }
    }

    fun setFullscreen(enabled: Boolean) {
        val monitor = GLFW.glfwGetPrimaryMonitor()
        val mode = GLFW.glfwGetVideoMode(monitor)!!

        if (enabled) {
            GLFW.glfwSetWindowMonitor(nativeHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
        } else {
            GLFW.glfwSetWindowMonitor(nativeHandle, NULL, 100, 100, width, height, mode.refreshRate())
        }
    }

    fun setIcon(w: Int, h: Int, pixels: ByteBuffer)  {
        val icon: GLFWImage.Buffer = GLFWImage.create(1)
        icon.width(w)
        icon.height(h)
        icon.pixels(pixels)
        GLFW.glfwSetWindowIcon(nativeHandle, icon)
    }

    override fun update() {
        GLFW.glfwPollEvents()
        GLFW.glfwSwapBuffers(nativeHandle)
    }

    override fun destroy() {
        GLFW.glfwDestroyWindow(nativeHandle)
    }
}
