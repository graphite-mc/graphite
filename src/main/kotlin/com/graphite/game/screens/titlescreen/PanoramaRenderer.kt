package com.graphite.game.screens.titlescreen

import com.graphite.platform.graphics.wgpu.withWGPU
import com.graphite.renderer.shader.CoreShaders
import com.graphite.renderer.targets.RenderTarget
import com.graphite.renderer.utility.binding
import com.graphite.renderer.utility.createBindGroup
import com.graphite.renderer.utility.withCommandEncoder
import io.ygdrasil.webgpu.*
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.joml.Matrix4f
import javax.imageio.ImageIO

object PanoramaRenderer {
    private val PANORAMA_FACES = arrayOf(
        Identifier("textures/gui/title/background/panorama_0.png"),
        Identifier("textures/gui/title/background/panorama_1.png"),
        Identifier("textures/gui/title/background/panorama_2.png"),
        Identifier("textures/gui/title/background/panorama_3.png"),
        Identifier("textures/gui/title/background/panorama_4.png"),
        Identifier("textures/gui/title/background/panorama_5.png")
    )

    private val cubeVertices = floatArrayOf(
        -1f, -1f,  1f,  0f, 1f,
        1f, -1f,  1f,  1f, 1f,
        1f,  1f,  1f,  1f, 0f,
        -1f,  1f,  1f,  0f, 0f,

        -1f, -1f, -1f,  1f, 1f,
        -1f,  1f, -1f,  1f, 0f,
        1f,  1f, -1f,  0f, 0f,
        1f, -1f, -1f,  0f, 1f,

        -1f,  1f, -1f,  0f, 1f,
        -1f,  1f,  1f,  0f, 0f,
        1f,  1f,  1f,  1f, 0f,
        1f,  1f, -1f,  1f, 1f,

        -1f, -1f, -1f,  0f, 0f,
        1f, -1f, -1f,  1f, 0f,
        1f, -1f,  1f,  1f, 1f,
        -1f, -1f,  1f,  0f, 1f,

        1f, -1f, -1f,  0f, 1f,
        1f,  1f, -1f,  0f, 0f,
        1f,  1f,  1f,  1f, 0f,
        1f, -1f,  1f,  1f, 1f,

        -1f, -1f, -1f,  1f, 1f,
        -1f, -1f,  1f,  0f, 1f,
        -1f,  1f,  1f,  0f, 0f,
        -1f,  1f, -1f,  1f, 0f
    )

    private val cubeIndices = shortArrayOf(
        0, 1, 2, 2, 3, 0,
        4, 5, 6, 6, 7, 4,
        8, 9, 10, 10, 11, 8,
        12, 13, 14, 14, 15, 12,
        16, 17, 18, 18, 19, 16,
        20, 21, 22, 22, 23, 20
    )

    private lateinit var cubeVertexBuffer: GPUBuffer
    private lateinit var cubeIndexBuffer: GPUBuffer
    private lateinit var uniformBuffer: GPUBuffer

    private lateinit var panoramaTextures: Array<GPUTextureView>
    private lateinit var sampler: GPUSampler

    private lateinit var skyboxPipeline: GPURenderPipeline
    private lateinit var blurPipeline: GPURenderPipeline

    private lateinit var skyboxBindGroupLayout: GPUBindGroupLayout
    private lateinit var blurBindGroupLayout: GPUBindGroupLayout

    private lateinit var skyboxBindGroups: Array<GPUBindGroup>

    private lateinit var blurHorizBuffer: GPUBuffer
    private lateinit var blurVertBuffer: GPUBuffer
    private lateinit var blurBindGroupHoriz: GPUBindGroup
    private lateinit var blurBindGroupVert: GPUBindGroup
    private lateinit var blurBindGroupHoriz2: GPUBindGroup
    private lateinit var blurBindGroupVert2: GPUBindGroup
    private lateinit var blitBindGroup: GPUBindGroup

    private lateinit var renderTarget: RenderTarget
    private lateinit var blurTarget1: RenderTarget
    private lateinit var blurTarget2: RenderTarget

