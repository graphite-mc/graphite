package com.graphite.renderer.pathway.core

import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.renderer.geometry.Geometry
import com.graphite.renderer.geometry.impl.SimpleGeometry
import com.graphite.renderer.shader.CoreShaders
import com.graphite.renderer.shader.GraphiteShader
import com.graphite.renderer.utility.createBindGroup
import com.graphite.renderer.utility.withCommandEncoder
import io.ygdrasil.webgpu.*

object BlitRenderer {
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

    private lateinit var bindGroupLayout: GPUBindGroupLayout
    private lateinit var renderPipeline: GPURenderPipeline

    private lateinit var textureSampler: GPUSampler

    fun init() = withWGPU {
        shader = CoreShaders.FULLSCREEN_BLIT
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
                )
            )
        )

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
    }

    fun render(view: GPUTextureView) = withWGPU {
        val bindGroup = device.createBindGroup(
            bindGroupLayout,
            0u to textureSampler,
            1u to view,
        )

        val surfaceTexture = surface.getCurrentTexture()
        val view = surfaceTexture.texture.createView()

        device.withCommandEncoder { commandEncoder ->
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
    }
}