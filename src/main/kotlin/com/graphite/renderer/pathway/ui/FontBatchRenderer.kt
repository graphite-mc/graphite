package com.graphite.renderer.pathway.ui

import com.graphite.platform.graphics.wgpu.withWGPU
import io.ygdrasil.webgpu.*
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.max

class FontBatchRenderer(
    private val quadRenderer: QuadBatchRenderer,
    private val fontTexture: Identifier = Identifier("textures/font/ascii.png"),
    private var unicode: Boolean = false
) {
    companion object {
        private val UNICODE_PAGES = Array<Identifier?>(256) { null }
        private const val FORMATTING_CODE_CHAR = '§'
        private const val COLOR_CODES = "0123456789abcdefklmnor"

        private const val CHAR_MAP =
            "ÀÁÂÈÊËÍÓÔÕÚßãõğİıŒœŞşŴŵžȇ\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000ÇüéâäàåçêëèïîìÄÅÉæÆôöòûùÿÖÜø£Ø×ƒáíóúñÑªº¿®¬½¼¡«»░▒▓│┤╡╢╖╕╣║╗╝╜╛┐└┴┬├─┼╞╟╚╔╩╦╠═╬╧╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀αβΓπΣσμτΦΘΩδ∞∅∈∩≡±≥≤⌠⌡÷≈°∙·√ⁿ²■\u0000"
    }

    private val characterWidths = IntArray(256)
    private val glyphWidths = ByteArray(65536)
    private val colorCodes = IntArray(32)

    var fontHeight = 9
    private val random = Random()

    private lateinit var fontTextureView: GPUTextureView
    private var fontTextureId = 0f

    private val unicodePageTextures = mutableMapOf<Int, GPUTextureView>()

    private var x = 0f
    private var y = 0f
    private var red = 1f
    private var green = 1f
    private var blue = 1f
    private var alpha = 1f
    private var obfuscated = false
    private var bold = false
    private var italic = false
    private var underline = false
    private var strikethrough = false
    private var scale = 0f

    fun init(client: MinecraftClient, anaglyph3d: Boolean = false) {
        for (i in 0 until 32) {
            val j = (i shr 3 and 1) * 85
            var k = (i shr 2 and 1) * 170 + j
            var l = (i shr 1 and 1) * 170 + j
            var m = (i shr 0 and 1) * 170 + j

            if (i == 6) {
                k += 85
            }

            if (anaglyph3d) {
                val n = (k * 30 + l * 59 + m * 11) / 100
                val o = (k * 30 + l * 70) / 100
                val p = (k * 30 + m * 70) / 100
                k = n
                l = o
                m = p
            }

            if (i >= 16) {
                k /= 4
                l /= 4
                m /= 4
            }

            colorCodes[i] = (k and 255 shl 16) or (l and 255 shl 8) or (m and 255)
        }

        loadFontTexture(client)

        readGlyphSizes(client)
    }

    private fun loadFontTexture(client: MinecraftClient) = withWGPU {
        val bufferedImage = ImageIO.read(
            client.resourceManager.getResource(fontTexture).inputStream
        )

        val width = bufferedImage.width
        val height = bufferedImage.height
        val pixels = IntArray(width * height)
        bufferedImage.getRGB(0, 0, width, height, pixels, 0, width)

        val charHeight = height / 16
        val charWidth = width / 16
        val scale = 8.0f / charWidth

        for (charIndex in 0 until 256) {
            val col = charIndex % 16
            val row = charIndex / 16

            if (charIndex == 32) { // Space character
                characterWidths[charIndex] = 4
                continue
            }

            var w = charWidth - 1
            while (w >= 0) {
                val x = col * charWidth + w
                var empty = true

                for (h in 0 until charHeight) {
                    val pixelY = (row * charHeight + h) * width
                    if ((pixels[x + pixelY] ushr 24 and 255) != 0) {
                        empty = false
                        break
                    }
                }

                if (!empty) break
                w--
            }

            characterWidths[charIndex] = ((w + 1) * scale + 0.5).toInt() + 1
        }

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

        fontTextureView = texture.createView()
    }

    private fun readGlyphSizes(client: MinecraftClient) {
        try {
            val stream = client.defaultResourcePack.open(Identifier("font/glyph_sizes.bin"))
            stream.read(glyphWidths)
            stream.close()
        } catch (e: Exception) {
            throw RuntimeException("Failed to load glyph sizes", e)
        }
    }

    private fun getUnicodePage(page: Int, client: MinecraftClient): GPUTextureView? {
        if (unicodePageTextures.containsKey(page)) {
            return unicodePageTextures[page]
        }

        if (UNICODE_PAGES[page] == null) {
            UNICODE_PAGES[page] = Identifier("textures/font/unicode_page_%02x.png".format(page))
        }

        return try {
            withWGPU {
                val bufferedImage = ImageIO.read(
                    client.defaultResourcePack.open(UNICODE_PAGES[page]!!)
                )

                val width = bufferedImage.width
                val height = bufferedImage.height
                val pixels = IntArray(width * height)
                bufferedImage.getRGB(0, 0, width, height, pixels, 0, width)

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

                val view = texture.createView()
                unicodePageTextures[page] = view
                view
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setupFrame() {
        fontTextureId = quadRenderer.bindTexture(fontTextureView)
    }

    fun drawWithShadow(text: String, x: Float, y: Float, scale: Float, color: Int): Int {
        return draw(text, x, y, scale, color, true)
    }

    fun draw(text: String, x: Float, y: Float, scale: Float, color: Int): Int {
        return draw(text, x, y, scale, color, false)
    }

    private fun draw(text: String, x: Float, y: Float, scale: Float, color: Int, shadow: Boolean): Int {
        resetState()

        this.scale = scale

        return if (shadow) {
            val shadowX = drawLayer(text, x + 0.5f, y + 0.5f, color, true)
            max(shadowX, drawLayer(text, x, y, color, false))
        } else {
            drawLayer(text, x, y, color, false)
        }
    }

    private fun drawLayer(text: String, x: Float, y: Float, color: Int, shadow: Boolean): Int {
        var finalColor = color
        if ((finalColor and -67108864) == 0) {
            finalColor = finalColor or -16777216
        }

        if (shadow) {
            finalColor = (finalColor and 16579836 shr 2) or (finalColor and -16777216)
        }

        red = (finalColor shr 16 and 255) / 255f
        green = (finalColor shr 8 and 255) / 255f
        blue = (finalColor and 255) / 255f
        alpha = (finalColor ushr 24 and 255) / 255f

        this.x = x
        this.y = y

        renderString(text, shadow)

        return this.x.toInt()
    }

    private fun renderString(text: String, shadow: Boolean) {
        var i = 0
        while (i < text.length) {
            val c = text[i]

            if (c == FORMATTING_CODE_CHAR && i + 1 < text.length) {
                val formatCode = COLOR_CODES.indexOf(
                    text[i + 1].lowercaseChar()
                )

                when {
                    formatCode < 16 -> {
                        // Color code
                        obfuscated = false
                        bold = false
                        strikethrough = false
                        underline = false
                        italic = false

                        val colorIndex = if (formatCode < 0 || formatCode > 15) 15 else formatCode
                        val adjustedIndex = if (shadow) colorIndex + 16 else colorIndex
                        val colorValue = colorCodes[adjustedIndex]

                        red = (colorValue shr 16) / 255f
                        green = (colorValue shr 8 and 255) / 255f
                        blue = (colorValue and 255) / 255f
                    }
                    formatCode == 16 -> obfuscated = true
                    formatCode == 17 -> bold = true
                    formatCode == 18 -> strikethrough = true
                    formatCode == 19 -> underline = true
                    formatCode == 20 -> italic = true
                    formatCode == 21 -> {
                        // Reset
                        obfuscated = false
                        bold = false
                        strikethrough = false
                        underline = false
                        italic = false
                    }
                }

                i += 2
                continue
            }

            renderChar(c, shadow)
            i++
        }
    }

    private fun renderChar(char: Char, shadow: Boolean) {
        if (char == ' ') {
            x += 4f * scale
            return
        }

        val charIndex = CHAR_MAP.indexOf(char)
        var finalChar = char

        if (obfuscated && charIndex != -1) {
            val originalWidth = getCharWidth(char)
            do {
                val randomIndex = random.nextInt(CHAR_MAP.length)
                finalChar = CHAR_MAP[randomIndex]
            } while (originalWidth != getCharWidth(finalChar))
        }

        val width = if (charIndex != -1 && !unicode) {
            drawCharNormal(finalChar, shadow)
        } else {
            drawCharUnicode(finalChar, shadow)
        }

        if (bold) {
            x += 1f * scale
            if (!unicode) {
                drawCharNormal(finalChar, shadow)
            } else {
                drawCharUnicode(finalChar, shadow)
            }
            x -= 1f * scale
        }

        if (strikethrough) {
            val lineY = y + (fontHeight / 2f) * scale
            quadRenderer.drawQuad(x, lineY, width, 1f * scale, red, green, blue, alpha)
        }

        if (underline) {
            val lineY = y + fontHeight.toFloat() * scale
            quadRenderer.drawQuad(x, lineY - 1f * scale, width, 1f * scale, red, green, blue, alpha)
        }

        x += width
    }

    private fun drawCharNormal(char: Char, shadow: Boolean): Float {
        val charIndex = CHAR_MAP.indexOf(char)
        if (charIndex == -1) return 0f

        val col = charIndex % 16
        val row = charIndex / 16

        val charWidth = characterWidths[charIndex].toFloat()
        val texWidth = 128f
        val texHeight = 128f

        val u0 = (col * 8) / texWidth
        val v0 = (row * 8) / texHeight
        val u1 = ((col * 8) + charWidth - 1f) / texWidth
        val v1 = ((row * 8) + 7.99f) / texHeight

        val offset = if (shadow) 1f * scale else 0f
        val italicShift = if (italic) 1f * scale else 0f

        quadRenderer.drawTexturedQuad(
            x + offset + italicShift,
            y,
            (charWidth - 1f) * scale,
            7.99f * scale,
            u0, v0, u1, v1,
            red, green, blue, alpha,
            fontTextureId
        )

        return charWidth * scale
    }

    private fun drawCharUnicode(char: Char, shadow: Boolean): Float {
        val glyphWidth = glyphWidths[char.code].toInt()
        if (glyphWidth == 0) return 0f

        val page = char.code / 256
        val pageTexture = getUnicodePage(page, MinecraftClient.getInstance()) ?: return 0f
        val textureId = quadRenderer.bindTexture(pageTexture)

        val startWidth = (glyphWidth ushr 4).toFloat()
        val endWidth = (glyphWidth and 15).toFloat() + 1f

        val col = char.code % 16
        val row = (char.code and 255) / 16

        val u0 = (col * 16 + startWidth) / 256f
        val v0 = (row * 16) / 256f
        val u1 = (col * 16 + endWidth - 0.02f) / 256f
        val v1 = (row * 16 + 15.98f) / 256f

        val width = (endWidth - startWidth) / 2f
        val offset = if (shadow) 1f * scale else 0f

        quadRenderer.drawTexturedQuad(
            x + offset,
            y,
            width * scale,
            7.99f * scale,
            u0, v0, u1, v1,
            red, green, blue, alpha,
            textureId
        )

        return (width + 1f) * scale
    }

    /**
     * Get the width of a string in pixels
     */
    fun getTextWidth(text: String): Int {
        var width = 0
        var isBold = false

        for (i in text.indices) {
            val c = text[i]
            val charWidth = getCharWidth(c)

            if (charWidth < 0 && i < text.length - 1) {
                val formatChar = text[i + 1]
                isBold = formatChar == 'l' || formatChar == 'L'
                continue
            }

            width += charWidth
            if (isBold && charWidth > 0) {
                width++
            }
        }

        return width
    }

    /**
     * Get the width of a character
     */
    fun getCharWidth(char: Char): Int {
        if (char == FORMATTING_CODE_CHAR) return -1
        if (char == ' ') return 4

        val charIndex = CHAR_MAP.indexOf(char)

        return if (char.code > 0 && charIndex != -1 && !unicode) {
            characterWidths[charIndex]
        } else if (glyphWidths[char.code].toInt() != 0) {
            val startWidth = glyphWidths[char.code].toInt() ushr 4
            val endWidth = glyphWidths[char.code].toInt() and 15
            ((endWidth - startWidth) / 2) + 1
        } else {
            0
        }
    }

    private fun resetState() {
        obfuscated = false
        bold = false
        italic = false
        underline = false
        strikethrough = false
    }
}
