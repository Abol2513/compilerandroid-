package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.ConsoleCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent

private enum class GuideChapter(val label: String) {
    ALL("همه فصل‌ها"),
    WORKFLOW("شیوه کار با برنامه"),
    PLACEMENT("کجا چی بگذارم؟"),
    MEGA_SCALE("پروژه‌های 10GB+ و 4GB"),
    TOOLCHAIN_16KB("جدول JDK و 16KB"),
}

private data class PlacementGuideItem(
    val id: String,
    val chapter: GuideChapter,
    val badge: String,
    val title: String,
    val exactPath: String,
    val summary: String,
    val steps: List<String>,
    val snippetTitle: String? = null,
    val snippetCode: String? = null,
)

private val guideItems = listOf(
    PlacementGuideItem(
        id = "workflow_overview",
        chapter = GuideChapter.WORKFLOW,
        badge = "گام ۱ تا ۴",
        title = "روش کار کامل با تیتان‌دروید آرشیتکت",
        exactPath = "تب تحلیلگر -> انتخاب فایل یا بازرسی خودکار",
        summary = "این برنامه بدون نیاز به دسترسی خطرناک حافظه، آرشیوهای چند گیگابایتی را به صورت جریانی (Streaming) می‌خواند و نقشه بیلد و پچ‌ها را می‌سازد.",
        steps = listOf(
            "۱. در تب «تحلیلگر» دکمه «انتخاب فایل» را بزنید و یک فایل .zip (سورس پروژه)، .apk، .aab یا .aar را انتخاب کنید. برای تست فوری، دکمه «خودآزمایی APK» همین برنامه را اسکن می‌کند.",
            "۲. گزارش بازشده شامل ۴ بخش است: «نمای کلی»، «دیوار آتش امنیتی» (تشخیص ZipSlip و کش‌های خراب)، «زنجیره ابزار» (JDK دقیق) و «حجم و 16KB ELF».",
            "۳. در گزارش، دکمه «ارسال به معمار بیلد» را بزنید تا حجم پروژه و نسخه AGP مستقیماً به تب «معمار بیلد» منتقل شود و فایل‌های تنظیمات آماده کپی تولید شوند.",
            "۴. اگر خطای Gradle یا CMake دارید، متن خطا را در تب «پچ‌یاب» پیست کنید تا پچ دقیق و مسیر فایل اصلاحی نمایش داده شود.",
        ),
    ),
    PlacementGuideItem(
        id = "place_gradle_props",
        chapter = GuideChapter.PLACEMENT,
        badge = "محل فایل ۱",
        title = "کدهای حافظه JVM و موازی‌سازی (gradle.properties) را کجا بگذارم؟",
        exactPath = "ProjectRoot/gradle.properties  (پوشه اصلی ریشه پروژه، کنار settings.gradle.kts)",
        summary = "خروجی بخش «gradle.properties» در تب معمار بیلد، مصرف رم JVM و تعداد ورکرهای گرادل را کنترل می‌کند تا بیلدهای سنگین با خطای OutOfMemoryError متوقف نشوند.",
        steps = listOf(
            "فایل gradle.properties را در پوشه ریشه پروژه اندرویدی (کنار build.gradle.kts سطح بالا) باز کنید.",
            "اگر خط org.gradle.jvmargs از قبل وجود دارد، آن را حذف و با خط تولیدشده توسط معمار بیلد جایگزین کنید.",
            "خط org.gradle.vfs.watch=false برای پروژه‌های پرحجم ضروری است تا سیستم‌عامل لینوکس با خطای inotify قفل نکند.",
        ),
        snippetTitle = "ProjectRoot/gradle.properties",
        snippetCode = """
            org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -Dfile.encoding=UTF-8
            org.gradle.parallel=true
            org.gradle.caching=true
            org.gradle.vfs.watch=false
            org.gradle.workers.max=4
            android.useAndroidX=true
        """.trimIndent(),
    ),
    PlacementGuideItem(
        id = "place_app_gradle",
        chapter = GuideChapter.PLACEMENT,
        badge = "محل فایل ۲",
        title = "کدهای noCompress، جادوی 16KB و namespace را کجا بگذارم؟",
        exactPath = "ProjectRoot/app/build.gradle.kts  (داخل ماژول app، بلوک android { ... })",
        summary = "تمام تنظیمات بسته‌بندی فایل‌های حجیم AI/بازی، هم‌ترازی کتابخانه‌های بومی .so و فلگ‌های کامپایلر CMake داخل build.gradle.kts ماژول اصلی قرار می‌گیرند.",
        steps = listOf(
            "فایل app/build.gradle.kts (یا app/build.gradle) را باز کنید.",
            "بلوک androidResources { noCompress += ... } و packaging { jniLibs { useLegacyPackaging = false } } را دقیقاً داخل آکولاد android { ... } قرار دهید.",
            "اگر پروژه C++/NDK دارد، بلوک externalNativeBuild { cmake { ... } } را داخل defaultConfig { ... } قرار دهید.",
            "اگر خطای Namespace not specified داشتید، خط namespace = \"com.your.package\" را در ابتدای android { ... } بگذارید.",
        ),
        snippetTitle = "app/build.gradle.kts",
        snippetCode = """
            android {
                namespace = "com.your.package"
                androidResources {
                    noCompress += listOf("onnx", "tflite", "gguf", "pak", "obb", "bin")
                }
                packaging {
                    jniLibs { useLegacyPackaging = false }
                }
            }
        """.trimIndent(),
    ),
    PlacementGuideItem(
        id = "place_settings_gradle",
        chapter = GuideChapter.PLACEMENT,
        badge = "محل فایل ۳",
        title = "مخازن google() و mavenCentral() را کجا بگذارم؟",
        exactPath = "ProjectRoot/settings.gradle.kts  (بخش pluginManagement و dependencyResolutionManagement)",
        summary = "در پروژه‌های قدیمی که هنوز jcenter() یا آدرس‌های http:// دارند، اصلاح مخازن باید در settings.gradle.kts انجام شود.",
        steps = listOf(
            "فایل settings.gradle.kts را در ریشه پروژه باز کنید.",
            "هر جا jcenter() دیدید آن را پاک کنید و مطمئن شوید google() و mavenCentral() با پروتکل امن HTTPS وجود دارند.",
            "در پروژه‌های بسیار قدیمی که مخزن در build.gradle ریشه بود، بخش allprojects { repositories { ... } } را اصلاح کنید.",
        ),
        snippetTitle = "settings.gradle.kts",
        snippetCode = """
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
        """.trimIndent(),
    ),
    PlacementGuideItem(
        id = "place_manifest",
        chapter = GuideChapter.PLACEMENT,
        badge = "محل فایل ۴",
        title = "اصلاح android:exported و حذف package تکراری کجا انجام می‌شود؟",
        exactPath = "ProjectRoot/app/src/main/AndroidManifest.xml",
        summary = "برای بیلد روی اندروید ۱۲ تا ۱۶+ (API 31 تا 36)، هر اکتیویتی یا سرویسی که intent-filter دارد باید صراحتاً android:exported داشته باشد.",
        steps = listOf(
            "فایل app/src/main/AndroidManifest.xml را باز کنید.",
            "اگر تگ <manifest> در خط اول دارای package=\"...\" است و AGP 8+ خطا داد، package را از مانیفست پاک کنید (چون قبلاً در build.gradle.kts به عنوان namespace ثبت شده).",
            "برای اکتیویتی اصلی لانچر android:exported=\"true\" و برای سرویس‌ها/گیرنده‌های داخلی android:exported=\"false\" بنویسید.",
        ),
    ),
    PlacementGuideItem(
        id = "place_env_ci",
        chapter = GuideChapter.PLACEMENT,
        badge = "محل فایل ۵",
        title = "دستورهای export کش و دیسک (TMPDIR / GRADLE_USER_HOME) را کجا بزنم؟",
        exactPath = "ترمینال لینوکس/مک سرور بیلد یا فایل تنظیمات CI/CD (مثل GitHub Actions / GitLab CI)",
        summary = "این دستورها کد کاتلین نیستند؛ متغیرهای محیطی سیستم‌عامل هستند تا هنگام اکسترکت پروژه‌های ۱۰ گیگابایتی، پوشه موقت رم (/tmp) پر نشود.",
        steps = listOf(
            "این خطوط را داخل فایل‌های Gradle نگذارید!",
            "در سرور بیلد یا ترمینال لینوکس، پیش از اجرای دستور gradle assembleRelease یا ./gradlew، دستورات export را کپی و اجرا کنید.",
            "مسیر /mnt/build_volume را به نام درایو بزرگ و دائمی سرور خود تغییر دهید.",
        ),
    ),
    PlacementGuideItem(
        id = "mega_tree_map",
        chapter = GuideChapter.PLACEMENT,
        badge = "نقشه درختی",
        title = "نقشه کامل پوشه‌های یک پروژه استاندارد اندروید",
        exactPath = "ProjectRoot/",
        summary = "برای اینکه هیچ وقت محل قرارگیری فایل‌ها را اشتباه نگیرید، ساختار استاندارد پوشه‌ها همیشه به شکل زیر است:",
        steps = listOf(
            "پوشه ریشه (ProjectRoot) شامل gradle.properties، settings.gradle.kts و build.gradle.kts اصلی است.",
            "پوشه gradle/ حاوی libs.versions.toml (نسخه کتابخانه‌ها و کاتلین/KSP) و wrapper است.",
            "پوشه app/ حاوی app/build.gradle.kts، پوشه src/main/AndroidManifest.xml و کدهای cpp (CMakeLists.txt) است.",
        ),
        snippetTitle = "ساختار درختی استاندارد پروژه",
        snippetCode = """
            MyAndroidProject/
            ├── gradle.properties            <-- حافظه JVM و Workers اینجاست
            ├── settings.gradle.kts          <-- مخازن google() / mavenCentral() اینجاست
            ├── build.gradle.kts             <-- پلاگین‌های سطح کل پروژه
            ├── gradle/
            │   └── libs.versions.toml       <-- هماهنگی نسخه Kotlin / KSP / AGP
            └── app/
                ├── build.gradle.kts         <-- namespace، noCompress، 16KB و packaging
                └── src/main/
                    ├── AndroidManifest.xml  <-- android:exported و مجوزها
                    ├── cpp/CMakeLists.txt   <-- فلگ‌های کامپایلر C++ و 16KB Linker
                    └── jniLibs/arm64-v8a/   <-- فایل‌های .so بررسی‌شده در اسکنر
        """.trimIndent(),
    ),
    PlacementGuideItem(
        id = "mega_10gb_rules",
        chapter = GuideChapter.MEGA_SCALE,
        badge = "فرمول ۱۰ گیگ",
        title = "قانون فضای دیسک ۴ برابری و هم‌زمانی امن کامپایل C++",
        exactPath = "تب معمار بیلد -> اسلایدرهای دیسک و رم",
        summary = "یک فایل ZIP ۱۰ گیگابایتی هنگام باز شدن + فایل‌های میانی NDK + DEX حداقل ۴۰ تا ۶۰ گیگابایت فضا می‌خواهد.",
        steps = listOf(
            "فرمول فضای آزاد دیسک: max(4 × حجم آرشیو، 1.8 × حجم غیرفشرده + 15 GB).",
            "فرمول تردهای C++ (J_native): هر هسته clang++ حدود ۲.۲ گیگابایت رم مصرف می‌کند؛ بنابراین حتی روی CPU ۳۲ هسته‌ای با ۱۶ گیگ رم، نباید بیش از ۷ کار هم‌زمان اجرا کرد.",
            "اگر فایل‌های حجیم بالای ۳.۸ گیگابایت دارید، فرمت ZIP32 از کار می‌افتد؛ حتماً خروجی را به AAB + Play Asset Delivery یا APK + .obb تقسیم کنید.",
        ),
    ),
    PlacementGuideItem(
        id = "toolchain_jdk_16kb",
        chapter = GuideChapter.TOOLCHAIN_16KB,
        badge = "ماتریس JDK",
        title = "جدول تطابق AGP با JDK و تست هم‌ترازی 16KB",
        exactPath = "گزارش اسکن -> تب زنجیره ابزار و تب Payload & 16KB",
        summary = "بسیاری از خطاهای مرموز گرادل فقط به خاطر انتخاب JDK اشتباه (مثلاً JDK 21 برای پروژه قدیمی AGP 4) رخ می‌دهند.",
        steps = listOf(
            "AGP 3.x تا 4.1 -> حتماً JDK 8 (یا حداکثر 11 برای 4.1).",
            "AGP 4.2 تا 7.2 -> نیازمند JDK 11.",
            "AGP 7.3 تا 8.x -> نیازمند JDK 17 (یا JDK 21 برای AGP 8.x جدید).",
            "AGP 9.x+ -> بهینه برای JDK 21 (با پشتیبانی از JDK 17+).",
            "هم‌ترازی 16KB: تمام فایل‌های .so داخل lib/arm64-v8a/ باید در سگمنت PT_LOAD هم‌ترازی >= 16384 بایت داشته باشند.",
        ),
    ),
)

