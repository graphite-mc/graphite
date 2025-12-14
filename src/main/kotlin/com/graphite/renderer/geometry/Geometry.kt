package com.graphite.renderer.geometry

import io.ygdrasil.webgpu.GPUBuffer

interface Geometry : AutoCloseable {
    val vertexBuffer: GPUBuffer
    val indexBuffer: GPUBuffer

    override fun close() {
        vertexBuffer.close()
        indexBuffer.close()
    }
}