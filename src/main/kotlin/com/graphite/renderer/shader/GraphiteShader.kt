package com.graphite.renderer.shader

import com.graphite.platform.graphics.wgpu.withWGPU
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import net.minecraft.util.Identifier

class GraphiteShader(private val id: Identifier) {
    val module by lazy {
        withWGPU {
            device.createShaderModule(ShaderModuleDescriptor(
                code = javaClass.getResourceAsStream("/assets/${id.namespace}/shaders/${id.path}.wgsl")!!.readBytes().decodeToString()
            ))
        }
    }
}