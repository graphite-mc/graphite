package com.graphite.utility

import com.graphite.platform.graphics.wgpu.withWGPU
import io.ygdrasil.webgpu.SurfaceConfiguration

object ReconfigurationManager {
    private val queuedReconfigurations = ArrayList<SurfaceConfiguration>()

    fun queueReconfigure(configuration: SurfaceConfiguration) {
        queuedReconfigurations.add(configuration)
    }

    fun flushLast() = withWGPU {
        if (queuedReconfigurations.isNotEmpty()) {
            queuedReconfigurations.last().apply {
                surface.configure(this)
                queuedReconfigurations.remove(this)
            }
        }
    }
}