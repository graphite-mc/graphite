package com.graphite.utility

import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.asArrayBuffer
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlin.use

inline fun arrayBufferOf(input: ByteArray, action: (ArrayBuffer) -> Unit) = Arena.ofConfined().use { arena ->
    val byteSizeToCopy = (input.size * Byte.SIZE_BYTES).toLong()
    val segment = arena.allocate(byteSizeToCopy)
    MemorySegment.copy(MemorySegment.ofArray(input), 0, segment, 0, byteSizeToCopy)
    segment.asArrayBuffer(byteSizeToCopy)
        .let(action)
}

inline fun shortBufferOf(input: ShortArray, action: (ArrayBuffer) -> Unit) = Arena.ofConfined().use { arena ->
    val byteSizeToCopy = (input.size * Short.SIZE_BYTES).toLong()
    val segment = arena.allocate(byteSizeToCopy)
    MemorySegment.copy(MemorySegment.ofArray(input), 0, segment, 0, byteSizeToCopy)
    segment.asArrayBuffer(byteSizeToCopy)
        .let(action)
}