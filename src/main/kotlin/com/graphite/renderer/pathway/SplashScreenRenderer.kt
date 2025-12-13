package com.graphite.renderer.pathway

import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.renderer.shader.ShaderLoader
import com.graphite.renderer.texture.mc.MCTexture
import com.graphite.utility.shortBufferOf
import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutEntry
import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.BufferBindingLayout
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBufferBindingType
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPUFilterMode
import io.ygdrasil.webgpu.GPUFrontFace
import io.ygdrasil.webgpu.GPUIndexFormat
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPUSamplerBindingType
import io.ygdrasil.webgpu.GPUShaderStage
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureSampleType
import io.ygdrasil.webgpu.GPUTextureViewDimension
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.GPUVertexStepMode
import io.ygdrasil.webgpu.PipelineLayoutDescriptor
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.SamplerBindingLayout
import io.ygdrasil.webgpu.SamplerDescriptor
import io.ygdrasil.webgpu.TextureBindingLayout
import io.ygdrasil.webgpu.VertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.arrayBufferOf
import io.ygdrasil.webgpu.writeInto
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureManager
import net.minecraft.util.Identifier
import javax.imageio.ImageIO

object SplashScreenRenderer {
    private val vertices = doubleArrayOf(
        -1.0, -1.0, 0.0, 1.0,
        1.0, -1.0, 1.0, 1.0,
        -1.0,  1.0, 0.0, 0.0,
        1.0,  1.0, 1.0, 0.0,
    ).map { it.toFloat() }.toFloatArray()
    private val indices = shortArrayOf(
        0, 1, 2,
        2, 1, 3
    )

    private val MOJANG_LOGO_TEXTURE = Identifier("textures/gui/title/mojang.png")

    fun drawSplashScreen(client: MinecraftClient, textureManager: TextureManager) = withWGPU {
        val inputStream = client.defaultResourcePack.open(MOJANG_LOGO_TEXTURE)
        val textureId = textureManager.registerDynamicTexture("logo", NativeImageBackedTexture(ImageIO.read(inputStream)))
        val texture = (textureManager.getTexture(textureId) as MCTexture).graphiteTexture
        val textureView = texture.createView()

        val shaderModule = ShaderLoader.loadShader(device, "splash_blit")

        val vertexBuffer = device.createBuffer(
            BufferDescriptor(
                size = (vertices.size * Float.SIZE_BYTES).toULong(),
                usage = setOf(GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst)
            )
        )
        arrayBufferOf(vertices) {
            device.queue.writeBuffer(vertexBuffer, 0u, it)
        }

        val indexBuffer = device.createBuffer(
            BufferDescriptor(
                size = (indices.size * 2).toULong(),
                usage = setOf(GPUBufferUsage.Index, GPUBufferUsage.CopyDst)
            )
        )

        shortBufferOf(indices) {
            device.queue.writeBuffer(indexBuffer, 0u, it)
        }

        val uniformData = floatArrayOf(
            client.width.toFloat(), client.height.toFloat(),
            256.0f, 256.0f
        )

        val uniformBuffer = device.createBuffer(
            BufferDescriptor(
                size = (uniformData.size * 4).toULong(),
                usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
            )
        )

        arrayBufferOf(uniformData) {
            device.queue.writeBuffer(uniformBuffer, 0u, it)
        }

        val bindGroupLayout = device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                entries = listOf(
                    BindGroupLayoutEntry(
                        binding = 0u,
                        visibility = setOf(GPUShaderStage.Fragment),
                        sampler = SamplerBindingLayout(
                            type = GPUSamplerBindingType.Filtering
                        )
                    ),
                    BindGroupLayoutEntry(
                        binding = 1u,
                        visibility = setOf(GPUShaderStage.Fragment),
                        texture = TextureBindingLayout(
                            sampleType = GPUTextureSampleType.Float,
                            viewDimension = GPUTextureViewDimension.TwoD
                        )
                    ),
                    BindGroupLayoutEntry(
                        binding = 2u,
                        visibility = setOf(GPUShaderStage.Vertex, GPUShaderStage.Fragment),
                        buffer = BufferBindingLayout(
                            type = GPUBufferBindingType.Uniform
                        )
                    )
                )
            )
        )

        val sampler = device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest
            )
        )

        val pipelineLayout = device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayouts = listOf(bindGroupLayout))
        )

        val renderPipeline = device.createRenderPipeline(
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex = VertexState(
                    module = shaderModule,
                    entryPoint = "vs_main",
                    buffers = listOf(
                        VertexBufferLayout(
                            arrayStride = 16u,
                            stepMode = GPUVertexStepMode.Vertex,
                            attributes = listOf(
                                VertexAttribute(
                                    format = GPUVertexFormat.Float32x2,
                                    offset = 0u,
                                    shaderLocation = 0u
                                ),
                                VertexAttribute(
                                    format = GPUVertexFormat.Float32x2,
                                    offset = 8u,
                                    shaderLocation = 1u
                                )
                            )
                        )
                    )
                ),
                fragment = FragmentState(
                    module = shaderModule,
                    entryPoint = "fs_main",
                    targets = listOf(
                        ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)
                    )
                ),
                primitive = PrimitiveState(
                    topology = GPUPrimitiveTopology.TriangleList,
                    frontFace = GPUFrontFace.CCW,
                    cullMode = GPUCullMode.None
                )
            )
        )

        val bindGroup = device.createBindGroup(
            BindGroupDescriptor(
                layout = bindGroupLayout,
                entries = listOf(
                    BindGroupEntry(binding = 0u, resource = sampler),
                    BindGroupEntry(binding = 1u, resource = textureView),
                    BindGroupEntry(
                        binding = 2u,
                        resource = BufferBinding(
                            buffer = uniformBuffer,
                            size = (uniformData.size * 4).toULong()
                        )
                    )
                )
            )
        )

        val commandEncoder = device.createCommandEncoder()
        val surfaceTexture = surface.getCurrentTexture()
        val view = surfaceTexture.texture.createView()

        val renderPass = commandEncoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = view,
                        loadOp = GPULoadOp.Clear,
                        storeOp = GPUStoreOp.Store,
                        clearValue = Color(1.0, 1.0, 1.0, 1.0)
                    )
                )
            )
        )

        renderPass.setPipeline(renderPipeline)
        renderPass.setVertexBuffer(0u, vertexBuffer)
        renderPass.setIndexBuffer(indexBuffer, GPUIndexFormat.Uint16)
        renderPass.setBindGroup(0u, bindGroup)
        renderPass.drawIndexed(6u, 1u, 0u, 0, 0u)
        renderPass.end()

        device.queue.submit(listOf(commandEncoder.finish()))
        surface.present()
    }
}