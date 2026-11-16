package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ApkMetadataSummary
import com.example.domain.model.ArchiveKind
import com.example.domain.model.EcosystemStack
import com.example.domain.model.ElfAlignmentResult
import com.example.domain.model.FirewallFinding
import com.example.domain.model.PayloadSegment
import com.example.domain.model.ScanReport
import com.example.domain.model.Severity
import com.example.domain.model.ToolchainRecommendation
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "scan_reports")
data class ScanReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fileName: String,
    val archiveKind: String,
    val scannedAtEpochMs: Long,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val entryCount: Int,
    val primaryStack: String,
    val overallSeverity: String,
    val payloadJson: String,
)

object ScanReportMapper {

    fun toEntity(report: ScanReport): ScanReportEntity {
        val root = JSONObject().apply {
            put("detectedStacks", JSONArray(report.detectedStacks.map { it.name }))
            put("zip32RiskSummary", report.zip32RiskSummary)
            put("recommendedArtifactStrategy", report.recommendedArtifactStrategy)
            put("largeAssets", JSONArray(report.largeAssetsNeedingNoCompress))
            put("remediationSteps", JSONArray(report.remediationSteps))
            put("findings", JSONArray().apply {
                report.firewallFindings.forEach { f ->
                    put(JSONObject().apply {
                        put("id", f.id)
                        put("title", f.title)
                        put("detail", f.detail)
                        put("severity", f.severity.name)
                        put("samples", JSONArray(f.affectedSamples))
                    })
                }
            })
            put("segments", JSONArray().apply {
                report.payloadSegments.forEach { s ->
                    put(JSONObject().apply {
                        put("category", s.category)
                        put("bytes", s.bytes)
                        put("entryCount", s.entryCount)
                        put("colorToken", s.colorToken)
                    })
                }
            })
            put("elfResults", JSONArray().apply {
                report.elfResults.forEach { e ->
                    put(JSONObject().apply {
                        put("soPath", e.soPath)
                        put("abi", e.abi)
                        put("is64Bit", e.is64Bit)
                        put("minLoadAlignBytes", e.minLoadAlignBytes)
                        put("is16KbAligned", e.is16KbAligned)
                    })
                }
            })
            put("toolchain", JSONObject().apply {
                put("agpVersion", report.toolchain.agpVersion)
                put("gradleVersion", report.toolchain.gradleVersion)
                put("kotlinVersion", report.toolchain.kotlinVersion)
                put("compileSdk", report.toolchain.compileSdk)
                put("targetSdk", report.toolchain.targetSdk)
                put("minSdk", report.toolchain.minSdk)
                put("ndkVersion", report.toolchain.ndkVersion)
                put("recommendedJdk", report.toolchain.recommendedJdk)
                put("compatibleJdks", report.toolchain.compatibleJdks)
                put("notes", report.toolchain.notes)
            })
            report.apkMetadata?.let { apk ->
                put("apkMetadata", JSONObject().apply {
                    put("packageName", apk.packageName)
                    put("versionName", apk.versionName)
                    put("versionCode", apk.versionCode)
                    put("minSdk", apk.minSdk)
                    put("targetSdk", apk.targetSdk)
                    put("isDebuggable", apk.isDebuggable)
                    put("dexCount", apk.dexCount)
                    put("nativeAbis", JSONArray(apk.nativeAbis))
                    put("hasSigningBlockOrCert", apk.hasSigningBlockOrCert)
                })
            }
        }

        return ScanReportEntity(
            id = report.id,
            fileName = report.fileName,
            archiveKind = report.archiveKind.name,
            scannedAtEpochMs = report.scannedAtEpochMs,
            compressedBytes = report.compressedBytes,
            uncompressedBytes = report.uncompressedBytes,
            entryCount = report.entryCount,
            primaryStack = report.primaryStack.name,
            overallSeverity = report.overallSeverity.name,
            payloadJson = root.toString(),
        )
    }

