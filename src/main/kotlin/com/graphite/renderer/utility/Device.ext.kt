package com.graphite.renderer.utility

import com.graphite.utility.shortBufferOf
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.GPUBindGroupLayout
import io.ygdrasil.webgpu.GPUBindingResource
import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCommandEncoder
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.arrayBufferOf

fun GPUDevice.createBufferFromArray(input: FloatArray, vararg usage: GPUBufferUsage): GPUBuffer {
    val buffer = createBuffer(
        BufferDescriptor(
            size = (input.size * Float.SIZE_BYTES).toULong(),
            usage = usage.toSet()
        )
    )
    arrayBufferOf(input) {
        queue.writeBuffer(buffer, 0u, it)
    }

    return buffer
}

fun GPUDevice.createBufferFromArray(input: ShortArray, vararg usage: GPUBufferUsage): GPUBuffer {
    val buffer = createBuffer(
        BufferDescriptor(
            size = (input.size * Short.SIZE_BYTES).toULong(),
            usage = usage.toSet()
        )
    )
    shortBufferOf(input) {
        queue.writeBuffer(buffer, 0u, it)
    }

    return buffer
}

fun GPUDevice.withCommandEncoder(action: (encoder: GPUCommandEncoder) -> Unit) {
    val encoder = createCommandEncoder()
    action(encoder)
    val commandBuffer = encoder.finish()
    queue.submit(commandBuffer)
}

fun GPUDevice.createBindGroup(layout: GPUBindGroupLayout, vararg entries: Pair<UInt, GPUBindingResource>) =
    createBindGroup(BindGroupDescriptor(
        layout = layout,
        entries = entries.map { (binding, resource) ->
            BindGroupEntry(
                binding = binding,
                resource = resource
            )
        }
    ))