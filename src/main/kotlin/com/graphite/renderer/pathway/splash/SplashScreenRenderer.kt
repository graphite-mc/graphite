package com.graphite.renderer.pathway.splash

import com.graphite.math.vec2.Vec2
import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.renderer.geometry.Geometry
import com.graphite.renderer.geometry.impl.SimpleGeometry
import com.graphite.renderer.shader.CoreShaders
import com.graphite.renderer.shader.GraphiteShader
import com.graphite.renderer.shader.uniform.UniformData
import com.graphite.renderer.texture.mc.MCTexture
import com.graphite.renderer.utility.binding
import com.graphite.renderer.utility.createBindGroup
import com.graphite.renderer.utility.createBufferFromArray
import com.graphite.renderer.utility.withCommandEncoder
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutEntry
import io.ygdrasil.webgpu.BufferBindingLayout
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBindGroupLayout
import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.GPUBufferBindingType
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPUFilterMode
import io.ygdrasil.webgpu.GPUFrontFace
import io.ygdrasil.webgpu.GPUIndexFormat
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPURenderPipeline
import io.ygdrasil.webgpu.GPUSampler
import io.ygdrasil.webgpu.GPUSamplerBindingType
import io.ygdrasil.webgpu.GPUShaderStage
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureSampleType
import io.ygdrasil.webgpu.GPUTextureView
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
import net.minecraft.client.MinecraftClient
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.texture.TextureManager
import net.minecraft.util.Identifier
import javax.imageio.ImageIO

object SplashScreenRenderer {
    private val MOJANG_LOGO_TEXTURE = Identifier("textures/gui/title/mojang.png")

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

    private lateinit var geometry: Geometry
    private lateinit var shader: GraphiteShader

    private lateinit var ubo: GPUBuffer

    private lateinit var bindGroupLayout: GPUBindGroupLayout
    private lateinit var renderPipeline: GPURenderPipeline

    private lateinit var textureSampler: GPUSampler
    private lateinit var textureView: GPUTextureView

    fun init(client: MinecraftClient, textureManager: TextureManager) = withWGPU {
        shader = CoreShaders.SPLASH_BLIT
        geometry = SimpleGeometry(
            device = device,
            vertices = vertices,
            indices = indices
        )

        bindGroupLayout = device.createBindGroupLayout(
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

        val uniformData = floatArrayOf(
            1f, 1f,
            256.0f, 256.0f
        )

        ubo = device.createBufferFromArray(uniformData, GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)

        textureSampler = device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest
            )
        )

        val pipelineLayout = device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayouts = listOf(bindGroupLayout))
        )

        renderPipeline = device.createRenderPipeline(
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex = VertexState(
                    module = shader.module,
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
                    module = shader.module,
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

        val inputStream = client.defaultResourcePack.open(MOJANG_LOGO_TEXTURE)
        val textureId =
            textureManager.registerDynamicTexture("logo", NativeImageBackedTexture(ImageIO.read(inputStream)))
        val texture = (textureManager.getTexture(textureId) as MCTexture).graphiteTexture
        textureView = texture.createView()
    }

    fun render(width: Float, height: Float) = withWGPU {
        val size = UniformData(
            Vec2(width, height),
            Vec2(256f, 256f),
        ).writeInto(device, ubo)

        val bindGroup = device.createBindGroup(
            bindGroupLayout,
            0u to textureSampler,
            1u to textureView,
            2u to ubo.binding(size)
        )

        device.withCommandEncoder { commandEncoder ->
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
            renderPass.setVertexBuffer(0u, geometry.vertexBuffer)
            renderPass.setIndexBuffer(geometry.indexBuffer, GPUIndexFormat.Uint16)
            renderPass.setBindGroup(0u, bindGroup)
            renderPass.drawIndexed(6u, 1u, 0u, 0, 0u)
            renderPass.end()
        }

        surface.present()
    }
}