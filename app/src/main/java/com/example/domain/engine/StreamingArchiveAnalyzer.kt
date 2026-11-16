package com.example.domain.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.domain.model.ApkMetadataSummary
import com.example.domain.model.ArchiveKind
import com.example.domain.model.EcosystemStack
import com.example.domain.model.ElfAlignmentResult
import com.example.domain.model.FirewallFinding
import com.example.domain.model.PayloadSegment
import com.example.domain.model.ScanReport
import com.example.domain.model.Severity
import com.example.domain.model.ToolchainRecommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import kotlin.math.max

class StreamingArchiveAnalyzer(private val context: Context) {

    suspend fun analyzeUri(
        uri: Uri,
        onProgress: (phase: String, entries: Int, bytesProcessed: Long, totalHint: Long) -> Unit,
    ): Result<ScanReport> = withContext(Dispatchers.IO) {
        runCatching {
            val (displayName, sizeHint) = queryUriMetadata(uri)
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: error("امکان باز کردن جریان ورودی برای فایل انتخاب‌شده وجود ندارد.")
            inputStream.use { stream ->
                analyzeStream(
                    fileName = displayName,
                    totalBytesHint = sizeHint,
                    inputStream = stream,
                    localApkFile = null,
                    onProgress = onProgress,
                )
            }
        }
    }

    suspend fun analyzeSelfApk(
        onProgress: (phase: String, entries: Int, bytesProcessed: Long, totalHint: Long) -> Unit,
    ): Result<ScanReport> = withContext(Dispatchers.IO) {
        runCatching {
            val sourcePath = context.applicationInfo.sourceDir
            val apkFile = File(sourcePath)
            val fileName = "self_${context.packageName}.apk"
            FileInputStream(apkFile).use { stream ->
                analyzeStream(
                    fileName = fileName,
                    totalBytesHint = apkFile.length(),
                    inputStream = stream,
                    localApkFile = apkFile,
                    onProgress = onProgress,
                )
            }
        }
    }

