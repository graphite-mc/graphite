package com.graphite.renderer.geometry.impl

import com.graphite.renderer.geometry.Geometry
import com.graphite.renderer.utility.createBufferFromArray
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUDevice

class SimpleGeometry(
    device: GPUDevice,
    vertices: FloatArray,
    indices: ShortArray? = null
) : Geometry {
    override val vertexBuffer = device.createBufferFromArray(vertices, GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst)
    override val indexBuffer = indices?.let {
        device.createBufferFromArray(it, GPUBufferUsage.Index, GPUBufferUsage.CopyDst)
    } ?: error("Requested index buffer, but no indices were provided")
}