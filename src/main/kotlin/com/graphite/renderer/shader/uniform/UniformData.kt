package com.graphite.renderer.shader.uniform

import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.arrayBufferOf
import org.lwjgl.util.vector.Vector2f

class UniformData(
    vararg val elements: Vector2f
) {
    val size get() = elements.sumOf { 2 * Float.SIZE_BYTES }.toULong()

    fun writeInto(device: GPUDevice, buffer: GPUBuffer): ULong {
        val arrays = elements.map { arrayOf(it.x, it.y) }

        val totalSize = arrays.sumOf { it.size }
        val combined = FloatArray(totalSize).toTypedArray()

        var offset = 0
        for (arr in arrays) {
            arr.copyInto(
                destination = combined,
                destinationOffset = offset
            )
            offset += arr.size
        }

        arrayBufferOf(combined.toFloatArray()) {
            device.queue.writeBuffer(
                buffer = buffer,
                bufferOffset = 0u,
                data = it
            )
        }

        return size
    }
}