    internal fun analyzeStream(
        fileName: String,
        totalBytesHint: Long,
        inputStream: InputStream,
        localApkFile: File?,
        onProgress: (phase: String, entries: Int, bytesProcessed: Long, totalHint: Long) -> Unit,
    ): ScanReport {
        val kind = detectArchiveKind(fileName)
        var entryCount = 0
        var totalCompressed = 0L
        var totalUncompressed = 0L

        val zipSlipSamples = mutableListOf<String>()
        val nestedArchiveSamples = mutableListOf<String>()
        val osJunkSamples = mutableListOf<String>()
        val staleCacheSamples = mutableListOf<String>()
        val highRatioSamples = mutableListOf<String>()
        val largeNoCompressAssets = mutableListOf<String>()
        var encryptedDetected = false

        var nativeSoBytes = 0L
        var nativeSoCount = 0
        var codeDexBytes = 0L
        var codeDexCount = 0
        var assetModelBytes = 0L
        var assetModelCount = 0
        var resourceBytes = 0L
        var resourceCount = 0
        var sourceConfigBytes = 0L
        var sourceConfigCount = 0
        var junkCacheBytes = 0L
        var junkCacheCount = 0

        val pathSet = HashSet<String>(512)
        val configSnippets = LinkedHashMap<String, String>()
        val elfResults = mutableListOf<ElfAlignmentResult>()
        val detectedAbis = LinkedHashSet<String>()
        var hasCertSignature = false
        var dexFileCount = 0

        val scratch = ByteArray(65536)
        val zis = ZipInputStream(BufferedInputStream(inputStream, 65536))

        onProgress("در حال خواندن جریانی هدرهای آرشیو", 0, 0L, totalBytesHint)

        var lastProgressEmit = System.currentTimeMillis()
        while (true) {
            val entry: ZipEntry = try {
                zis.nextEntry ?: break
            } catch (e: ZipException) {
                if (e.message?.contains("encrypted", ignoreCase = true) == true) {
                    encryptedDetected = true
                    break
                }
                throw e
            }

            entryCount++
            val rawName = entry.name
            val normalized = rawName.replace('\\', '/')
            if (pathSet.size < 8000) {
                pathSet.add(normalized)
            }

            if (isZipSlipPath(rawName)) {
                if (zipSlipSamples.size < 6) zipSlipSamples.add(rawName)
            }

            if (entry.isDirectory) {
                zis.closeEntry()
                continue
            }

            val lowerName = normalized.lowercase()

            val isConfig = isInspectableConfig(lowerName)
            val isElfCandidate = lowerName.endsWith(".so") &&
                (lowerName.contains("/lib/") || lowerName.startsWith("lib/") || lowerName.contains("jnilibs/")) &&
                elfResults.size < 40

            val uncompressedSize: Long
            if (isConfig) {
                val contentBytes = readUpTo(zis, 131_072, scratch)
                uncompressedSize = if (entry.size >= 0L) entry.size else contentBytes.size.toLong()
                configSnippets[normalized] = contentBytes.toString(Charsets.UTF_8)
            } else if (isElfCandidate) {
                val headerBytes = readUpTo(zis, 16_384, scratch)
                val extraCount = if (entry.size < 0L) drainEntry(zis, scratch) else 0L
                uncompressedSize = if (entry.size >= 0L) entry.size else headerBytes.size.toLong() + extraCount
                ElfParser.inspectHeaderBytes(normalized, headerBytes)?.let { elf ->
                    elfResults.add(elf)
                    detectedAbis.add(elf.abi)
                }
            } else {
                uncompressedSize = if (entry.size >= 0L) {
                    entry.size
                } else {
                    drainEntry(zis, scratch)
                }
            }

            val compressedSize = max(0L, entry.compressedSize)
            totalCompressed += compressedSize
            totalUncompressed += uncompressedSize

            if (compressedSize >= 1_048_576L && uncompressedSize > compressedSize * 100L) {
                if (highRatioSamples.size < 5) {
                    highRatioSamples.add("$normalized (${uncompressedSize / compressedSize}x)")
                }
            }

            if (hasNestedArchiveExtension(lowerName)) {
                if (nestedArchiveSamples.size < 6) nestedArchiveSamples.add(normalized)
            }

            if (isOsJunk(normalized)) {
                if (osJunkSamples.size < 6) osJunkSamples.add(normalized)
                junkCacheBytes += uncompressedSize
                junkCacheCount++
            } else if (isStaleForeignCache(lowerName)) {
                if (staleCacheSamples.size < 6) staleCacheSamples.add(normalized)
                junkCacheBytes += uncompressedSize
                junkCacheCount++
            } else if (lowerName.endsWith(".so")) {
                nativeSoBytes += uncompressedSize
                nativeSoCount++
                extractAbi(normalized)?.let { detectedAbis.add(it) }
            } else if (lowerName.endsWith(".dex") || lowerName.endsWith(".smali") || lowerName.endsWith(".jar")) {
                codeDexBytes += uncompressedSize
                codeDexCount++
                if (lowerName.endsWith(".dex")) dexFileCount++
            } else if (isLargeBinaryAsset(lowerName)) {
                assetModelBytes += uncompressedSize
                assetModelCount++
                if (uncompressedSize >= 10L * 1024L * 1024L && largeNoCompressAssets.size < 10) {
                    largeNoCompressAssets.add("$normalized (${ReportFormatter.formatBytes(uncompressedSize)})")
                }
            } else if (lowerName.startsWith("res/") || lowerName.contains("/src/main/res/") || lowerName == "resources.arsc") {
                resourceBytes += uncompressedSize
                resourceCount++
            } else {
                sourceConfigBytes += uncompressedSize
                sourceConfigCount++
            }

            if (lowerName.startsWith("meta-inf/") &&
                (lowerName.endsWith(".rsa") || lowerName.endsWith(".dsa") || lowerName.endsWith(".ec") || lowerName.endsWith(".sf"))
            ) {
                hasCertSignature = true
            }

            zis.closeEntry()

            val now = System.currentTimeMillis()
            if (entryCount % 120 == 0 || now - lastProgressEmit >= 200L) {
                lastProgressEmit = now
                val phase = when {
                    entryCount < 50 -> "بررسی دیوار آتش امنیتی و مسیرها"
                    elfResults.isNotEmpty() -> "ممیزی هم‌ترازی 16KB فایل‌های ELF"
                    else -> "شناسایی پشته و ساختار پروژه"
                }
                onProgress(phase, entryCount, totalCompressed, totalBytesHint)
            }
        }

        val finalCompressed = if (totalCompressed > 0L) totalCompressed else max(0L, totalBytesHint)
        val detectedStacks = fingerprintStacks(kind, pathSet, configSnippets)
        val primaryStack = detectedStacks.firstOrNull() ?: EcosystemStack.NATIVE_GRADLE

        val toolchain = extractToolchain(configSnippets, detectedStacks)
        val apkMetadata = extractApkMetadata(
            kind = kind,
            localApkFile = localApkFile,
            dexCount = dexFileCount,
            abis = detectedAbis.toList(),
            hasCert = hasCertSignature,
            toolchain = toolchain,
            configSnippets = configSnippets,
        )

        val findings = buildFirewallFindings(
            encryptedDetected = encryptedDetected,
            zipSlipSamples = zipSlipSamples,
            highRatioSamples = highRatioSamples,
            staleCacheSamples = staleCacheSamples,
            osJunkSamples = osJunkSamples,
            nestedArchiveSamples = nestedArchiveSamples,
            entryCount = entryCount,
            totalUncompressed = totalUncompressed,
            elfResults = elfResults,
            kind = kind,
            hasCertSignature = hasCertSignature,
        )

        val overallSeverity = when {
            findings.any { it.severity == Severity.CRITICAL } -> Severity.CRITICAL
            findings.any { it.severity == Severity.WARN } -> Severity.WARN
            else -> Severity.PASS
        }

        val segments = listOf(
            PayloadSegment("کتابخانه‌های بومی (.so)", nativeSoBytes, nativeSoCount, 0xFF00E5FF),
            PayloadSegment("کد / DEX / JAR", codeDexBytes, codeDexCount, 0xFFA78BFA),
            PayloadSegment("دارایی‌ها / مدل AI / مدیا", assetModelBytes, assetModelCount, 0xFFFFB300),
            PayloadSegment("منابع اندروید (res)", resourceBytes, resourceCount, 0xFF00E676),
            PayloadSegment("سورس و تنظیمات", sourceConfigBytes, sourceConfigCount, 0xFF64748B),
            PayloadSegment("فایل اضافه و کش فرسوده", junkCacheBytes, junkCacheCount, 0xFFFF3D57),
        ).filter { it.bytes > 0L || it.entryCount > 0 }

        val uncompressedGb = totalUncompressed / (1024.0 * 1024.0 * 1024.0)
        val zip32RiskSummary = when {
            totalUncompressed >= 3_800_000_000L || entryCount >= 60_000 ->
                "بحرانی: حجم غیرفشرده (${ReportFormatter.formatBytes(totalUncompressed)} و $entryCount فایل) به سقف ۴ گیگابایت / ۶۵۵۳۵ ورودی ZIP32 نزدیک یا از آن عبور کرده است."
            totalUncompressed >= 1_800_000_000L ->
                "حساس: حجم (${ReportFormatter.formatBytes(totalUncompressed)}) از مرز عملی ۱.۸ گیگابایت APK یکپارچه فراتر رفته است. خروجی AAB + Play Asset Delivery توصیه می‌شود."
            else ->
                "ایمن: حجم (${ReportFormatter.formatBytes(totalUncompressed)} و $entryCount فایل) با فاصله مطمئن زیر محدودیت ZIP32 قرار دارد."
        }

        val strategy = when {
            uncompressedGb >= 3.8 ->
                "پروژه را به Android App Bundle (.aab) همراه با Play Asset Delivery یا APK + فایل .obb تقسیم کنید و zip64 = true را فعال نمایید."
            largeNoCompressAssets.isNotEmpty() ->
                "پسوندهای مدل و مدیای حجیم را به androidResources.noCompress در app/build.gradle.kts اضافه کنید و سمبل‌های دیباگ .so را حذف نمایید."
            elfResults.any { !it.is16KbAligned } ->
                "کتابخانه‌های ۶۴ بیتی .so را با فلگ -Wl,-z,max-page-size=16384 مجدداً لینک کنید یا از NDK r28+ استفاده نمایید."
            else ->
                "مسیر بیلد استاندارد: ${primaryStack.buildSystem} همراه با ${toolchain.recommendedJdk}."
        }

        val remediation = buildRemediationSteps(
            findings = findings,
            elfResults = elfResults,
            largeAssets = largeNoCompressAssets,
            toolchain = toolchain,
        )

        return ScanReport(
            fileName = fileName,
            archiveKind = kind,
            scannedAtEpochMs = System.currentTimeMillis(),
            compressedBytes = finalCompressed,
            uncompressedBytes = totalUncompressed,
            entryCount = entryCount,
            detectedStacks = detectedStacks,
            primaryStack = primaryStack,
            overallSeverity = overallSeverity,
            firewallFindings = findings,
            payloadSegments = segments,
            elfResults = elfResults,
            apkMetadata = apkMetadata,
            toolchain = toolchain,
            largeAssetsNeedingNoCompress = largeNoCompressAssets,
            zip32RiskSummary = zip32RiskSummary,
            recommendedArtifactStrategy = strategy,
            remediationSteps = remediation,
        )
    }

