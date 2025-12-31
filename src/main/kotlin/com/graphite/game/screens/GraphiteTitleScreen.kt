package com.graphite.game.screens

import com.graphite.game.screens.titlescreen.PanoramaRenderer
import com.graphite.renderer.RenderSystem
import com.graphite.renderer.pathway.ui.BatchedRenderer
import com.graphite.renderer.texture.TextureMgr
import com.graphite.renderer.utility.createTexture
import com.graphite.renderer.utility.getTextureView
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.util.Identifier
import kotlin.random.Random

object GraphiteTitleScreen : Screen() {
    private val minecraftRandomNumber = Random.nextFloat()

    private val minecraftTitleTextureView by lazy {
        client.textureManager.createTexture(Identifier("textures/gui/title/minecraft.png"))
    }

    init {
        PanoramaRenderer.init(MinecraftClient.getInstance())
    }

    override fun render(mouseX: Int, mouseY: Int, tickDelta: Float) {
        PanoramaRenderer.render(RenderSystem.defaultRenderTarget().colorView, tickDelta)

        BatchedRenderer.withBatch {
            val mcTitleTexture = bindFrom(minecraftTitleTextureView)

            val l = client.width / 2 - 137 // 274 / 2
            val m = 30
            val scale = 2.0

            if (minecraftRandomNumber < 1.0E-4) {
                drawQuad(
                    ((l + 0) * scale).toFloat(), (m * scale).toFloat(), (99 * scale).toFloat(), (44 * scale).toFloat(),
                    (0f / 256f), (0f / 256f), (99f / 256f), (44f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )

                drawQuad(
                    ((l + 99) * scale).toFloat(), (m * scale).toFloat(), (27 * scale).toFloat(), (44 * scale).toFloat(),
                    (129f / 256f), (0f / 256f), (156f / 256f), (44f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )

                drawQuad(
                    ((l + 99 + 26) * scale).toFloat(), (m * scale).toFloat(), (3 * scale).toFloat(), (44 * scale).toFloat(),
                    (126f / 256f), (0f / 256f), (129f / 256f), (44f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )

                drawQuad(
                    ((l + 99 + 26 + 3) * scale).toFloat(), (m * scale).toFloat(), (26 * scale).toFloat(), (44 * scale).toFloat(),
                    (99f / 256f), (0f / 256f), (125f / 256f), (44f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )

                drawQuad(
                    ((l + 155) * scale).toFloat(), (m * scale).toFloat(), (155 * scale).toFloat(), (44 * scale).toFloat(),
                    (0f / 256f), (45f / 256f), (155f / 256f), (89f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )
            } else {
                drawQuad(
                    ((l + 0) * scale).toFloat(), (m * scale).toFloat(), (155 * scale).toFloat(), (44 * scale).toFloat(),
                    (0f / 256f), (0f / 256f), (155f / 256f), (44f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )

                drawQuad(
                    ((l + 155) * scale).toFloat(), (m * scale).toFloat(), (155 * scale).toFloat(), (44 * scale).toFloat(),
                    (0f / 256f), (45f / 256f), (155f / 256f), (89f / 256f),
                    1f, 1f, 1f, 1f,
                    mcTitleTexture
                )
            }

            val copyRightText = "Copyright Mojang AB. Do not distribute!"
            val versionText = "Minecraft 1.8.9"
            drawTextWithShadow(versionText, 2f, client.height - 10f, 2f, -1)
            drawTextWithShadow(copyRightText, client.width - textRenderer.getStringWidth(copyRightText) - 2f, client.height - 10f, 2f, -1)
        }
    }
}