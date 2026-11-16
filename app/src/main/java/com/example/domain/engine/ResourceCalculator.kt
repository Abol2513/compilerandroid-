package com.example.domain.engine

import com.example.domain.model.BuildArchitectInput
import com.example.domain.model.BuildArchitectOutput
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object ResourceCalculator {

    fun mapJdkForAgp(agpVersionRaw: String?): Pair<String, String> {
        val cleaned = agpVersionRaw?.trim().orEmpty()
        val parts = cleaned.split('.').mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val major = parts.getOrNull(0)
        val minor = parts.getOrNull(1) ?: 0

        return when {
            major == null -> "JDK 17 یا JDK 21" to "استاندارد پیش‌فرض برای AGP 8+ و 9+"
            major <= 3 -> "JDK 8" to "نسخه AGP 3.x فقط با JDK 8 سازگار است؛ JDK بالاتر باعث خطای بایت‌کد Groovy/ASM می‌شود"
            major == 4 && minor <= 1 -> "JDK 8 (یا JDK 11)" to "بازه سازگاری استاندارد برای AGP 4.0 تا 4.1"
            major in 4..6 -> "JDK 11" to "نسخه‌های AGP 4.2 تا 6.x نیازمند JDK 11 هستند"
            major == 7 && minor < 3 -> "JDK 11" to "نسخه‌های AGP 7.0 تا 7.2 نیازمند JDK 11 هستند"
            major == 7 -> "JDK 17 (یا JDK 11)" to "نسخه‌های AGP 7.3 تا 7.4 از JDK 11 و 17 پشتیبانی می‌کنند"
            major == 8 -> "JDK 17 یا JDK 21" to "نسخه AGP 8.x حداقل نیازمند JDK 17 است"
            else -> "JDK 21 (پشتیبانی از JDK 17+)" to "زنجیره ابزار مدرن برای AGP 9.x+"
        }
    }

    fun calculate(input: BuildArchitectInput): BuildArchitectOutput {
        val requiredDisk = max(
            4.0 * input.archiveSizeGb,
            1.8 * input.uncompressedSizeGb + 15.0,
        )
        val deficit = max(0.0, requiredDisk - input.freeDiskGb)
        val isDiskSufficient = input.freeDiskGb >= requiredDisk

        val safeNativeJobs = min(
            input.cpuCores.coerceAtLeast(1),
            max(1, floor(input.hostRamGb / 2.2).toInt()),
        )
        val gradleWorkers = if (input.usesNdkCpp) {
            safeNativeJobs
        } else {
            min(input.cpuCores.coerceAtLeast(1), max(2, floor(input.hostRamGb / 1.5).toInt()))
        }

        val heapRaw = (input.hostRamGb * 0.55).roundToInt()
        val recommendedHeapGb = when {
            input.hostRamGb <= 8.0 -> heapRaw.coerceIn(2, 4)
            input.hostRamGb <= 16.0 -> heapRaw.coerceIn(4, 8)
            else -> heapRaw.coerceIn(6, 16)
        }

        val (recommendedJdk, _) = mapJdkForAgp(input.agpMajorMinor)
        val useZgc = recommendedJdk.contains("21") && recommendedHeapGb >= 8
        val gcFlag = if (useZgc) {
            "-XX:+UseZGC -XX:+ZGenerational"
        } else {
            "-XX:+UseG1GC"
        }

        val ramWarning = if (input.hostRamGb < 16.0 && (input.archiveSizeGb >= 2.0 || input.usesNdkCpp)) {
            "رم میزبان کمتر از ۱۶ گیگابایت است (${input.hostRamGb.toInt()} GB). هم‌زمانی کامپایل C++/NDK به $safeNativeJobs تسک و حافظه JVM به ${recommendedHeapGb} GB محدود شد تا سیستم دچار کرش OOM نشود."
        } else {
            null
        }

        val packagingStrategy = when {
            input.uncompressedSizeGb >= 3.8 ->
                "هشدار بحرانی سقف 4GB ZIP32: از Android App Bundle (.aab) همراه با Play Asset Delivery (بسته‌های install-time / fast-follow) یا APK + فایل توسعه .obb استفاده کنید و zip64 = true را فعال نمایید."
            input.uncompressedSizeGb >= 1.8 || input.hasMultiGbAssets ->
                "حجم بالا (>= 1.8 GB): پسوندهای سنگین را در androidResources.noCompress قرار دهید، سمبل‌های دیباگ .so را با llvm-strip حذف کنید، abiFilters را روی arm64-v8a محدود نمایید و خروجی AAB بگیرید."
            else ->
                "پیکربندی استاندارد: خروجی APK و AAB استاندارد با هم‌ترازی ۱۶ کیلوبایتی ELF و فشرده‌سازی R8."
        }

        val gradleProps = buildString {
            appendLine("# محل قرارگیری: ریشه پروژه -> ProjectRoot/gradle.properties")
            appendLine("org.gradle.jvmargs=-Xmx${recommendedHeapGb}g -XX:MaxMetaspaceSize=1g $gcFlag -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8")
            appendLine("org.gradle.parallel=true")
            appendLine("org.gradle.caching=true")
            appendLine("org.gradle.vfs.watch=false")
            appendLine("org.gradle.workers.max=$gradleWorkers")
            appendLine("android.useAndroidX=true")
            if (input.usesNdkCpp) {
                appendLine("android.ndk.suppressMinSdkVersionError=21")
            }
        }.trimEnd()

        val packagingDsl = buildString {
            appendLine("// محل قرارگیری: ماژول اپلیکیشن -> app/build.gradle.kts")
            appendLine("android {")
            appendLine("    androidResources {")
            appendLine("        noCompress += listOf(\"onnx\", \"tflite\", \"gguf\", \"ptl\", \"bin\", \"pak\", \"obb\", \"unity3d\", \"mp4\")")
            appendLine("    }")
            appendLine("    packaging {")
            appendLine("        jniLibs {")
            appendLine("            useLegacyPackaging = false // الزامی برای هم‌ترازی 16KB")
            appendLine("        }")
            appendLine("    }")
            if (input.usesNdkCpp) {
                appendLine("    defaultConfig {")
                appendLine("        externalNativeBuild {")
                appendLine("            cmake {")
                appendLine("                arguments += listOf(\"-DCMAKE_C_COMPILER_LAUNCHER=ccache\", \"-DCMAKE_CXX_COMPILER_LAUNCHER=ccache\")")
                appendLine("                cFlags += listOf(\"-Wl,-z,max-page-size=16384\")")
                appendLine("                cppFlags += listOf(\"-Wl,-z,max-page-size=16384\")")
                appendLine("            }")
                appendLine("        }")
                appendLine("    }")
            }
            append("}")
        }

        val envExports = buildString {
            appendLine("# محل قرارگیری: ترمینال لینوکس/مک یا اسکریپت CI پیش از اجرای ./gradlew")
            appendLine("export BUILD_ROOT=\"/mnt/build_volume/titan_stage\"")
            appendLine("export TMPDIR=\"\$BUILD_ROOT/tmp\"")
            appendLine("export GRADLE_USER_HOME=\"\$BUILD_ROOT/gradle_home\"")
            appendLine("export CCACHE_DIR=\"\$BUILD_ROOT/ccache\"")
            appendLine("export ANDROID_CCACHE=\"ccache\"")
            appendLine("export CMAKE_BUILD_PARALLEL_LEVEL=$safeNativeJobs")
        }.trimEnd()

        return BuildArchitectOutput(
            requiredDiskGb = requiredDisk,
            diskDeficitGb = deficit,
            isDiskSufficient = isDiskSufficient,
            safeNativeJobs = safeNativeJobs,
            gradleWorkersMax = gradleWorkers,
            recommendedHeapGb = recommendedHeapGb,
            recommendedGcFlag = gcFlag,
            recommendedJdk = recommendedJdk,
            packagingStrategy = packagingStrategy,
            ramWarning = ramWarning,
            generatedGradleProperties = gradleProps,
            generatedPackagingDsl = packagingDsl,
            generatedEnvExports = envExports,
        )
    }
}
