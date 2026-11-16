package com.example.domain.engine

import com.example.domain.model.ElfAlignmentResult
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ElfParser {
    private const val PT_LOAD = 1
    private const val PAGE_16KB = 16384L

    fun inspectHeaderBytes(soPath: String, bytes: ByteArray): ElfAlignmentResult? {
        if (bytes.size < 64) return null
        // Verify ELF magic: 0x7F 'E' 'L' 'F'
        if (bytes[0] != 0x7F.toByte() ||
            bytes[1] != 'E'.code.toByte() ||
            bytes[2] != 'L'.code.toByte() ||
            bytes[3] != 'F'.code.toByte()
        ) {
            return null
        }

        val eiClass = bytes[4].toInt() and 0xFF // 1 = 32-bit, 2 = 64-bit
        val eiData = bytes[5].toInt() and 0xFF  // 1 = Little Endian, 2 = Big Endian
        val byteOrder = when (eiData) {
            1 -> ByteOrder.LITTLE_ENDIAN
            2 -> ByteOrder.BIG_ENDIAN
            else -> return null
        }

        val buf = ByteBuffer.wrap(bytes).order(byteOrder)
        val is64Bit = eiClass == 2
        if (eiClass != 1 && eiClass != 2) return null

        val phOff: Long
        val phEntSize: Int
        val phNum: Int

        if (is64Bit) {
            phOff = buf.getLong(32)
            phEntSize = buf.getShort(54).toInt() and 0xFFFF
            phNum = buf.getShort(56).toInt() and 0xFFFF
        } else {
            phOff = (buf.getInt(28).toLong() and 0xFFFFFFFFL)
            phEntSize = buf.getShort(42).toInt() and 0xFFFF
            phNum = buf.getShort(44).toInt() and 0xFFFF
        }

        if (phOff < 0L || phEntSize <= 0 || phNum <= 0) return null

        var minLoadAlign = Long.MAX_VALUE
        var foundLoad = false

        for (i in 0 until phNum) {
            val entryOffsetLong = phOff + i.toLong() * phEntSize.toLong()
            if (entryOffsetLong < 0L || entryOffsetLong + phEntSize > bytes.size.toLong()) {
                break
            }
            val off = entryOffsetLong.toInt()
            val pType = buf.getInt(off)
            if (pType == PT_LOAD) {
                foundLoad = true
                val align = if (is64Bit) {
                    if (off + 56 > bytes.size) break
                    buf.getLong(off + 48)
                } else {
                    if (off + 32 > bytes.size) break
                    buf.getInt(off + 28).toLong() and 0xFFFFFFFFL
                }
                if (align in 1 until minLoadAlign) {
                    minLoadAlign = align
                }
            }
        }

        if (!foundLoad || minLoadAlign == Long.MAX_VALUE) return null

        val abi = extractAbiFromPath(soPath)
        val aligned16Kb = !is64Bit || minLoadAlign >= PAGE_16KB
        return ElfAlignmentResult(
            soPath = soPath,
            abi = abi,
            is64Bit = is64Bit,
            minLoadAlignBytes = minLoadAlign,
            is16KbAligned = aligned16Kb,
        )
    }

    private fun extractAbiFromPath(path: String): String {
        val segments = path.replace('\\', '/').split('/')
        val libIndex = segments.indexOfLast { it == "lib" || it == "jniLibs" }
        return if (libIndex >= 0 && libIndex + 1 < segments.size) {
            segments[libIndex + 1]
        } else {
            "unknown"
        }
    }
}
