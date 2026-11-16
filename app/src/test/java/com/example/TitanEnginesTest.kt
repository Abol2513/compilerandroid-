package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.engine.ElfParser
import com.example.domain.engine.PatchPlaybookEngine
import com.example.domain.engine.ResourceCalculator
import com.example.domain.engine.StreamingArchiveAnalyzer
import com.example.domain.model.BuildArchitectInput
import com.example.domain.model.EcosystemStack
import com.example.domain.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TitanEnginesTest {

    @Test
    fun `resource calculator enforces 4x archive disk rule and native concurrency formula`() {
        val output = ResourceCalculator.calculate(
            BuildArchitectInput(
                archiveSizeGb = 10.0,
                uncompressedSizeGb = 22.0,
                hostRamGb = 16.0,
                cpuCores = 16,
                freeDiskGb = 45.0,
                agpMajorMinor = "9.1",
                usesNdkCpp = true,
                hasMultiGbAssets = true,
            )
        )
        // max(4*10, 1.8*22 + 15) = max(40.0, 54.6) = 54.6
        assertEquals(54.6, output.requiredDiskGb, 0.01)
        assertFalse(output.isDiskSufficient)
        assertEquals(9.6, output.diskDeficitGb, 0.01)
        // floor(16 / 2.2) = 7
        assertEquals(7, output.safeNativeJobs)
        assertTrue(output.recommendedJdk.contains("21"))
        assertTrue(output.generatedGradleProperties.contains("org.gradle.vfs.watch=false"))
    }

    @Test
    fun `elf parser distinguishes 16KB aligned and 4KB misaligned ELF64 binaries`() {
        val alignedElf = buildSyntheticElf64(ptLoadAlign = 16384L)
        val alignedResult = ElfParser.inspectHeaderBytes("lib/arm64-v8a/libok.so", alignedElf)
        assertNotNull(alignedResult)
        assertTrue(alignedResult!!.is16KbAligned)
        assertEquals("arm64-v8a", alignedResult.abi)

        val unalignedElf = buildSyntheticElf64(ptLoadAlign = 4096L)
        val unalignedResult = ElfParser.inspectHeaderBytes("lib/arm64-v8a/libbad.so", unalignedElf)
        assertNotNull(unalignedResult)
        assertFalse(unalignedResult!!.is16KbAligned)
        assertEquals(4096L, unalignedResult.minLoadAlignBytes)
    }

    @Test
    fun `streaming archive analyzer detects zipslip, stale cache, junk, stack and elf alignment`() {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addEntry(name: String, bytes: ByteArray) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(bytes)
                zos.closeEntry()
            }
            addEntry(
                "MyProject/gradle/libs.versions.toml",
                """
                [versions]
                agp = "8.7.2"
                kotlin = "2.1.0"
                """.trimIndent().toByteArray(),
            )
            addEntry(
                "MyProject/app/build.gradle.kts",
                """
                plugins { id("com.android.application") }
                android { compileSdk = 35 }
                """.trimIndent().toByteArray(),
            )
            addEntry("MyProject/__MACOSX/.DS_Store", byteArrayOf(1, 2, 3))
            addEntry("MyProject/app/.cxx/CMakeCache.txt", "CMAKE_HOME_DIRECTORY:INTERNAL=C:/Users/dev".toByteArray())
            addEntry("../../etc/passwd", "evil".toByteArray())
            addEntry("MyProject/app/src/main/jniLibs/arm64-v8a/libcore.so", buildSyntheticElf64(16384L))
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val analyzer = StreamingArchiveAnalyzer(context)
        val report = analyzer.analyzeStream(
            fileName = "mega_project.zip",
            totalBytesHint = baos.size().toLong(),
            inputStream = ByteArrayInputStream(baos.toByteArray()),
            localApkFile = null,
            onProgress = { _, _, _, _ -> },
        )

        assertEquals(Severity.CRITICAL, report.overallSeverity)
        assertTrue(report.firewallFindings.any { it.id == "zipslip" && it.severity == Severity.CRITICAL })
        assertTrue(report.firewallFindings.any { it.id == "stale_cache" && it.severity == Severity.WARN })
        assertTrue(report.firewallFindings.any { it.id == "os_junk" && it.severity == Severity.WARN })
        assertTrue(report.detectedStacks.contains(EcosystemStack.NATIVE_GRADLE))
        assertTrue(report.detectedStacks.contains(EcosystemStack.NDK_CPP_RUST_GO))
        assertEquals("8.7.2", report.toolchain.agpVersion)
        assertEquals("2.1.0", report.toolchain.kotlinVersion)
        assertEquals(1, report.elfResults.size)
        assertTrue(report.elfResults.first().is16KbAligned)
    }

    @Test
    fun `patch playbook diagnoser matches 16KB and namespace errors`() {
        val matches = PatchPlaybookEngine.diagnoseLog(
            "Execution failed: Namespace not specified and LOAD segment not aligned at 16384"
        )
        assertTrue(matches.any { it.id == "agp_missing_namespace" })
        assertTrue(matches.any { it.id == "ndk_16kb_alignment" })
    }

    private fun buildSyntheticElf64(ptLoadAlign: Long): ByteArray {
        val buf = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0, 0x7F.toByte())
        buf.put(1, 'E'.code.toByte())
        buf.put(2, 'L'.code.toByte())
        buf.put(3, 'F'.code.toByte())
        buf.put(4, 2.toByte()) // ELF64
        buf.put(5, 1.toByte()) // Little Endian
        buf.putLong(32, 64L)   // e_phoff = 64
        buf.putShort(54, 56.toShort()) // e_phentsize = 56
        buf.putShort(56, 1.toShort())  // e_phnum = 1
        // Program header at offset 64
        buf.putInt(64, 1) // PT_LOAD
        buf.putLong(64 + 48, ptLoadAlign) // p_align
        return buf.array()
    }
}
