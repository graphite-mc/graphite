package com.graphite.utility

import com.graphite.platform.graphics.wgpu.WGPUManager

object Hooks {
    @JvmStatic @JvmName("getMaxTexSize2D") fun getMaxTexSize2D() = WGPUManager.device.limits.maxTextureDimension2D }