    private val projection = Matrix4f()
    private val view = Matrix4f()
    private val vp = Matrix4f()
    private val vpArray = FloatArray(16)

    private var rotation = 0f

    private const val RT_SIZE = 512

    fun init(client: MinecraftClient) = withWGPU {
        cubeVertexBuffer = device.createBuffer(
            BufferDescriptor(
                size = (cubeVertices.size * 4).toULong(),
                usage = setOf(GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst)
            )
        )
        device.queue.writeBuffer(cubeVertexBuffer, 0u, cubeVertices)

        cubeIndexBuffer = device.createBuffer(
            BufferDescriptor(
                size = (cubeIndices.size * 2).toULong(),
                usage = setOf(GPUBufferUsage.Index, GPUBufferUsage.CopyDst)
            )
        )
        device.queue.writeBuffer(cubeIndexBuffer, 0u, cubeIndices)

        uniformBuffer = device.createBuffer(
            BufferDescriptor(
                size = 64u,
                usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
            )
        )

        panoramaTextures = PANORAMA_FACES.map { loadPanoramaTexture(client, it) }.toTypedArray()

        sampler = device.createSampler(
            SamplerDescriptor(
                magFilter = GPUFilterMode.Linear,
                minFilter = GPUFilterMode.Linear,
                addressModeU = GPUAddressMode.ClampToEdge,
                addressModeV = GPUAddressMode.ClampToEdge
            )
        )

        skyboxBindGroupLayout = device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                entries = listOf(
                    BindGroupLayoutEntry(
                        binding = 0u,
                        visibility = setOf(GPUShaderStage.Vertex),
                        buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform)
                    ),
                    BindGroupLayoutEntry(
                        binding = 1u,
                        visibility = setOf(GPUShaderStage.Fragment),
                        sampler = SamplerBindingLayout(type = GPUSamplerBindingType.Filtering)
                    ),
                    BindGroupLayoutEntry(
                        binding = 2u,
                        visibility = setOf(GPUShaderStage.Fragment),
                        texture = TextureBindingLayout(
                            sampleType = GPUTextureSampleType.Float,
                            viewDimension = GPUTextureViewDimension.TwoD
                        )
                    )
                )
            )
        )

        blurBindGroupLayout = device.createBindGroupLayout(
            BindGroupLayoutDescriptor(
                entries = listOf(
                    BindGroupLayoutEntry(
                        binding = 0u,
                        visibility = setOf(GPUShaderStage.Fragment),
                        sampler = SamplerBindingLayout(type = GPUSamplerBindingType.Filtering)
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
                        visibility = setOf(GPUShaderStage.Fragment),
                        buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform)
                    )
                )
            )
        )

        createPipelines()

        renderTarget = RenderTarget(RT_SIZE, RT_SIZE, hasDepth = false)
        renderTarget.init()

        blurTarget1 = RenderTarget(RT_SIZE, RT_SIZE, hasDepth = false)
        blurTarget1.init()

        blurTarget2 = RenderTarget(RT_SIZE, RT_SIZE, hasDepth = false)
        blurTarget2.init()

        createBindGroups()
    }

    private fun createBindGroups() = withWGPU {
        val textureFaceMap = intArrayOf(3, 2, 4, 5, 1, 0)
        skyboxBindGroups = Array(6) { cubeFace ->
            device.createBindGroup(
                skyboxBindGroupLayout,
                0u to uniformBuffer.binding(64u),
                1u to sampler,
                2u to panoramaTextures[textureFaceMap[cubeFace]]
            )
        }

        val blurScale = 4.0f / RT_SIZE
        blurHorizBuffer = device.createBuffer(
            BufferDescriptor(
                size = 16u,
                usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
            )
        )
        device.queue.writeBuffer(blurHorizBuffer, 0u, floatArrayOf(blurScale, 0f, 0f, 0f))

        blurVertBuffer = device.createBuffer(
            BufferDescriptor(
                size = 16u,
                usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
            )
        )
        device.queue.writeBuffer(blurVertBuffer, 0u, floatArrayOf(0f, blurScale, 0f, 0f))

        blurBindGroupHoriz = device.createBindGroup(
            blurBindGroupLayout,
            0u to sampler,
            1u to renderTarget.colorView,
            2u to blurHorizBuffer.binding(16u)
        )

        blurBindGroupVert = device.createBindGroup(
            blurBindGroupLayout,
            0u to sampler,
            1u to blurTarget1.colorView,
            2u to blurVertBuffer.binding(16u)
        )

        blurBindGroupHoriz2 = device.createBindGroup(
            blurBindGroupLayout,
            0u to sampler,
            1u to blurTarget2.colorView,
            2u to blurHorizBuffer.binding(16u)
        )

        blurBindGroupVert2 = device.createBindGroup(
            blurBindGroupLayout,
            0u to sampler,
            1u to blurTarget1.colorView,
            2u to blurVertBuffer.binding(16u)
        )

        val blitBuffer = device.createBuffer(
            BufferDescriptor(
                size = 16u,
                usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)
            )
        )
        device.queue.writeBuffer(blitBuffer, 0u, floatArrayOf(0f, 0f, 0f, 0f))

        blitBindGroup = device.createBindGroup(
            blurBindGroupLayout,
            0u to sampler,
            1u to blurTarget2.colorView,
            2u to blitBuffer.binding(16u)
        )
    }

    private fun loadPanoramaTexture(client: MinecraftClient, id: Identifier): GPUTextureView = withWGPU {
        val image = ImageIO.read(client.defaultResourcePack.open(id))
        val width = image.width
        val height = image.height

        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)

        val rgbaData = ByteArray(width * height * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            rgbaData[i * 4 + 0] = (pixel shr 16 and 0xFF).toByte()
            rgbaData[i * 4 + 1] = (pixel shr 8 and 0xFF).toByte()
            rgbaData[i * 4 + 2] = (pixel and 0xFF).toByte()
            rgbaData[i * 4 + 3] = (pixel ushr 24 and 0xFF).toByte()
        }

        val texture = device.createTexture(
            TextureDescriptor(
                size = Extent3D(width.toUInt(), height.toUInt(), 1u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = setOf(GPUTextureUsage.TextureBinding, GPUTextureUsage.CopyDst)
            )
        )

        device.queue.writeTexture(
            destination = TexelCopyTextureInfo(
                texture = texture,
                mipLevel = 0u,
                origin = Origin3D(0u, 0u, 0u)
            ),
            data = rgbaData,
            dataLayout = TexelCopyBufferLayout(
                offset = 0u,
                bytesPerRow = (width * 4).toUInt(),
                rowsPerImage = height.toUInt()
            ),
            size = Extent3D(width.toUInt(), height.toUInt(), 1u)
        )

        texture.createView()
    }

    private fun createPipelines() = withWGPU {
        val skyboxShader = CoreShaders.PANORAMA_SKYBOX
        val blurShader = CoreShaders.PANORAMA_BLUR

        skyboxPipeline = device.createRenderPipeline(
            RenderPipelineDescriptor(
                layout = device.createPipelineLayout(
                    PipelineLayoutDescriptor(bindGroupLayouts = listOf(skyboxBindGroupLayout))
                ),
                vertex = VertexState(
                    module = skyboxShader.module,
                    entryPoint = "vs_main",
                    buffers = listOf(
                        VertexBufferLayout(
                            arrayStride = 20u,
                            stepMode = GPUVertexStepMode.Vertex,
                            attributes = listOf(
                                VertexAttribute(
                                    format = GPUVertexFormat.Float32x3,
                                    offset = 0u,
                                    shaderLocation = 0u
                                ),
                                VertexAttribute(
                                    format = GPUVertexFormat.Float32x2,
                                    offset = 12u,
                                    shaderLocation = 1u
                                )
                            )
                        )
                    )
                ),
                fragment = FragmentState(
                    module = skyboxShader.module,
                    entryPoint = "fs_main",
                    targets = listOf(
                        ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)
                    )
                ),
                primitive = PrimitiveState(
                    topology = GPUPrimitiveTopology.TriangleList,
                    cullMode = GPUCullMode.Front
                ),
                depthStencil = null
            )
        )

        blurPipeline = device.createRenderPipeline(
            RenderPipelineDescriptor(
                layout = device.createPipelineLayout(
                    PipelineLayoutDescriptor(bindGroupLayouts = listOf(blurBindGroupLayout))
                ),
                vertex = VertexState(
                    module = blurShader.module,
                    entryPoint = "vs_main"
                ),
                fragment = FragmentState(
                    module = blurShader.module,
                    entryPoint = "fs_main",
                    targets = listOf(
                        ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)
                    )
                ),
                primitive = PrimitiveState(
                    topology = GPUPrimitiveTopology.TriangleList,
                    cullMode = GPUCullMode.None
                )
            )
        )
    }

    fun render(targetView: GPUTextureView, partialTicks: Float) = withWGPU {
        rotation += partialTicks * 0.8f

        device.withCommandEncoder { commandEncoder ->
            renderSkybox(commandEncoder)
            applyGaussianBlur(commandEncoder)
            blitToScreen(commandEncoder, targetView)
        }
    }

    private fun renderSkybox(commandEncoder: GPUCommandEncoder) = withWGPU {
        projection.identity().perspective(
            Math.toRadians(120.0).toFloat(),
            1f,
            0.05f,
            10f
        )

        val rotY = -rotation * 0.1f

        view.identity()
            .rotateY(Math.toRadians(rotY.toDouble()).toFloat())

        vp.set(projection).mul(view)
        vp.get(vpArray)

        device.queue.writeBuffer(uniformBuffer, 0u, vpArray)

        val renderPass = renderTarget.beginRenderPass(
            commandEncoder,
            enableDepth = false,
            clearColor = Color(0.0, 0.0, 0.0, 1.0),
            loadOp = GPULoadOp.Clear
        )

        renderPass.setPipeline(skyboxPipeline)
        renderPass.setVertexBuffer(0u, cubeVertexBuffer)
        renderPass.setIndexBuffer(cubeIndexBuffer, GPUIndexFormat.Uint16)

        for (cubeFace in 0 until 6) {
            renderPass.setBindGroup(0u, skyboxBindGroups[cubeFace])
            renderPass.drawIndexed(6u, 1u, (cubeFace * 6).toUInt(), 0, 0u)
        }

        renderPass.end()
    }

    private fun applyGaussianBlur(commandEncoder: GPUCommandEncoder) = withWGPU {
        commandEncoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = blurTarget1.colorView,
                        loadOp = GPULoadOp.Clear,
                        storeOp = GPUStoreOp.Store,
                        clearValue = Color(0.0, 0.0, 0.0, 1.0)
                    )
                )
            )
        ).apply {
            setPipeline(blurPipeline)
            setBindGroup(0u, blurBindGroupHoriz)
            draw(3u, 1u, 0u, 0u)
            end()
        }

        commandEncoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = blurTarget2.colorView,
                        loadOp = GPULoadOp.Clear,
                        storeOp = GPUStoreOp.Store,
                        clearValue = Color(0.0, 0.0, 0.0, 1.0)
                    )
                )
            )
        ).apply {
            setPipeline(blurPipeline)
            setBindGroup(0u, blurBindGroupVert)
            draw(3u, 1u, 0u, 0u)
            end()
        }
    }

    private fun blitToScreen(commandEncoder: GPUCommandEncoder, targetView: GPUTextureView) = withWGPU {
        commandEncoder.beginRenderPass(
            RenderPassDescriptor(
                colorAttachments = listOf(
                    RenderPassColorAttachment(
                        view = targetView,
                        loadOp = GPULoadOp.Load,
                        storeOp = GPUStoreOp.Store
                    )
                )
            )
        ).apply {
            setPipeline(blurPipeline)
            setBindGroup(0u, blitBindGroup)
            draw(3u, 1u, 0u, 0u)
            end()
        }
    }
}