    fun toDomain(entity: ScanReportEntity): ScanReport {
        val root = JSONObject(entity.payloadJson)

        val stacksArr = root.optJSONArray("detectedStacks")
        val stacks = buildList {
            if (stacksArr != null) {
                for (i in 0 until stacksArr.length()) {
                    runCatching { EcosystemStack.valueOf(stacksArr.getString(i)) }.getOrNull()?.let { add(it) }
                }
            }
        }.ifEmpty {
            listOf(runCatching { EcosystemStack.valueOf(entity.primaryStack) }.getOrDefault(EcosystemStack.NATIVE_GRADLE))
        }

        val findingsArr = root.optJSONArray("findings")
        val findings = buildList {
            if (findingsArr != null) {
                for (i in 0 until findingsArr.length()) {
                    val obj = findingsArr.getJSONObject(i)
                    val samplesArr = obj.optJSONArray("samples")
                    val samples = buildList {
                        if (samplesArr != null) {
                            for (j in 0 until samplesArr.length()) add(samplesArr.getString(j))
                        }
                    }
                    add(
                        FirewallFinding(
                            id = obj.optString("id"),
                            title = obj.optString("title"),
                            detail = obj.optString("detail"),
                            severity = runCatching { Severity.valueOf(obj.optString("severity")) }.getOrDefault(Severity.WARN),
                            affectedSamples = samples,
                        )
                    )
                }
            }
        }

        val segArr = root.optJSONArray("segments")
        val segments = buildList {
            if (segArr != null) {
                for (i in 0 until segArr.length()) {
                    val obj = segArr.getJSONObject(i)
                    add(
                        PayloadSegment(
                            category = obj.optString("category"),
                            bytes = obj.optLong("bytes"),
                            entryCount = obj.optInt("entryCount"),
                            colorToken = obj.optLong("colorToken"),
                        )
                    )
                }
            }
        }

        val elfArr = root.optJSONArray("elfResults")
        val elfs = buildList {
            if (elfArr != null) {
                for (i in 0 until elfArr.length()) {
                    val obj = elfArr.getJSONObject(i)
                    add(
                        ElfAlignmentResult(
                            soPath = obj.optString("soPath"),
                            abi = obj.optString("abi"),
                            is64Bit = obj.optBoolean("is64Bit"),
                            minLoadAlignBytes = obj.optLong("minLoadAlignBytes"),
                            is16KbAligned = obj.optBoolean("is16KbAligned"),
                        )
                    )
                }
            }
        }

        val tcObj = root.optJSONObject("toolchain")
        val toolchain = ToolchainRecommendation(
            agpVersion = tcObj?.optString("agpVersion")?.takeIf { it.isNotEmpty() && it != "null" },
            gradleVersion = tcObj?.optString("gradleVersion")?.takeIf { it.isNotEmpty() && it != "null" },
            kotlinVersion = tcObj?.optString("kotlinVersion")?.takeIf { it.isNotEmpty() && it != "null" },
            compileSdk = tcObj?.optInt("compileSdk", -1)?.takeIf { it > 0 },
            targetSdk = tcObj?.optInt("targetSdk", -1)?.takeIf { it > 0 },
            minSdk = tcObj?.optInt("minSdk", -1)?.takeIf { it > 0 },
            ndkVersion = tcObj?.optString("ndkVersion")?.takeIf { it.isNotEmpty() && it != "null" },
            recommendedJdk = tcObj?.optString("recommendedJdk").orEmpty().ifEmpty { "JDK 17 / JDK 21" },
            compatibleJdks = tcObj?.optString("compatibleJdks").orEmpty().ifEmpty { "JDK 17 / JDK 21" },
            notes = tcObj?.optString("notes").orEmpty(),
        )

        val apkObj = root.optJSONObject("apkMetadata")
        val apkMeta = apkObj?.let {
            val abiArr = it.optJSONArray("nativeAbis")
            val abis = buildList {
                if (abiArr != null) {
                    for (i in 0 until abiArr.length()) add(abiArr.getString(i))
                }
            }
            ApkMetadataSummary(
                packageName = it.optString("packageName").takeIf { s -> s.isNotEmpty() && s != "null" },
                versionName = it.optString("versionName").takeIf { s -> s.isNotEmpty() && s != "null" },
                versionCode = if (it.has("versionCode") && !it.isNull("versionCode")) it.optLong("versionCode") else null,
                minSdk = it.optInt("minSdk", -1).takeIf { v -> v > 0 },
                targetSdk = it.optInt("targetSdk", -1).takeIf { v -> v > 0 },
                isDebuggable = it.optBoolean("isDebuggable"),
                dexCount = it.optInt("dexCount"),
                nativeAbis = abis,
                hasSigningBlockOrCert = it.optBoolean("hasSigningBlockOrCert"),
            )
        }

        val largeArr = root.optJSONArray("largeAssets")
        val largeAssets = buildList {
            if (largeArr != null) {
                for (i in 0 until largeArr.length()) add(largeArr.getString(i))
            }
        }

        val remArr = root.optJSONArray("remediationSteps")
        val remSteps = buildList {
            if (remArr != null) {
                for (i in 0 until remArr.length()) add(remArr.getString(i))
            }
        }

        return ScanReport(
            id = entity.id,
            fileName = entity.fileName,
            archiveKind = runCatching { ArchiveKind.valueOf(entity.archiveKind) }.getOrDefault(ArchiveKind.UNKNOWN),
            scannedAtEpochMs = entity.scannedAtEpochMs,
            compressedBytes = entity.compressedBytes,
            uncompressedBytes = entity.uncompressedBytes,
            entryCount = entity.entryCount,
            detectedStacks = stacks,
            primaryStack = runCatching { EcosystemStack.valueOf(entity.primaryStack) }.getOrDefault(EcosystemStack.NATIVE_GRADLE),
            overallSeverity = runCatching { Severity.valueOf(entity.overallSeverity) }.getOrDefault(Severity.WARN),
            firewallFindings = findings,
            payloadSegments = segments,
            elfResults = elfs,
            apkMetadata = apkMeta,
            toolchain = toolchain,
            largeAssetsNeedingNoCompress = largeAssets,
            zip32RiskSummary = root.optString("zip32RiskSummary"),
            recommendedArtifactStrategy = root.optString("recommendedArtifactStrategy"),
            remediationSteps = remSteps,
        )
    }
}
