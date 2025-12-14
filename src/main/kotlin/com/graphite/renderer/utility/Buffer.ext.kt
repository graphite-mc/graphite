package com.graphite.renderer.utility

import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.GPUBuffer

fun GPUBuffer.binding(size: ULong) = BufferBinding(
    buffer = this,
    size = size
)