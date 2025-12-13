package com.graphite.platform.graphics.wgpu

fun <R> withWGPU(block: WGPUContext.() -> R): R {
    return block.invoke(WGPUManager.context)
}