package com.graphite.game.screens

import com.graphite.renderer.pathway.ui.BatchedRenderer
import net.minecraft.client.gui.screen.Screen
import java.awt.Color

class GraphiteTitleScreen : Screen() {
    override fun render(mouseX: Int, mouseY: Int, tickDelta: Float) = BatchedRenderer.withBatch {
        drawQuad(10f, 10f, 100f, 15f, Color.RED)
        drawTextWithShadow("§cHello, §bworld§1!", 10f, 10f, 4f, Color.BLUE.rgb)
    }
}