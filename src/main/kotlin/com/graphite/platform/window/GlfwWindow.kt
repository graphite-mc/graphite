package com.graphite.platform.window

import com.graphite.platform.graphics.wgpu.WGPUManager
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.PresentMode
import io.ygdrasil.webgpu.SurfaceConfiguration
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
            WGPUManager.surface.configure(SurfaceConfiguration(
                device = WGPUManager.device,
                format = GPUTextureFormat.RGBA8Unorm,
                presentMode = if (value) PresentMode.Fifo else PresentMode.Mailbox
            ))
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

        GLFW.glfwSetWindowSizeCallback(nativeHandle) { handle, w, h ->
            WGPUManager.surface.configure(SurfaceConfiguration(
                device = WGPUManager.device,
                format = GPUTextureFormat.RGBA8Unorm,
                presentMode = if (vsync) PresentMode.Fifo else PresentMode.Mailbox
            ))
            this.width = w
            this.height = h
        }
    }

    override fun setFullscreen(enabled: Boolean) {
        val monitor = GLFW.glfwGetPrimaryMonitor()
        val mode = GLFW.glfwGetVideoMode(monitor)!!

        if (enabled) {
            GLFW.glfwSetWindowMonitor(nativeHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate())
        } else {
            GLFW.glfwSetWindowMonitor(nativeHandle, NULL, 100, 100, width, height, mode.refreshRate())
        }
    }

    override fun setSize(width: Int, height: Int) {
        GLFW.glfwSetWindowSize(nativeHandle, width, height)
    }

    fun setIcon(vararg icons: Icon)  {
        val glfwIcons = GLFWImage.malloc(icons.size)
        for ((i, icon) in icons.withIndex()) {
            glfwIcons.get(i)
                .set(icon.width, icon.height, icon.pixels)
        }
        GLFW.glfwSetWindowIcon(nativeHandle, glfwIcons)
        glfwIcons.free()
    }

    override fun update() {
        GLFW.glfwPollEvents()
    }

    override fun destroy() {
        GLFW.glfwDestroyWindow(nativeHandle)
    }

    data class Icon(
        val width: Int,
        val height: Int,
        val pixels: ByteBuffer
    )
}
