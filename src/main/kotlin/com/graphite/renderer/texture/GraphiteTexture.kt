package com.graphite.renderer.texture

import com.graphite.platform.graphics.wgpu.WGPUContext
import com.graphite.platform.graphics.wgpu.withWGPU
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.TextureViewDescriptor
import net.minecraft.util.Identifier

abstract class GraphiteTexture(val identifier: Identifier) {
    protected val texture by lazy {
        withWGPU {
            device.createTexture(createTextureDescriptor(this))
        }
    }

    abstract fun createTextureDescriptor(context: WGPUContext): TextureDescriptor
    abstract fun upload()

    fun createView() = texture.createView()

    fun close() {
        texture.close()
    }
}