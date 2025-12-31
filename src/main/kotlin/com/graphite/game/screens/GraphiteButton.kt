package com.graphite.game.screens

import com.graphite.renderer.pathway.ui.BatchedRenderer
import com.graphite.renderer.utility.createTexture
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.widget.ButtonWidget

class GraphiteButton(
    id: Int, x: Int, y: Int, w: Int, h: Int, text: String
) : ButtonWidget(id, x, y, w, h, text) {
    constructor(id: Int, x: Int, y: Int, text: String) : this(id, x, y, 200, 20, text)

    companion object {
        val texture by lazy {
            MinecraftClient.getInstance().getTextureManager().createTexture(WIDGETS_LOCATION)
        }
    }

    override fun render(minecraftClient: MinecraftClient?, i: Int, j: Int) {
        BatchedRenderer.let { br ->
            if (visible) {
                val binding = br.bindFrom(texture)

                hovered = i >= this.x && j >= this.y && i < this.x + this.width * 2f && j < this.y + this.height * 2f
                val k: Int = this.getYImage(this.hovered)

                val texWidth = 256f
                val texHeight = 256f

                val u0Left = 0f / texWidth
                val v0 = (46f + k * 20f) / texHeight
                val u1Left = (this.width / 2f) / texWidth
                val v1 = (46f + k * 20f + this.height) / texHeight

                br.drawQuad(
                    this.x.toFloat(), this.y.toFloat(),
                    (this.width / 2f) * 2f, this.height.toFloat() * 2f,
                    u0Left, v0, u1Left, v1,
                    1f, 1f, 1f, 1f,
                    binding // texture slot
                )

                val u0Right = (200f - this.width / 2f) / texWidth
                val u1Right = 200f / texWidth

                br.drawQuad(
                    (this.x + (this.width / 2) * 2f), this.y.toFloat(),
                    (this.width / 2f) * 2f, this.height * 2f,
                    u0Right, v0, u1Right, v1,
                    1f, 1f, 1f, 1f,
                    binding // texture slot
                )

                this.mouseDragged(minecraftClient, i, j)

                var l = 14737632
                if (!this.active) {
                    l = 10526880
                } else if (this.hovered) {
                    l = 16777120
                }

                br.drawTextWithShadow(
                    this.message,
                    (this.x + this.width - (br.getTextWidth(this.message) * 2f) / 2),
                    (this.y + (this.height * 2f - (8 * 2f)) / 2),
                    2f,
                    l
                )
            }
        }
    }

    override fun isMouseOver(minecraftClient: MinecraftClient?, i: Int, j: Int): Boolean {
        return this.active && this.visible && i >= this.x && j >= this.y && i < this.x + this.width * 2f && j < this.y + this.height * 2f
    }
}