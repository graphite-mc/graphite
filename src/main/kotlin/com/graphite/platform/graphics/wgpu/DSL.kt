package com.graphite.platform.graphics.wgpu

fun withWGPU(block: WGPUContext.() -> Unit) {
    block.invoke(WGPUManager.context)
}