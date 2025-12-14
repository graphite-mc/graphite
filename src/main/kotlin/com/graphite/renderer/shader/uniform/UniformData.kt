package com.graphite.renderer.shader.uniform

import com.graphite.math.UniformElement
import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.arrayBufferOf

class UniformData(
    vararg val elements: UniformElement
) {
    val size get() = elements.sumOf { it.size }.toULong()

    fun writeInto(device: GPUDevice, buffer: GPUBuffer): ULong {
        val arrays = elements.map { it.data }

        val totalSize = arrays.sumOf { it.size }
        val combined = FloatArray(totalSize)

        var offset = 0
        for (arr in arrays) {
            arr.copyInto(
                destination = combined,
                destinationOffset = offset
            )
            offset += arr.size
        }

        arrayBufferOf(combined) {
            device.queue.writeBuffer(
                buffer = buffer,
                bufferOffset = 0u,
                data = it
            )
        }

        return size
    }
}