package com.graphite.renderer.targets

import com.graphite.platform.graphics.wgpu.withWGPU
import io.ygdrasil.webgpu.*

class RenderTarget(
    var width: Int,
    var height: Int,
    val hasDepth: Boolean = false,
    val sampleCount: Int = 1,
    val format: GPUTextureFormat = GPUTextureFormat.RGBA8Unorm
) {
    private lateinit var colorTexture: GPUTexture
    private lateinit var depthTexture: GPUTexture

    lateinit var colorView: GPUTextureView
        private set

    lateinit var depthView: GPUTextureView
        private set

    var isInitialized = false
        private set

    fun init() = withWGPU {
        if (isInitialized) {
            close()
        }

        colorTexture = device.createTexture(
            TextureDescriptor(
                size = Extent3D(width.toUInt(), height.toUInt(), 1u),
                mipLevelCount = 1u,
                sampleCount = sampleCount.toUInt(),
                dimension = GPUTextureDimension.TwoD,
                format = format,
                usage = setOf(
                    GPUTextureUsage.RenderAttachment,
                    GPUTextureUsage.TextureBinding,
                    GPUTextureUsage.CopySrc
                )
            )
        )

        colorView = colorTexture.createView()

        if (hasDepth) {
            depthTexture = device.createTexture(
                TextureDescriptor(
                    size = Extent3D(width.toUInt(), height.toUInt(), 1u),
                    mipLevelCount = 1u,
                    sampleCount = sampleCount.toUInt(),
                    dimension = GPUTextureDimension.TwoD,
                    format = GPUTextureFormat.Depth24Plus,
                    usage = setOf(
                        GPUTextureUsage.RenderAttachment
                    )
                )
            )

            depthView = depthTexture.createView()
        }

        isInitialized = true
    }

    fun resize(newWidth: Int, newHeight: Int) {
        if (width == newWidth && height == newHeight) return

        width = newWidth
        height = newHeight
        init()
    }

    fun beginRenderPass(
        commandEncoder: GPUCommandEncoder,
        enableDepth: Boolean,
        clearColor: Color? = Color(0.0, 0.0, 0.0, 1.0),
        loadOp: GPULoadOp = if (clearColor != null) GPULoadOp.Clear else GPULoadOp.Load,
        storeOp: GPUStoreOp = GPUStoreOp.Store
    ): GPURenderPassEncoder {
        if (!isInitialized) {
            throw IllegalStateException("RenderTarget must be initialized before use")
        }

        val colorAttachment = RenderPassColorAttachment(
            view = colorView,
            loadOp = loadOp,
            storeOp = storeOp,
            clearValue = clearColor
        )

        val depthAttachment = if (hasDepth && enableDepth) {
            RenderPassDepthStencilAttachment(
                view = depthView,
                depthLoadOp = GPULoadOp.Clear,
                depthStoreOp = GPUStoreOp.Store,
                depthClearValue = 1.0f
            )
        } else null

        return commandEncoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(colorAttachment),
                depthStencilAttachment = depthAttachment
            )
        )
    }

    inline fun render(
        clearColor: Color? = Color(0.0, 0.0, 0.0, 1.0),
        enableDepth: Boolean = false,
        crossinline block: (GPURenderPassEncoder) -> Unit
    ) = withWGPU {
        device.queue.submit(
            listOf(
                device.createCommandEncoder().apply {
                    val renderPass = beginRenderPass(this, enableDepth, clearColor)
                    block(renderPass)
                    renderPass.end()
                }.finish()
            )
        )
    }

    fun blitTo(
        commandEncoder: GPUCommandEncoder,
        destinationView: GPUTextureView,
        pipeline: GPURenderPipeline,
        bindGroup: GPUBindGroup
    ) {
        val renderPass = commandEncoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = destinationView,
                        loadOp = GPULoadOp.Load,
                        storeOp = GPUStoreOp.Store
                    )
                )
            )
        )

        renderPass.setPipeline(pipeline)
        renderPass.setBindGroup(0u, bindGroup)
        renderPass.draw(3u, 1u, 0u, 0u)
        renderPass.end()
    }

    fun close() {
        if (!isInitialized) return

        colorTexture.close()
        if (hasDepth) {
            depthTexture.close()
        }

        isInitialized = false
    }

    fun copyToBuffer(commandEncoder: GPUCommandEncoder, buffer: GPUBuffer) {
        commandEncoder.copyTextureToBuffer(
            source = TexelCopyTextureInfo(
                texture = colorTexture,
                mipLevel = 0u,
                origin = Origin3D(0u, 0u, 0u)
            ),
            destination = TexelCopyBufferInfo(
                buffer = buffer,
                offset = 0u,
                bytesPerRow = (width * 4).toUInt(),
                rowsPerImage = height.toUInt()
            ),
            copySize = Extent3D(width.toUInt(), height.toUInt(), 1u)
        )
    }
}