    private fun queryUriMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "archive.zip"
        var size = 0L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                    name = cursor.getString(nameIdx)
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    size = cursor.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    private fun detectArchiveKind(fileName: String): ArchiveKind {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".apk") -> ArchiveKind.APK
            lower.endsWith(".aab") -> ArchiveKind.AAB
            lower.endsWith(".aar") -> ArchiveKind.AAR
            lower.endsWith(".zip") -> ArchiveKind.ZIP
            else -> ArchiveKind.UNKNOWN
        }
    }

    private fun isZipSlipPath(rawName: String): Boolean {
        val normalized = rawName.replace('\\', '/')
        return normalized.startsWith("/") ||
            normalized.contains("../") ||
            normalized == ".." ||
            Regex("^[A-Za-z]:").containsMatchIn(rawName)
    }

    private fun hasNestedArchiveExtension(lowerName: String): Boolean {
        return lowerName.endsWith(".zip") ||
            lowerName.endsWith(".7z") ||
            lowerName.endsWith(".tar.gz") ||
            lowerName.endsWith(".tgz") ||
            lowerName.endsWith(".tar.zst") ||
            lowerName.endsWith(".rar")
    }

    private fun isOsJunk(path: String): Boolean {
        val name = path.substringAfterLast('/')
        return path.contains("__MACOSX/") ||
            name == ".DS_Store" ||
            name.equals("Thumbs.db", ignoreCase = true) ||
            name.equals("desktop.ini", ignoreCase = true)
    }

    private fun isStaleForeignCache(lowerPath: String): Boolean {
        return lowerPath.endsWith("cmakecache.txt") ||
            lowerPath.contains("/.cxx/") ||
            lowerPath.startsWith(".cxx/") ||
            lowerPath.contains("/build/intermediates/") ||
            lowerPath.contains("/.gradle/configuration-cache/")
    }

    private fun isLargeBinaryAsset(lowerName: String): Boolean {
        val exts = listOf(".onnx", ".tflite", ".gguf", ".ptl", ".bin", ".pak", ".obb", ".unity3d", ".assets", ".resource", ".mp4", ".ogg", ".wav")
        return exts.any { lowerName.endsWith(it) } || lowerName.startsWith("assets/") || lowerName.contains("/assets/")
    }

    private fun isInspectableConfig(lowerName: String): Boolean {
        val file = lowerName.substringAfterLast('/')
        return file == "build.gradle" ||
            file == "build.gradle.kts" ||
            file == "settings.gradle" ||
            file == "settings.gradle.kts" ||
            file == "gradle-wrapper.properties" ||
            file == "libs.versions.toml" ||
            file == "pubspec.yaml" ||
            file == "package.json" ||
            file == "cmakelists.txt" ||
            (file == "androidmanifest.xml" && !lowerName.startsWith("androidmanifest.xml"))
    }

    private fun extractAbi(path: String): String? {
        val parts = path.split('/')
        val idx = parts.indexOfLast { it == "lib" || it == "jniLibs" }
        return if (idx >= 0 && idx + 1 < parts.size) parts[idx + 1] else null
    }

    private fun readUpTo(zis: ZipInputStream, maxBytes: Int, scratch: ByteArray): ByteArray {
        val out = ArrayList<Byte>(minOf(4096, maxBytes))
        var total = 0
        while (total < maxBytes) {
            val toRead = minOf(scratch.size, maxBytes - total)
            val read = zis.read(scratch, 0, toRead)
            if (read <= 0) break
            for (i in 0 until read) out.add(scratch[i])
            total += read
        }
        return out.toByteArray()
    }

    private fun drainEntry(zis: ZipInputStream, scratch: ByteArray): Long {
        var total = 0L
        while (true) {
            val read = zis.read(scratch, 0, scratch.size)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun fingerprintStacks(
        kind: ArchiveKind,
        paths: Set<String>,
        configs: Map<String, String>,
    ): List<EcosystemStack> {
        val stacks = LinkedHashSet<EcosystemStack>()
        val lowerPaths = paths.map { it.lowercase() }
        fun hasFile(suffix: String) = lowerPaths.any { it == suffix || it.endsWith("/$suffix") }
        fun hasDir(segment: String) = lowerPaths.any { it.contains("/$segment/") || it.startsWith("$segment/") }

        if (hasFile("apktool.yml") && hasDir("smali")) stacks.add(EcosystemStack.DECOMPILED_APKTOOL)
        if (hasFile("pubspec.yaml")) stacks.add(EcosystemStack.FLUTTER_DART)
        if (hasFile("package.json")) {
            val pkgJson = configs.entries.firstOrNull { it.key.lowercase().endsWith("package.json") }?.value.orEmpty()
            if (pkgJson.contains("react-native") || pkgJson.contains("expo")) {
                stacks.add(EcosystemStack.REACT_NATIVE_EXPO)
            }
            if (hasFile("capacitor.config.json") || hasFile("capacitor.config.ts") || hasFile("ionic.config.json") || hasFile("config.xml")) {
                stacks.add(EcosystemStack.CAPACITOR_CORDOVA)
            }
        }
        if (hasDir("unitylibrary") || hasDir("il2cppoutput")) stacks.add(EcosystemStack.UNITY_EXPORTED)
        if (hasDir("assets") && hasFile("projectsettings/projectversion.txt")) stacks.add(EcosystemStack.UNITY_RAW)
        if (lowerPaths.any { it.endsWith(".uproject") } || hasFile("project.godot") || hasFile("export_presets.cfg")) {
            stacks.add(EcosystemStack.UNREAL_GODOT_LIBGDX)
        }
        if (hasFile("cmakelists.txt") || hasFile("android.mk") || hasFile("cargo.toml") || hasFile("go.mod") || lowerPaths.any { it.endsWith(".so") }) {
            stacks.add(EcosystemStack.NDK_CPP_RUST_GO)
        }
        if (configs.values.any { it.contains("kotlin(\"multiplatform\")") || it.contains("org.jetbrains.kotlin.multiplatform") } || hasDir("composeapp")) {
            stacks.add(EcosystemStack.KOTLIN_MULTIPLATFORM)
        }
        if (lowerPaths.any { it.endsWith(".sln") || it.endsWith(".csproj") }) stacks.add(EcosystemStack.DOTNET_MAUI)
        if (hasFile("buildozer.spec") || configs.values.any { it.contains("chaquopy") }) stacks.add(EcosystemStack.PYTHON_EDGE_AI)
        if (hasFile("android.bp")) stacks.add(EcosystemStack.AOSP_KERNEL)
        if (hasFile("build.gradle") || hasFile("build.gradle.kts") || kind == ArchiveKind.APK || kind == ArchiveKind.AAB || kind == ArchiveKind.AAR) {
            stacks.add(EcosystemStack.NATIVE_GRADLE)
        } else if (hasFile("androidmanifest.xml") && hasDir("src") && !hasFile("build.gradle") && !hasFile("build.gradle.kts")) {
            stacks.add(EcosystemStack.LEGACY_ANT_ECLIPSE)
        }

        if (stacks.isEmpty()) stacks.add(EcosystemStack.NATIVE_GRADLE)
        return stacks.toList()
    }

    private fun extractToolchain(
        configs: Map<String, String>,
        stacks: List<EcosystemStack>,
    ): ToolchainRecommendation {
        val allText = configs.values.joinToString("\n")
        val agp = Regex("""com\.android\.application["']?\s*(?:version\s*)?["']([0-9]+\.[0-9]+(?:\.[0-9]+)?)""")
            .find(allText)?.groupValues?.getOrNull(1)
            ?: Regex("""agp\s*=\s*["']([0-9]+\.[0-9]+(?:\.[0-9]+)?)["']""").find(allText)?.groupValues?.getOrNull(1)
            ?: Regex("""com\.android\.tools\.build:gradle:([0-9]+\.[0-9]+(?:\.[0-9]+)?)""").find(allText)?.groupValues?.getOrNull(1)

        val gradle = Regex("""gradle-([0-9]+\.[0-9]+(?:\.[0-9]+)?)-(?:bin|all)\.zip""").find(allText)?.groupValues?.getOrNull(1)
        val kotlinVer = Regex("""kotlin\s*=\s*["']([0-9]+\.[0-9]+(?:\.[0-9]+)?)["']""").find(allText)?.groupValues?.getOrNull(1)
            ?: Regex("""kotlin-gradle-plugin:([0-9]+\.[0-9]+(?:\.[0-9]+)?)""").find(allText)?.groupValues?.getOrNull(1)

        val compileSdk = Regex("""compileSdk\s*(?:=|\s)\s*(?:release\()?([0-9]{2})""").find(allText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val targetSdk = Regex("""targetSdk\s*(?:=|\s)\s*([0-9]{2})""").find(allText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val minSdk = Regex("""minSdk\s*(?:=|\s)\s*([0-9]{2})""").find(allText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val ndkVer = Regex("""ndkVersion\s*=\s*["']([0-9]+\.[0-9]+\.[0-9]+)["']""").find(allText)?.groupValues?.getOrNull(1)

        val (jdk, jdkNotes) = ResourceCalculator.mapJdkForAgp(agp)
        val stackNote = if (stacks.contains(EcosystemStack.NDK_CPP_RUST_GO)) {
            "$jdkNotes. از نصب هم‌زمان NDK، CMake و ccache اطمینان حاصل کنید."
        } else {
            jdkNotes
        }

        return ToolchainRecommendation(
            agpVersion = agp,
            gradleVersion = gradle,
            kotlinVersion = kotlinVer,
            compileSdk = compileSdk,
            targetSdk = targetSdk,
            minSdk = minSdk,
            ndkVersion = ndkVer,
            recommendedJdk = jdk,
            compatibleJdks = jdk,
            notes = stackNote,
        )
    }

    @Suppress("DEPRECATION")
    private fun extractApkMetadata(
        kind: ArchiveKind,
        localApkFile: File?,
        dexCount: Int,
        abis: List<String>,
        hasCert: Boolean,
        toolchain: ToolchainRecommendation,
        configSnippets: Map<String, String>,
    ): ApkMetadataSummary? {
        if (kind != ArchiveKind.APK && kind != ArchiveKind.AAB) return null

        if (localApkFile != null && localApkFile.exists()) {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(localApkFile.absolutePath, PackageManager.PackageInfoFlags.of(0L))
            } else {
                pm.getPackageArchiveInfo(localApkFile.absolutePath, 0)
            }
            if (info != null) {
                val appInfo = info.applicationInfo
                val isDebug = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
                return ApkMetadataSummary(
                    packageName = info.packageName,
                    versionName = info.versionName,
                    versionCode = vCode,
                    minSdk = appInfo?.minSdkVersion,
                    targetSdk = appInfo?.targetSdkVersion,
                    isDebuggable = isDebug,
                    dexCount = dexCount,
                    nativeAbis = abis,
                    hasSigningBlockOrCert = hasCert,
                )
            }
        }

        val manifestText = configSnippets.entries.firstOrNull { it.key.endsWith("AndroidManifest.xml") }?.value
        val pkgName = manifestText?.let { Regex("""package=["']([^"']+)["']""").find(it)?.groupValues?.getOrNull(1) }

        return ApkMetadataSummary(
            packageName = pkgName,
            versionName = null,
            versionCode = null,
            minSdk = toolchain.minSdk,
            targetSdk = toolchain.targetSdk,
            isDebuggable = false,
            dexCount = dexCount,
            nativeAbis = abis,
            hasSigningBlockOrCert = hasCert,
        )
    }

    private fun buildFirewallFindings(
        encryptedDetected: Boolean,
        zipSlipSamples: List<String>,
        highRatioSamples: List<String>,
        staleCacheSamples: List<String>,
        osJunkSamples: List<String>,
        nestedArchiveSamples: List<String>,
        entryCount: Int,
        totalUncompressed: Long,
        elfResults: List<ElfAlignmentResult>,
        kind: ArchiveKind,
        hasCertSignature: Boolean,
    ): List<FirewallFinding> {
        val list = mutableListOf<FirewallFinding>()

        if (encryptedDetected) {
            list += FirewallFinding(
                id = "encrypted",
                title = "شناسایی فایل‌های رمزگذاری‌شده در آرشیو",
                detail = "یک ورودی رمزدار در ZIP مشاهده شد. بیلد خودکار بدون رمز عبور یا بازگشایی اولیه متوقف می‌شود.",
                severity = Severity.CRITICAL,
            )
        }

        if (zipSlipSamples.isNotEmpty()) {
            list += FirewallFinding(
                id = "zipslip",
                title = "مسدودسازی حمله مسیر ZipSlip",
                detail = "مسیرهای ناایمن (دارای ../ یا مسیر ریشه) شناسایی شد که هنگام اکسترکت می‌توانند از پوشه ایزوله خارج شوند.",
                severity = Severity.CRITICAL,
                affectedSamples = zipSlipSamples,
            )
        } else {
            list += FirewallFinding(
                id = "zipslip_ok",
                title = "دیوار آتش امنیت مسیر (ZipSlip)",
                detail = "تمام $entryCount فایل داخل آرشیو در مسیرهای امن و مجاز قرار دارند.",
                severity = Severity.PASS,
            )
        }

        if (highRatioSamples.isNotEmpty()) {
            list += FirewallFinding(
                id = "bomb_guard",
                title = "هشدار نسبت فشرده‌سازی شدید (بمب فشرده)",
                detail = "حداقل یک فایل ضریب بازشدن بالای ۱۰۰ برابر دارد؛ پیش از استخراج از وجود فضای دیسک کافی (>= 1.8x + 15GB) مطمئن شوید.",
                severity = Severity.WARN,
                affectedSamples = highRatioSamples,
            )
        }

        if (staleCacheSamples.isNotEmpty()) {
            list += FirewallFinding(
                id = "stale_cache",
                title = "وجود کش‌های فرسوده سیستم دیگر",
                detail = "پوشه‌های CMakeCache.txt یا .cxx و intermediates از کامپیوتر قبلی داخل زیپ مانده‌اند. پیش از بیلد این کش‌ها را پاک کنید (jniLibs/*.so را نگه دارید).",
                severity = Severity.WARN,
                affectedSamples = staleCacheSamples,
            )
        }

        if (osJunkSamples.isNotEmpty()) {
            list += FirewallFinding(
                id = "os_junk",
                title = "وجود فایل‌های زائد macOS / Windows",
                detail = "پاکسازی پوشه __MACOSX/ و فایل‌های .DS_Store و Thumbs.db پیش از بیلد توصیه می‌شود.",
                severity = Severity.WARN,
                affectedSamples = osJunkSamples,
            )
        }

        if (nestedArchiveSamples.isNotEmpty()) {
            list += FirewallFinding(
                id = "nested_archives",
                title = "آرشیوهای تو در تو یافت شد",
                detail = "درون بسته فایل‌های فشرده دیگری قرار دارند؛ استخراج بازگشتی تا عمق ۳ مرحله پیشنهاد می‌شود.",
                severity = Severity.WARN,
                affectedSamples = nestedArchiveSamples,
            )
        }

        val misalignedElf = elfResults.filter { !it.is16KbAligned }
        if (misalignedElf.isNotEmpty()) {
            list += FirewallFinding(
                id = "elf_16kb_fail",
                title = "خطای هم‌ترازی صفحه ۱۶ کیلوبایتی ELF",
                detail = "${misalignedElf.size} کتابخانه ۶۴ بیتی هم‌ترازی ۴ کیلوبایتی دارند و روی دستگاه‌های ۱۶ کیلوبایتی اندروید ۱۵ و ۱۶+ اجرا نمی‌شوند.",
                severity = Severity.CRITICAL,
                affectedSamples = misalignedElf.take(5).map { "${it.soPath} (${it.minLoadAlignBytes} B)" },
            )
        } else if (elfResults.isNotEmpty()) {
            list += FirewallFinding(
                id = "elf_16kb_ok",
                title = "تایید هم‌ترازی ۱۶ کیلوبایتی ELF",
                detail = "تمام ${elfResults.size} کتابخانه بومی .so بررسی‌شده از هم‌ترازی >= 16,384 بایت پشتیبانی می‌کنند.",
                severity = Severity.PASS,
            )
        }

        if (totalUncompressed >= 3_800_000_000L || entryCount >= 60_000) {
            list += FirewallFinding(
                id = "zip32_wall",
                title = "عبور از مرز ۴ گیگابایت / ۶۵ هزار فایل ZIP32",
                detail = "ساخت APK یکپارچه با خطا مواجه می‌شود. خروجی را به AAB + Play Asset Delivery یا APK + .obb تبدیل کنید.",
                severity = Severity.CRITICAL,
            )
        }

        if (kind == ArchiveKind.APK && !hasCertSignature) {
            list += FirewallFinding(
                id = "apk_sig_warn",
                title = "نبودن گواهی V1 در META-INF",
                detail = "فایل امضای V1 در META-INF دیده نشد (ممکن است APK با امضای مدرن V2/V3 امضا شده باشد؛ با apksigner verify تایید نهایی بگیرید).",
                severity = Severity.WARN,
            )
        }

        return list
    }

    private fun buildRemediationSteps(
        findings: List<FirewallFinding>,
        elfResults: List<ElfAlignmentResult>,
        largeAssets: List<String>,
        toolchain: ToolchainRecommendation,
    ): List<String> {
        val steps = mutableListOf<String>()
        if (findings.any { it.id == "stale_cache" }) {
            steps += "فایل‌های CMakeCache.txt، .cxx/ و build/intermediates/ را پیش از اجرای Gradle حذف کنید."
        }
        if (findings.any { it.id == "os_junk" }) {
            steps += "پوشه __MACOSX/ و فایل‌های .DS_Store را پاک کرده و پایان خطوط gradlew را به LF تبدیل کنید."
        }
        if (elfResults.any { !it.is16KbAligned }) {
            steps += "تارگت‌های ۶۴ بیتی .so را با فلگ -Wl,-z,max-page-size=16384 کامپایل و jniLibs.useLegacyPackaging = false را در app/build.gradle.kts تنظیم کنید."
        }
        if (largeAssets.isNotEmpty()) {
            steps += "پسوند فایل‌های مدیا یا مدل AI حجیم را در androidResources.noCompress واقع در app/build.gradle.kts ثبت کنید."
        }
        steps += "متغیر JAVA_HOME سیستم بیلد را پیش از اجرای تسک‌ها روی ${toolchain.recommendedJdk} تنظیم کنید."
        return steps
    }
}
