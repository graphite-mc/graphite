package com.graphite.renderer.utility

import io.ygdrasil.webgpu.GPUCommandBuffer
import io.ygdrasil.webgpu.GPUQueue

fun GPUQueue.submit(vararg commandBuffers: GPUCommandBuffer) {
    submit(commandBuffers.toList())
}