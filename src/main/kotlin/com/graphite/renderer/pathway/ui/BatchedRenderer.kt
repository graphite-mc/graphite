package com.graphite.renderer.pathway.ui

import com.graphite.renderer.shader.CoreShaders
import io.ygdrasil.webgpu.GPUTextureView
import net.minecraft.client.MinecraftClient
import java.awt.Color

object BatchedRenderer {
    private val quadBatcher = QuadBatchRenderer()
    private val fontBatcher = FontBatchRenderer(quadBatcher)

    private val client get() = MinecraftClient.getInstance()

    val fontHeight get() = fontBatcher.fontHeight

    fun initialize() {
        quadBatcher.init(CoreShaders.UI_QUADS)
        fontBatcher.init(client)
    }

    fun begin() {
        quadBatcher.begin(client.width.toFloat(), client.height.toFloat())
        fontBatcher.setupFrame()
    }

    fun bindFrom(view: GPUTextureView) = quadBatcher.bindTexture(view)

    fun drawQuad(
        x: Float, y: Float,
        width: Float, height: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        quadBatcher.drawQuad(x, y, width, height, r, g, b, a)
    }

    fun drawQuad(
        x: Float, y: Float,
        width: Float, height: Float,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        r: Float, g: Float, b: Float, a: Float,
        textureId: Float
    ) {
        quadBatcher.drawTexturedQuad(x, y, width, height, u0, v0, u1, v1, r, g, b, a, textureId)
    }

    fun drawQuad(
        x: Float, y: Float,
        width: Float, height: Float,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        tint: Color,
        textureId: Float
    ) {
        drawQuad(x, y, width, height, u0, v0, u1, v1, tint.red / 255f, tint.blue / 255f, tint.green / 255f, tint.alpha / 255f, textureId)
    }

    fun drawQuad(
        x: Float, y: Float,
        width: Float, height: Float,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        tint: Int,
        textureId: Float
    ) {
        val tint = Color(tint)
        drawQuad(x, y, width, height, u0, v0, u1, v1, tint, textureId)
    }

    fun drawQuad(x: Float, y: Float, width: Float, height: Float, color: Color) {
        drawQuad(x, y, width, height, color.red / 255f, color.blue / 255f, color.green / 255f, color.alpha / 255f)
    }

    fun drawQuad(x: Float, y: Float, width: Float, height: Float, color: Int) {
        val color = Color(color)
        drawQuad(x, y, width, height, color)
    }

    fun drawText(text: String, x: Float, y: Float, scale: Float, color: Int) {
        fontBatcher.draw(text, x, y, scale, color)
    }

    fun drawTextWithShadow(text: String, x: Float, y: Float, scale: Float, color: Int) {
        fontBatcher.drawWithShadow(text, x, y, scale, color)
    }

    fun getTextWidth(text: String) = fontBatcher.getTextWidth(text)

    fun flush() {
        quadBatcher.flush()
    }

    fun withBatch(batch: BatchedRenderer.() -> Unit) {
        begin()
        batch()
        flush()
    }
}