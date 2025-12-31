package com.graphite.renderer.pathway.game

import com.graphite.platform.window.input.MouseInputHandler
import com.graphite.renderer.pathway.ui.BatchedRenderer
import net.minecraft.client.MinecraftClient

object GraphiteGameRenderer {
    private val client get() = MinecraftClient.getInstance()

    fun render(tickDelta: Float, nanoTime: Long) {
        val mouseX = MouseInputHandler.x
        val mouseY = MouseInputHandler.y

        if (!client.skipGameRender) {
            // TODO: Level renderer
            if (this.client.currentScreen != null) {
                this.client.currentScreen.render(mouseX.toInt(), mouseY.toInt(), tickDelta)
            }
        }

        BatchedRenderer.withBatch {
            val fpsText = "${MinecraftClient.getCurrentFps()} fps"
            drawTextWithShadow(fpsText, 5f, 5f, 2f, -1)
        }
    }
}