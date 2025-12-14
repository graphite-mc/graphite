package com.graphite.renderer.pathway.ui

import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.renderer.RenderSystem
import com.graphite.renderer.shader.GraphiteShader
import com.graphite.renderer.utility.binding
import com.graphite.renderer.utility.createBindGroup
import com.graphite.renderer.utility.createBufferFromArray
import com.graphite.renderer.utility.withCommandEncoder
import io.ygdrasil.webgpu.*

class QuadBatchRenderer(
    maxQuads: Int = 10000 // should be way more than enough
) {
    private val maxVertices = maxQuads * 4
    private val maxIndices = maxQuads * 6

    // [x, y, u, v, r, g, b, a, texId]
    private val vertexStride = 9 * 4
    private val vertexData = FloatArray(maxVertices * 9)
    private var vertexCount = 0

    private lateinit var vertexBuffer: GPUBuffer
    private lateinit var indexBuffer: GPUBuffer
    private lateinit var uniformBuffer: GPUBuffer

    private lateinit var bindGroupLayout: GPUBindGroupLayout
    private lateinit var pipeline: GPURenderPipeline

    private lateinit var whiteSampler: GPUSampler
    private lateinit var whiteTextureView: GPUTextureView

    private val textureSlots = Array<GPUTextureView?>(16) { null }
    private var currentTextureSlot = 1

    fun init(shader: GraphiteShader) = withWGPU {
        vertexBuffer = device.createBuffer(
            BufferDescriptor(
                size = (maxVertices * vertexStride).toULong(),
                usage = setOf(GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst)
            )
        )

        val indices = ShortArray(maxIndices) { i ->
            val quadIndex = i / 6
            val vertexIndex = quadIndex * 4
            when (i % 6) {
                0 -> (vertexIndex + 0).toShort()
                1 -> (vertexIndex + 1).toShort()
                2 -> (vertexIndex + 2).toShort()
                3 -> (vertexIndex + 2).toShort()
                4 -> (vertexIndex + 3).toShort()
                5 -> (vertexIndex + 1).toShort()
                else -> 0
            }
        }

        indexBuffer = device.createBufferFromArray(
            indices,
            GPUBufferUsage.Index, GPUBufferUsage.CopyDst
        )

        uniformBuffer = device.createBuffer(
            BufferDescriptor(
                size = 64u, // 4x4 matrix of floats
                usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
            )
        )

        val whiteTexture = device.createTexture(
            TextureDescriptor(
                size = Extent3D(1u, 1u, 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = setOf(GPUTextureUsage.TextureBinding, GPUTextureUsage.CopyDst)
            )
        )

        device.queue.writeTexture(
            destination = TexelCopyTextureInfo(
                texture = whiteTexture,
                mipLevel = 0u,
                origin = Origin3D(0u, 0u, 0u)
            ),
            data = byteArrayOf(-1, -1, -1, -1), // White pixel
            dataLayout = TexelCopyBufferLayout(
                offset = 0u,
                bytesPerRow = 4u,
                rowsPerImage = 1u
            ),
            size = Extent3D(1u, 1u, 1u)
        )

        whiteTextureView = whiteTexture.createView()
        textureSlots[0] = whiteTextureView

        whiteSampler = device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Nearest,
                minFilter = GPUFilterMode.Nearest
            )
        )

        bindGroupLayout = device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                entries = buildList {
                    add(
                        BindGroupLayoutEntry(
                            binding = 0u,
                            visibility = setOf(GPUShaderStage.Vertex),
                            buffer = BufferBindingLayout(
                                type = GPUBufferBindingType.Uniform
                            )
                        )
                    )

                    add(
                        BindGroupLayoutEntry(
                            binding = 1u,
                            visibility = setOf(GPUShaderStage.Fragment),
                            sampler = SamplerBindingLayout(
                                type = GPUSamplerBindingType.Filtering
                            )
                        )
                    )

                    for (i in 0 until 16) {
                        add(
                            BindGroupLayoutEntry(
                                binding = (2u + i.toUInt()),
                                visibility = setOf(GPUShaderStage.Fragment),
                                texture = TextureBindingLayout(
                                    sampleType = GPUTextureSampleType.Float,
                                    viewDimension = GPUTextureViewDimension.TwoD
                                )
                            )
                        )
                    }
                }
            )
        )

        val pipelineLayout = device.createPipelineLayout(
            PipelineLayoutDescriptor(bindGroupLayouts = listOf(bindGroupLayout))
        )

        pipeline = device.createRenderPipeline(
            RenderPipelineDescriptor(
                layout = pipelineLayout,
                vertex = VertexState(
                    module = shader.module,
                    entryPoint = "vs_main",
                    buffers = listOf(
                        VertexBufferLayout(
                            arrayStride = vertexStride.toULong(),
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
                                ),
                                VertexAttribute(
                                    format = GPUVertexFormat.Float32x4,
                                    offset = 16u,
                                    shaderLocation = 2u
                                ),
                                VertexAttribute(
                                    format = GPUVertexFormat.Float32,
                                    offset = 32u,
                                    shaderLocation = 3u
                                )
                            )
                        )
                    )
                ),
                fragment = FragmentState(
                    module = shader.module,
                    entryPoint = "fs_main",
                    targets = listOf(
                        ColorTargetState(
                            format = GPUTextureFormat.RGBA8Unorm,
                            blend = BlendState(
                                color = BlendComponent(
                                    srcFactor = GPUBlendFactor.SrcAlpha,
                                    dstFactor = GPUBlendFactor.OneMinusSrcAlpha,
                                    operation = GPUBlendOperation.Add
                                ),
                                alpha = BlendComponent(
                                    srcFactor = GPUBlendFactor.One,
                                    dstFactor = GPUBlendFactor.OneMinusSrcAlpha,
                                    operation = GPUBlendOperation.Add
                                )
                            )
                        )
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

    fun begin(width: Float, height: Float) {
        vertexCount = 0
        currentTextureSlot = 1
        for (i in 1 until textureSlots.size) {
            textureSlots[i] = null
        }

        val projection = floatArrayOf(
            2f / width, 0f, 0f, 0f,
            0f, -2f / height, 0f, 0f,
            0f, 0f, 1f, 0f,
            -1f, 1f, 0f, 1f
        )

        withWGPU {
            arrayBufferOf(projection) {
                device.queue.writeBuffer(uniformBuffer, 0u, it)
            }
        }
    }

    fun drawQuad(
        x: Float, y: Float,
        width: Float, height: Float,
        r: Float, g: Float, b: Float, a: Float
    ) {
        drawTexturedQuad(x, y, width, height, 0f, 0f, 1f, 1f, r, g, b, a, 0f)
    }

    fun drawTexturedQuad(
        x: Float, y: Float,
        width: Float, height: Float,
        u0: Float, v0: Float,
        u1: Float, v1: Float,
        r: Float, g: Float, b: Float, a: Float,
        textureId: Float
    ) {
        if (vertexCount + 4 > maxVertices) {
            flush()
        }

        val idx = vertexCount * 9

        vertexData[idx + 0] = x
        vertexData[idx + 1] = y
        vertexData[idx + 2] = u0
        vertexData[idx + 3] = v0
        vertexData[idx + 4] = r
        vertexData[idx + 5] = g
        vertexData[idx + 6] = b
        vertexData[idx + 7] = a
        vertexData[idx + 8] = textureId

        vertexData[idx + 9] = x + width
        vertexData[idx + 10] = y
        vertexData[idx + 11] = u1
        vertexData[idx + 12] = v0
        vertexData[idx + 13] = r
        vertexData[idx + 14] = g
        vertexData[idx + 15] = b
        vertexData[idx + 16] = a
        vertexData[idx + 17] = textureId

        vertexData[idx + 18] = x
        vertexData[idx + 19] = y + height
        vertexData[idx + 20] = u0
        vertexData[idx + 21] = v1
        vertexData[idx + 22] = r
        vertexData[idx + 23] = g
        vertexData[idx + 24] = b
        vertexData[idx + 25] = a
        vertexData[idx + 26] = textureId

        vertexData[idx + 27] = x + width
        vertexData[idx + 28] = y + height
        vertexData[idx + 29] = u1
        vertexData[idx + 30] = v1
        vertexData[idx + 31] = r
        vertexData[idx + 32] = g
        vertexData[idx + 33] = b
        vertexData[idx + 34] = a
        vertexData[idx + 35] = textureId

        vertexCount += 4
    }

    fun bindTexture(textureView: GPUTextureView): Float {
        for (i in 0 until currentTextureSlot) {
            if (textureSlots[i] == textureView) {
                return i.toFloat()
            }
        }

        if (currentTextureSlot >= textureSlots.size) {
            flush()
            currentTextureSlot = 1
        }

        textureSlots[currentTextureSlot] = textureView
        return (currentTextureSlot++).toFloat()
    }

    fun flush() {
        if (vertexCount == 0) return

        withWGPU {
            device.queue.writeBuffer(
                vertexBuffer,
                0u,
                vertexData.sliceArray(0 until vertexCount * 9)
            )

            val bindGroup = device.createBindGroup(
                bindGroupLayout,
                *buildList {
                    add(0u to uniformBuffer.binding(16uL * Float.SIZE_BYTES.toULong()))
                    add(1u to whiteSampler)
                    for (i in 0 until 16) {
                        add((2u + i.toUInt()) to (textureSlots[i] ?: whiteTextureView))
                    }
                }.toTypedArray()
            )

            RenderSystem.defaultRenderTarget().render(clearColor = null) { renderPass ->
                renderPass.setPipeline(pipeline)
                renderPass.setVertexBuffer(0u, vertexBuffer)
                renderPass.setIndexBuffer(indexBuffer, GPUIndexFormat.Uint16)
                renderPass.setBindGroup(0u, bindGroup)
                renderPass.drawIndexed((vertexCount / 4 * 6).toUInt(), 1u, 0u, 0, 0u)
            }
        }

        vertexCount = 0
    }
}