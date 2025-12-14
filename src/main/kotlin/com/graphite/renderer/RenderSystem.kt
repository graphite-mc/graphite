package com.graphite.renderer

import com.graphite.renderer.pathway.core.BlitRenderer
import com.graphite.renderer.targets.RenderTarget
import net.minecraft.client.MinecraftClient

object RenderSystem {
    private lateinit var renderTarget: RenderTarget

    private val client get() = MinecraftClient.getInstance()

    fun initialize() {
        renderTarget = RenderTarget(client.width, client.height, true)
        renderTarget.init()
        BlitRenderer.init()
    }

    fun resize(width: Int, height: Int) {
        renderTarget.resize(width, height)
    }

    fun defaultRenderTarget() = renderTarget
}