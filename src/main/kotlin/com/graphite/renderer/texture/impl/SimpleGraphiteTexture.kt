package com.graphite.renderer.texture.impl

import com.graphite.platform.graphics.wgpu.WGPUContext
import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.renderer.texture.GraphiteTexture
import com.graphite.utility.arrayBufferOf
import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.GPUTextureAspect
import io.ygdrasil.webgpu.GPUTextureDimension
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.Origin3D
import io.ygdrasil.webgpu.TexelCopyBufferLayout
import io.ygdrasil.webgpu.TexelCopyTextureInfo
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.writeInto
import net.minecraft.util.Identifier

class SimpleGraphiteTexture(
    identifier: Identifier,
    private val width: Int,
    private val height: Int,
    private val pixels: IntArray,
    protected val mipLevels: Int,
) : GraphiteTexture(identifier) {
    constructor(
        identifier: Identifier,
        width: Int,
        height: Int,
        pixels: IntArray,
    ) : this(identifier, width, height, pixels, 1)

    override fun createTextureDescriptor(context: WGPUContext): TextureDescriptor {
        return TextureDescriptor(
            label = identifier.path,
            size = Extent3D(
                width.toUInt(), height.toUInt()
            ),
            dimension = GPUTextureDimension.TwoD,
            format = GPUTextureFormat.RGBA8Unorm,
            mipLevelCount = mipLevels.toUInt(),
            usage = setOf(GPUTextureUsage.TextureBinding, GPUTextureUsage.CopyDst, GPUTextureUsage.RenderAttachment)
        )
    }

    override fun upload() = withWGPU {
        val byteBuffer = ByteArray(width * height * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            byteBuffer[i * 4 + 0] = (pixel shr 16 and 0xFF).toByte()
            byteBuffer[i * 4 + 1] = (pixel shr 8 and 0xFF).toByte()
            byteBuffer[i * 4 + 2] = (pixel and 0xFF).toByte()
            byteBuffer[i * 4 + 3] = (pixel shr 24 and 0xFF).toByte()
        }

        arrayBufferOf(byteBuffer) {
            byteBuffer.writeInto(it)

            device.queue.writeTexture(
                destination = TexelCopyTextureInfo(texture, 0u, Origin3D(0u, 0u), GPUTextureAspect.All),
                data = it,
                dataLayout = TexelCopyBufferLayout(
                    offset = 0u,
                    bytesPerRow = width.toUInt() * 4u,
                    rowsPerImage = height.toUInt()
                ),
                size = Extent3D(width.toUInt(), height.toUInt())
            )
        }
    }

    override fun uploadMipChunked(
        level: Int,
        mipWidth: Int,
        mipHeight: Int,
        xOffset: Int,
        yOffset: Int,
        pixels: IntArray
    ) = withWGPU {
        require(level >= 0) {
            "Mip level must be non-negative, got: $level"
        }
        require(level.toUInt() < mipLevels.toUInt()) {
            "Invalid mip level $level (texture has $mipLevels mip levels, valid range is 0-${mipLevels.toUInt() - 1u})"
        }

        val byteBuffer = ByteArray(pixels.size * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            byteBuffer[i * 4 + 0] = (pixel shr 16 and 0xFF).toByte()
            byteBuffer[i * 4 + 1] = (pixel shr 8 and 0xFF).toByte()
            byteBuffer[i * 4 + 2] = (pixel and 0xFF).toByte()
            byteBuffer[i * 4 + 3] = (pixel shr 24 and 0xFF).toByte()
        }

        arrayBufferOf(byteBuffer) {
            byteBuffer.writeInto(it)

            device.queue.writeTexture(
                destination = TexelCopyTextureInfo(
                    texture = texture,
                    mipLevel = level.toUInt(),
                    origin = Origin3D(xOffset.toUInt(), yOffset.toUInt(), 0u),
                    aspect = GPUTextureAspect.All
                ),
                data = it,
                dataLayout = TexelCopyBufferLayout(
                    offset = 0u,
                    bytesPerRow = mipWidth.toUInt() * 4u,
                    rowsPerImage = mipHeight.toUInt()
                ),
                size = Extent3D(mipWidth.toUInt(), mipHeight.toUInt(), 1u)
            )
        }
    }
}
