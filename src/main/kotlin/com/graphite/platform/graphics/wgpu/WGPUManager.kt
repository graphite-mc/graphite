package com.graphite.platform.graphics.wgpu

import com.graphite.platform.api.Os
import com.graphite.platform.api.Platform
import com.sun.jna.Pointer
import io.ygdrasil.webgpu.WGPU
import io.ygdrasil.webgpu.WGPUInstanceBackend
import com.sun.jna.platform.win32.Kernel32
import darwin.CAMetalLayer
import darwin.NSWindow
import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.DeviceDescriptor
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.NativeSurface
import io.ygdrasil.webgpu.RenderingContext
import io.ygdrasil.webgpu.Surface
import io.ygdrasil.webgpu.SurfaceRenderingContext
import io.ygdrasil.webgpu.toNativeAddress
import org.lwjgl.glfw.GLFWNativeCocoa.glfwGetCocoaWindow
import org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandDisplay
import org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandWindow
import org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Display
import org.lwjgl.glfw.GLFWNativeX11.glfwGetX11Window
import org.rococoa.ID
import org.rococoa.Rococoa

object WGPUManager {
    lateinit var wgpu: WGPU
        private set
    lateinit var surface: Surface
        private set
    lateinit var adapter: Adapter
        private set
    lateinit var device: GPUDevice
        private set
    lateinit var renderingContext: RenderingContext
        private set

    lateinit var context: WGPUContext
        private set

    suspend fun setupWGPU(windowHandle: Long) {
        this.wgpu = WGPU.createInstance(
            backend = WGPUInstanceBackend.Vulkan
        ) ?: throw WGPUException("Could not create WGPU instance.")

        val nativeSurface = wgpu.getNativeSurface(windowHandle)

        this.surface = Surface(nativeSurface, windowHandle)

        this.adapter = wgpu.requestAdapter(nativeSurface)
            ?: throw WGPUException("Could not get WGPU adapter.")

        this.device = adapter.requestDevice(
            DeviceDescriptor(
                onUncapturedError = {
                    throw WGPUException("Uncaptured WGPU error: ${it.message}")
                }
            )
        ).getOrElse {
            throw WGPUException("Could not get WGPU device: ${it.message}")
        }

        nativeSurface.computeSurfaceCapabilities(adapter)

        this.renderingContext = SurfaceRenderingContext(surface, surface.supportedFormats.first())

        this.context = WGPUContext(wgpu, surface, adapter, device, renderingContext)
    }
}

private fun WGPU.getNativeSurface(window: Long): NativeSurface = when (Platform.os) {
    Os.Linux -> when {
        glfwGetWaylandWindow(window) == 0L -> {
            val display = glfwGetX11Display().toNativeAddress()
            val x11_window = glfwGetX11Window(window).toULong()
            getSurfaceFromX11Window(display, x11_window) ?: error("fail to get surface on Linux")
        }

        else -> {
            val display = glfwGetWaylandDisplay().toNativeAddress()
            val wayland_window = glfwGetWaylandWindow(window).toNativeAddress()
            getSurfaceFromWaylandWindow(display, wayland_window)
        }
    }

    Os.Windows -> {
        val hwnd = glfwGetWin32Window(window).toNativeAddress()
        val hinstance = Kernel32.INSTANCE.GetModuleHandle(null).pointer.toNativeAddress()
        getSurfaceFromWindows(hinstance, hwnd) ?: error("fail to get surface on Windows")
    }

    Os.MacOs -> {
        val nsWindowPtr = glfwGetCocoaWindow(window)
        val nswindow = Rococoa.wrap(ID.fromLong(nsWindowPtr), NSWindow::class.java)
        nswindow.contentView()?.setWantsLayer(true)
        val layer = CAMetalLayer.layer()
        nswindow.contentView()?.setLayer(layer.id().toLong().toPointer())
        getSurfaceFromMetalLayer(layer.id().toLong().toNativeAddress())
    }
} ?: error("fail to get surface")


private fun Long.toPointer(): Pointer = Pointer(this)

