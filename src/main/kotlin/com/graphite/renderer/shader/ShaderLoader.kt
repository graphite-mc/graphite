package com.graphite.renderer.shader

import com.graphite.platform.graphics.wgpu.WGPUContext
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUShaderModule
import io.ygdrasil.webgpu.ShaderModuleDescriptor

object ShaderLoader {
    fun loadShader(device: GPUDevice, name: String): GPUShaderModule {
        return device.createShaderModule(ShaderModuleDescriptor(
            code = javaClass.getResourceAsStream("/assets/graphite/shaders/$name.wgsl")!!.readBytes().decodeToString()
        ))
    }
}