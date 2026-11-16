package com.example.domain.engine

import com.example.domain.model.PatchCategory
import com.example.domain.model.PatchRule

object PatchPlaybookEngine {

    val allRules: List<PatchRule> = listOf(
        PatchRule(
            id = "agp_missing_namespace",
            title = "نبودن namespace ماژول در AGP 8+ و 9+",
            category = PatchCategory.AGP_GRADLE,
            errorSignatures = listOf(
                "Namespace not specified",
                "Specify a namespace in the module's build file",
                "package attribute found in source AndroidManifest.xml",
            ),
            rootCause = "از نسخه AGP 8.0 به بعد، تعریف پکیج صرفاً در AndroidManifest.xml پشتیبانی نمی‌شود و Gradle وجود ویژگی صریح namespace در فایل بیلد ماژول را الزامی می‌داند.",
            targetFileLocation = "فایل app/build.gradle.kts (بخش android) و حذف package از AndroidManifest.xml",
            minimalPatchDiff = """
                |// مسیر: app/build.gradle.kts
                |android {
                |+   namespace = "com.example.app"
                |    compileSdk = 36
                |}
                |
                |<!-- مسیر: app/src/main/AndroidManifest.xml -->
                |- <manifest xmlns:android="..." package="com.example.app">
                |+ <manifest xmlns:android="...">
            """.trimMargin(),
            verificationHint = "پس از حذف صفت package از مانیفست، دستور gradle :app:assembleRelease را اجرا کنید.",
        ),
        PatchRule(
            id = "agp_dead_jcenter",
            title = "مخزن ازکارافتاده jcenter() یا پروتکل ناامن http://",
            category = PatchCategory.AGP_GRADLE,
            errorSignatures = listOf(
                "jcenter.bintray.com",
                "Could not find method jcenter()",
                "Using insecure protocols with repositories",
                "Received status code 403 from server: Forbidden",
            ),
            rootCause = "مخزن JCenter به‌طور کامل خاموش شده است و نسخه‌های جدید Gradle اتصال‌های ناامن http:// را به‌صورت پیش‌فرض مسدود می‌کنند.",
            targetFileLocation = "فایل settings.gradle.kts یا build.gradle.kts در ریشه پروژه (بخش repositories)",
            minimalPatchDiff = """
                |// مسیر: settings.gradle.kts یا build.gradle.kts
                |repositories {
                |-   jcenter()
                |-   maven { url = uri("http://jitpack.io") }
                |+   google()
                |+   mavenCentral()
                |+   maven { url = uri("https://jitpack.io") }
                |}
            """.trimMargin(),
            verificationHint = "دریافت کتابخانه‌ها را با دستور gradle :app:dependencies --refresh-dependencies بررسی کنید.",
        ),
        PatchRule(
            id = "agp_missing_buildconfig",
            title = "خطای Unresolved reference: BuildConfig",
            category = PatchCategory.AGP_GRADLE,
            errorSignatures = listOf(
                "Unresolved reference: BuildConfig",
                "cannot find symbol class BuildConfig",
                "BuildConfig generation is disabled",
            ),
            rootCause = "برای افزایش سرعت بیلد در AGP 8+، تولید کلاس BuildConfig به‌صورت پیش‌فرض غیرفعال است مگر اینکه در ماژول فعال شود.",
            targetFileLocation = "فایل build.gradle.kts ماژول موردنظر (داخل بلوک android)",
            minimalPatchDiff = """
                |// مسیر: app/build.gradle.kts
                |android {
                |+   buildFeatures {
                |+       buildConfig = true
                |+   }
                |}
            """.trimMargin(),
            verificationHint = "پروژه را Sync و مجدداً بیلد کنید تا کلاس BuildConfig ساخته شود.",
        ),
        PatchRule(
            id = "manifest_exported_api31",
            title = "نبودن android:exported در ادغام مانیفست (API 31 تا 36+)",
            category = PatchCategory.MANIFEST_API36,
            errorSignatures = listOf(
                "android:exported needs to be explicitly specified",
                "Apps targeting Android 12 and higher are required to specify",
                "Manifest merger failed",
            ),
            rootCause = "هر کامپوننتی (Activity، Service، Receiver) که دارای <intent-filter> است در اندروید ۱۲ و بالاتر باید صراحتاً مقدار android:exported را مشخص کند.",
            targetFileLocation = "فایل app/src/main/AndroidManifest.xml (تگ activity/service/receiver)",
            minimalPatchDiff = """
                |<!-- مسیر: app/src/main/AndroidManifest.xml -->
                |<activity
                |    android:name=".MainActivity"
                |+   android:exported="true">
                |    <intent-filter>
                |        <action android:name="android.intent.action.MAIN" />
                |        <category android:name="android.intent.category.LAUNCHER" />
                |    </intent-filter>
                |</activity>
            """.trimMargin(),
            verificationHint = "خروجی نهایی ادغام مانیفست را با gradle :app:processReleaseManifest چک کنید.",
        ),
        PatchRule(
            id = "androidx_jetifier",
            title = "تداخل کتابخانه قدیمی android.support.* با AndroidX",
            category = PatchCategory.AGP_GRADLE,
            errorSignatures = listOf(
                "Duplicate class android.support.",
                "package android.support.v4 does not exist",
                "AndroidX dependency without android.useAndroidX",
            ),
            rootCause = "وابستگی‌های قدیمی کامپایل‌شده با Support Library بدون فعال بودن Jetifier با پکیج‌های مدرن AndroidX تداخل پیدا می‌کنند.",
            targetFileLocation = "فایل gradle.properties در ریشه پروژه",
            minimalPatchDiff = """
                |# مسیر: ProjectRoot/gradle.properties
                |+android.useAndroidX=true
                |+android.enableJetifier=true
            """.trimMargin(),
            verificationHint = "در صورت امکان، کتابخانه‌های قدیمی را به معادل AndroidX ارتقا دهید.",
        ),
        PatchRule(
            id = "ndk_16kb_alignment",
            title = "ناهم‌ترازی صفحه حافظه ۱۶ کیلوبایتی ELF در اندروید ۱۵ و ۱۶+",
            category = PatchCategory.NDK_16KB,
            errorSignatures = listOf(
                "does not support 16 KB",
                "ELF alignment",
                "p_align",
                "LOAD segment not aligned at 16384",
            ),
            rootCause = "کتابخانه‌های ۶۴ بیتی .so که با NDK قدیمی‌تر از r28 کامپایل شده‌اند هم‌ترازی پیش‌فرض 4KB دارند و در دستگاه‌های 16KB اندروید ۱۵/۱۶ اجرا نمی‌شوند.",
            targetFileLocation = "فایل CMakeLists.txt و بخش packaging در app/build.gradle.kts",
            minimalPatchDiff = """
                |# مسیر: CMakeLists.txt
                |+target_link_options(native_lib PRIVATE "-Wl,-z,max-page-size=16384")
                |
                |// مسیر: app/build.gradle.kts
                |android {
                |    packaging {
                |        jniLibs {
                |+           useLegacyPackaging = false
                |        }
                |    }
                |}
            """.trimMargin(),
            verificationHint = "فایل APK نهایی را دوباره با اسکنر همین برنامه بررسی کنید تا نشان 16KB تایید شود.",
        ),
        PatchRule(
            id = "kotlin_compose_ksp_mismatch",
            title = "عدم تطابق نسخه کاتلین ۲.x با کامپایلر Compose یا KSP",
            category = PatchCategory.KOTLIN_KSP,
            errorSignatures = listOf(
                "Compose Compiler requires Kotlin version",
                "KSP version is not compatible with Kotlin",
                "Starting in Kotlin 2.0, the Compose Compiler Gradle plugin",
            ),
            rootCause = "از کاتلین ۲.۰ به بعد، پلاگین کامپایلر Compose به پلاگین رسمی kotlin.plugin.compose منتقل شده و پیش‌وند نسخه KSP باید دقیقاً با نسخه کاتلین هم‌تراز باشد.",
            targetFileLocation = "فایل gradle/libs.versions.toml و بخش plugins در app/build.gradle.kts",
            minimalPatchDiff = """
                |// مسیر: gradle/libs.versions.toml
                |[plugins]
                |+kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
                |
                |// مسیر: app/build.gradle.kts
                |plugins {
                |+   alias(libs.plugins.kotlin.compose)
                |}
            """.trimMargin(),
            verificationHint = "اطمینان حاصل کنید پیش‌وند نسخه KSP با نسخه Kotlin در libs.versions.toml هماهنگ است.",
        ),
        PatchRule(
            id = "zip32_4gb_wall",
            title = "عبور از مرز ۴ گیگابایت ZIP32 یا خطای حافظه AAPT2 در فایل‌های سنگین",
            category = PatchCategory.PACKAGING_ZIP32,
            errorSignatures = listOf(
                "Zip64",
                "archive contains more than 65535 entries",
                "INSTALL_FAILED_INVALID_APK",
                "aapt2",
                "OutOfMemoryError",
            ),
            rootCause = "فرمت استاندارد ZIP32 دارای سقف قطعی ۴ گیگابایت و ۶۵۵۳۵ فایل است؛ همچنین فشرده‌سازی مجدد مدل‌های هوش مصنوعی یا فایل‌های بازی باعث هنگ و پر شدن رم در aapt2 می‌شود.",
            targetFileLocation = "فایل app/build.gradle.kts (بخش androidResources) و ساختار ماژول‌های Asset Pack",
            minimalPatchDiff = """
                |// مسیر: app/build.gradle.kts
                |android {
                |    androidResources {
                |+       noCompress += listOf("onnx", "tflite", "gguf", "pak", "obb", "bin")
                |    }
                |}
                |
                |// برای حجم بالای 2GB: پروژه را به AAB + Play Asset Delivery یا APK + .obb بشکنید.
                |// در تسک‌های Zip گرادل مقدار zip64 = true را فعال کنید.
            """.trimMargin(),
            verificationHint = "سمبل‌های دیباگ فایل‌های .so را حذف کنید و حجم APK یکپارچه را زیر 1.8 GB نگه دارید.",
        ),
    )

    fun diagnoseLog(logText: String): List<PatchRule> {
        val trimmed = logText.trim()
        if (trimmed.isEmpty()) return emptyList()
        val lower = trimmed.lowercase()

        return allRules
            .map { rule ->
                val score = rule.errorSignatures.count { sig -> lower.contains(sig.lowercase()) }
                rule to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
