package com.example.domain.model

enum class ArchiveKind(val label: String) {
    ZIP("آرشیو سورس (ZIP)"),
    APK("بسته نصبی اندروید (APK)"),
    AAB("باندل گوگل‌پلی (AAB)"),
    AAR("کتابخانه اندروید (AAR)"),
    UNKNOWN("بسته آرشیو"),
}

enum class Severity(val label: String) {
    PASS("تایید شده"),
    WARN("هشدار"),
    CRITICAL("بحرانی"),
}

enum class EcosystemStack(
    val displayName: String,
    val buildSystem: String,
) {
    NATIVE_GRADLE("اندروید بومی (Gradle)", "Gradle + AGP"),
    NDK_CPP_RUST_GO("هسته NDK / C++ / Rust / Go", "CMake / ndk-build / cargo-ndk"),
    FLUTTER_DART("فلاتر و دارت (Flutter)", "flutter + Gradle"),
    REACT_NATIVE_EXPO("ری‌اکت نیتیو / Expo", "Metro + Hermes + Gradle"),
    CAPACITOR_CORDOVA("کپاسیتور / آیونیک / کوردوا", "Web Bundle + Gradle"),
    KOTLIN_MULTIPLATFORM("کاتلین مالتی‌پلتفرم (KMP)", "KMP + Compose MP"),
    DOTNET_MAUI("دات‌نت MAUI / زامارین", "dotnet publish"),
    UNITY_EXPORTED("یونیتی خروجی‌گرفته (Gradle)", "unityLibrary + IL2CPP NDK"),
    UNITY_RAW("پروژه خام یونیتی (Unity)", "Unity CLI Batchmode"),
    UNREAL_GODOT_LIBGDX("آنریل / گودو / LibGDX", "UAT / Godot / Gradle"),
    PYTHON_EDGE_AI("پایتون / Kivy / Chaquopy", "buildozer / p4a / Gradle"),
    LEGACY_ANT_ECLIPSE("پروژه قدیمی Ant / Eclipse", "ساخت خودکار Gradle"),
    DECOMPILED_APKTOOL("دی‌کامپایل‌شده Apktool", "apktool b + zipalign + apksigner"),
    AOSP_KERNEL("سورس AOSP / کرنل اندروید", "Soong / Clang LLVM"),
}

data class FirewallFinding(
    val id: String,
    val title: String,
    val detail: String,
    val severity: Severity,
    val affectedSamples: List<String> = emptyList(),
)

data class PayloadSegment(
    val category: String,
    val bytes: Long,
    val entryCount: Int,
    val colorToken: Long,
)

data class ElfAlignmentResult(
    val soPath: String,
    val abi: String,
    val is64Bit: Boolean,
    val minLoadAlignBytes: Long,
    val is16KbAligned: Boolean,
)

data class ApkMetadataSummary(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val isDebuggable: Boolean,
    val dexCount: Int,
    val nativeAbis: List<String>,
    val hasSigningBlockOrCert: Boolean,
)

data class ToolchainRecommendation(
    val agpVersion: String?,
    val gradleVersion: String?,
    val kotlinVersion: String?,
    val compileSdk: Int?,
    val targetSdk: Int?,
    val minSdk: Int?,
    val ndkVersion: String?,
    val recommendedJdk: String,
    val compatibleJdks: String,
    val notes: String,
)

data class ScanReport(
    val id: Long = 0L,
    val fileName: String,
    val archiveKind: ArchiveKind,
    val scannedAtEpochMs: Long,
    val compressedBytes: Long,
    val uncompressedBytes: Long,
    val entryCount: Int,
    val detectedStacks: List<EcosystemStack>,
    val primaryStack: EcosystemStack,
    val overallSeverity: Severity,
    val firewallFindings: List<FirewallFinding>,
    val payloadSegments: List<PayloadSegment>,
    val elfResults: List<ElfAlignmentResult>,
    val apkMetadata: ApkMetadataSummary?,
    val toolchain: ToolchainRecommendation,
    val largeAssetsNeedingNoCompress: List<String>,
    val zip32RiskSummary: String,
    val recommendedArtifactStrategy: String,
    val remediationSteps: List<String>,
) {
    val compressionRatio: Double
        get() = if (compressedBytes > 0L) uncompressedBytes.toDouble() / compressedBytes.toDouble() else 1.0

    val passCount: Int
        get() = firewallFindings.count { it.severity == Severity.PASS }

    val warnCount: Int
        get() = firewallFindings.count { it.severity == Severity.WARN }

    val criticalCount: Int
        get() = firewallFindings.count { it.severity == Severity.CRITICAL }
}

sealed interface ScanProgressState {
    data object Idle : ScanProgressState
    data class Scanning(
        val fileName: String,
        val phaseLabel: String,
        val entriesProcessed: Int,
        val bytesProcessed: Long,
        val totalBytesHint: Long,
    ) : ScanProgressState

    data class Success(val report: ScanReport) : ScanProgressState
    data class Error(val message: String) : ScanProgressState
}

data class HostTelemetry(
    val deviceModel: String,
    val primaryAbi: String,
    val supportedAbis: List<String>,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalDiskGb: Double,
    val freeDiskGb: Double,
    val androidRelease: String,
    val sdkInt: Int,
)

data class BuildArchitectInput(
    val archiveSizeGb: Double = 2.5,
    val uncompressedSizeGb: Double = 6.0,
    val hostRamGb: Double = 16.0,
    val cpuCores: Int = 8,
    val freeDiskGb: Double = 60.0,
    val agpMajorMinor: String = "9.1",
    val usesNdkCpp: Boolean = true,
    val hasMultiGbAssets: Boolean = true,
)

data class BuildArchitectOutput(
    val requiredDiskGb: Double,
    val diskDeficitGb: Double,
    val isDiskSufficient: Boolean,
    val safeNativeJobs: Int,
    val gradleWorkersMax: Int,
    val recommendedHeapGb: Int,
    val recommendedGcFlag: String,
    val recommendedJdk: String,
    val packagingStrategy: String,
    val ramWarning: String?,
    val generatedGradleProperties: String,
    val generatedPackagingDsl: String,
    val generatedEnvExports: String,
)

enum class PatchCategory(val label: String) {
    ALL("همه موارد"),
    AGP_GRADLE("AGP و Gradle"),
    MANIFEST_API36("مانیفست و API 35+"),
    NDK_16KB("NDK و هم‌ترازی 16KB"),
    KOTLIN_KSP("کاتلین و KSP"),
    PACKAGING_ZIP32("محدودیت 4GB / ZIP32"),
}

data class PatchRule(
    val id: String,
    val title: String,
    val category: PatchCategory,
    val errorSignatures: List<String>,
    val rootCause: String,
    val minimalPatchDiff: String,
    val verificationHint: String,
    val targetFileLocation: String,
)