@Composable
fun UserGuideScreen(
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedChapter by rememberSaveable { mutableStateOf(GuideChapter.ALL) }
    var expandedId by rememberSaveable { mutableStateOf("workflow_overview") }

    val visibleItems = if (selectedChapter == GuideChapter.ALL) {
        guideItems
    } else {
        guideItems.filter { it.chapter == selectedChapter }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConsoleCard(
                    borderColor = ElectricCyan.copy(alpha = 0.45f),
                    glowAccent = ElectricCyan,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyanMuted)
                                .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "دفترچه راهنما",
                                tint = ElectricCyan,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "دفترچه راهنمای مهندسی و محل فایل‌ها",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                            )
                            Text(
                                text = "راهنمای قدم‌به‌قدم کار با اسکنر و آدرس دقیق محل قرارگیری هر کد",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideChapter.entries.forEach { chapter ->
                        val selected = selectedChapter == chapter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedChapter = chapter },
                            label = {
                                Text(
                                    text = chapter.label,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanMuted,
                                selectedLabelColor = ElectricCyan,
                                containerColor = SteelElevated,
                                labelColor = TextSecondary,
                            ),
                            border = BorderStroke(1.dp, if (selected) ElectricCyan else HairlineBorder),
                            modifier = Modifier.testTag("guide_chapter_${chapter.name.lowercase()}"),
                        )
                    }
                }
            }

            items(visibleItems, key = { it.id }) { item ->
                val isExpanded = expandedId == item.id
                GuideItemCard(
                    item = item,
                    expanded = isExpanded,
                    onToggle = { expandedId = if (isExpanded) "" else item.id },
                    onCopied = onCopied,
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun GuideItemCard(
    item: PlacementGuideItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopied: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onToggle)
            .testTag("guide_card_${item.id}"),
        shape = MaterialTheme.shapes.large,
        color = GunmetalSurface,
        border = BorderStroke(1.dp, if (expanded) ElectricCyan.copy(alpha = 0.6f) else HairlineBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (item.chapter) {
                                    GuideChapter.PLACEMENT -> CyanMuted
                                    GuideChapter.MEGA_SCALE -> AmberWarn.copy(alpha = 0.18f)
                                    GuideChapter.TOOLCHAIN_16KB -> VioletAccent.copy(alpha = 0.18f)
                                    else -> EmeraldPass.copy(alpha = 0.16f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (item.chapter) {
                                GuideChapter.PLACEMENT -> ElectricCyan
                                GuideChapter.MEGA_SCALE -> AmberWarn
                                GuideChapter.TOOLCHAIN_16KB -> VioletAccent
                                else -> EmeraldPass
                            },
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "بستن" else "باز کردن",
                    tint = ElectricCyan,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ObsidianBg)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = item.exactPath,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = ElectricCyan,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = item.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    item.steps.forEachIndexed { idx, step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(SteelElevated)
                                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = ElectricCyan,
                                )
                            }
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (item.snippetCode != null && item.snippetTitle != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        CodeBlockCard(
                            title = item.snippetTitle,
                            code = item.snippetCode,
                            copyTestTag = "copy_guide_${item.id}",
                            placementHint = "محل قرارگیری: ${item.exactPath}",
                            onCopied = onCopied,
                        )
                    }
                }
            }
        }
    }
}
