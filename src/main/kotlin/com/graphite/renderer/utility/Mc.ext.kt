package com.graphite.renderer.utility

import com.graphite.renderer.texture.mc.MCTexture
import io.ygdrasil.webgpu.GPUTextureView
import net.minecraft.client.texture.Texture
import net.minecraft.client.texture.TextureManager
import net.minecraft.util.Identifier

fun TextureManager.getTextureView(id: Identifier): GPUTextureView {
    return (this.getTexture(id) as MCTexture).graphiteTexture.createView()
}

fun Texture.createView() = (this as MCTexture).graphiteTexture.createView()