package com.graphite.game

import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.platform.window.getWindow
import com.graphite.renderer.RenderSystem
import com.graphite.renderer.pathway.core.BlitRenderer
import com.graphite.renderer.pathway.game.GraphiteGameRenderer
import com.graphite.utility.ReconfigurationManager
import io.ygdrasil.webgpu.Color
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Util
import org.apache.logging.log4j.LogManager

object MinecraftGameHandler {
    private val logger = LogManager.getLogger("Graphite Minecraft Client")
    
    fun runGameLoop(client: MinecraftClient) {
        val nanoTime = System.nanoTime()
        val window = getWindow()

        if (window.shouldClose) {
            client.scheduleStop()
        }

        client.ticker.tick()
        
        if (client.paused && client.world != null) {
            val tickDelta = client.ticker.tickDelta
            client.ticker.tick()
            client.ticker.tickDelta = tickDelta
        } else {
            client.ticker.tick()
        }
        
        synchronized(client.tasks) {
            while(!client.tasks.isEmpty()) {
                Util.executeTask(client.tasks.poll(), logger)
            }
        }

        for (i in 0..<client.ticker.ticksThisFrame) {
        }

        ReconfigurationManager.flushLast()

        RenderSystem.defaultRenderTarget().render(clearColor = Color(0.0, 0.0, 0.0, 1.0)) {}

        if (!client.skipGameRender) {
            GraphiteGameRenderer.render(client.ticker.tickDelta, nanoTime)
        }

        BlitRenderer.render(RenderSystem.defaultRenderTarget().colorView)

        withWGPU { 
            surface.present()
        }
        window.update()
        client.fpsCounter++

        while(MinecraftClient.getTime() >= client.time + 1000L) {
            MinecraftClient.currentFps = client.fpsCounter
            client.fpsDebugString = String.format("%d fps", MinecraftClient.currentFps)
            client.time += 1000L
            client.fpsCounter = 0
        }
    }
}