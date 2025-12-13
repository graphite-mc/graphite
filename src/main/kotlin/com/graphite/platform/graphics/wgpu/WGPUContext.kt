package com.graphite.platform.graphics.wgpu

import io.ygdrasil.webgpu.Adapter
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.RenderingContext
import io.ygdrasil.webgpu.Surface
import io.ygdrasil.webgpu.WGPU

data class WGPUContext(
    val wgpu: WGPU,
    val surface: Surface,
    val adapter: Adapter,
    val device: GPUDevice,
    val renderingContext: RenderingContext
)