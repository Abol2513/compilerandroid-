package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.BuildArchitectInput
import com.example.domain.model.BuildArchitectOutput
import com.example.domain.model.Severity
import com.example.ui.components.CapacityArcGauge
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.ConsoleCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelemetryMetricTile
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonCrit
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BuildArchitectScreen(
    input: BuildArchitectInput,
    output: BuildArchitectOutput,
    hasLastScan: Boolean,
    onUpdateInput: ((BuildArchitectInput) -> BuildArchitectInput) -> Unit,
    onApplyPreset: (String) -> Unit,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ConsoleCard(glowAccent = ElectricCyan) {
                    SectionHeader(
                        tag = "مهندسی منابع و حافظه 10GB+",
                        title = "تنظیم سخت‌افزار و حجم پروژه",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PresetChip("همین دستگاه", "preset_device") { onApplyPreset("DEVICE") }
                        PresetChip("سرور CI ۱۶ گیگ", "preset_ci") { onApplyPreset("CI_16GB") }
                        PresetChip("پروژه غول ۱۰ گیگ", "preset_mega") { onApplyPreset("MEGA_10GB") }
                        if (hasLastScan) {
                            PresetChip("بارگذاری از آخرین اسکن", "preset_last_scan") { onApplyPreset("FROM_SCAN") }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    ParameterSlider(
                        label = "حجم آرشیو فشرده (S)",
                        valueText = String.format(Locale.US, "%.1f GB", input.archiveSizeGb),
                        value = input.archiveSizeGb.toFloat(),
                        range = 0.2f..15f,
                        testTag = "slider_archive_gb",
                        onValueChange = { v ->
                            onUpdateInput { it.copy(archiveSizeGb = v.toDouble(), uncompressedSizeGb = maxOf(it.uncompressedSizeGb, v.toDouble())) }
                        },
                    )
                    ParameterSlider(
                        label = "حجم بازشده و خام (U)",
                        valueText = String.format(Locale.US, "%.1f GB", input.uncompressedSizeGb),
                        value = input.uncompressedSizeGb.toFloat(),
                        range = 0.5f..30f,
                        testTag = "slider_uncompressed_gb",
                        onValueChange = { v ->
                            onUpdateInput { it.copy(uncompressedSizeGb = v.toDouble()) }
                        },
                    )
                    ParameterSlider(
                        label = "رم فیزیکی میزبان (RAM)",
                        valueText = String.format(Locale.US, "%.0f GB", input.hostRamGb),
                        value = input.hostRamGb.toFloat(),
                        range = 4f..64f,
                        testTag = "slider_ram_gb",
                        onValueChange = { v ->
                            onUpdateInput { it.copy(hostRamGb = v.roundToInt().toDouble()) }
                        },
                    )
                    ParameterSlider(
                        label = "فضای آزاد دیسک (Persistent)",
                        valueText = String.format(Locale.US, "%.0f GB", input.freeDiskGb),
                        value = input.freeDiskGb.toFloat(),
                        range = 5f..150f,
                        testTag = "slider_disk_gb",
                        onValueChange = { v ->
                            onUpdateInput { it.copy(freeDiskGb = v.roundToInt().toDouble()) }
                        },
                    )
                    ParameterSlider(
                        label = "تعداد هسته‌های CPU",
                        valueText = "${input.cpuCores} هسته",
                        value = input.cpuCores.toFloat(),
                        range = 2f..32f,
                        testTag = "slider_cpu_cores",
                        onValueChange = { v ->
                            onUpdateInput { it.copy(cpuCores = v.roundToInt().coerceAtLeast(1)) }
                        },
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "نسخه هدف AGP",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("4.1", "7.4", "8.7", "9.1").forEach { agp ->
                            val selected = input.agpMajorMinor.startsWith(agp)
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateInput { it.copy(agpMajorMinor = agp) } },
                                label = { Text("AGP $agp", style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanMuted,
                                    selectedLabelColor = ElectricCyan,
                                    containerColor = SteelElevated,
                                    labelColor = TextSecondary,
                                ),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else HairlineBorder),
                                modifier = Modifier.testTag("agp_chip_$agp"),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "دارای کد بومی NDK / C++ / IL2CPP",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Switch(
                            checked = input.usesNdkCpp,
                            onCheckedChange = { checked -> onUpdateInput { it.copy(usesNdkCpp = checked) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricCyan, checkedThumbColor = ObsidianBg),
                            modifier = Modifier.testTag("switch_ndk_cpp"),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "دارای مدل‌های AI یا فایل‌های بازی حجیم",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Switch(
                            checked = input.hasMultiGbAssets,
                            onCheckedChange = { checked -> onUpdateInput { it.copy(hasMultiGbAssets = checked) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = ElectricCyan, checkedThumbColor = ObsidianBg),
                            modifier = Modifier.testTag("switch_multigb_assets"),
                        )
                    }
                }
            }

            item {
                ConsoleCard(
                    borderColor = if (output.isDiskSufficient) ElectricCyan.copy(alpha = 0.45f) else CrimsonCrit.copy(alpha = 0.75f),
                    glowAccent = if (output.isDiskSufficient) EmeraldPass else CrimsonCrit,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionHeader(
                            tag = "بودجه محاسبه‌شده ایمن",
                            title = "گیج آمادگی سیستم",
                            modifier = Modifier.weight(1f),
                        )
                        StatusBadge(
                            severity = if (output.isDiskSufficient) Severity.PASS else Severity.CRITICAL,
                            customLabel = if (output.isDiskSufficient) "دیسک کافی" else "کمبود فضا",
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val diskRatio = (input.freeDiskGb / output.requiredDiskGb.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                        CapacityArcGauge(
                            label = "پوشش دیسک",
                            ratio = diskRatio,
                            centerText = "${(diskRatio * 100).roundToInt()}%",
                            subText = String.format(Locale.US, "نیاز %.1f GB", output.requiredDiskGb),
                            accentColor = if (output.isDiskSufficient) EmeraldPass else CrimsonCrit,
                            modifier = Modifier.weight(1f),
                        )
                        val jobRatio = (output.safeNativeJobs.toFloat() / input.cpuCores.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        CapacityArcGauge(
                            label = "هم‌زمانی ایمن C++",
                            ratio = jobRatio,
                            centerText = "${output.safeNativeJobs}/${input.cpuCores}",
                            subText = "ورکر گرادل: ${output.gradleWorkersMax}",
                            accentColor = ElectricCyan,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TelemetryMetricTile(
                            label = "رم JVM / GC",
                            value = "-Xmx${output.recommendedHeapGb}g",
                            subValue = output.recommendedGcFlag,
                            accentColor = AmberWarn,
                            modifier = Modifier.weight(1f),
                        )
                        TelemetryMetricTile(
                            label = "JDK موردنیاز",
                            value = output.recommendedJdk,
                            subValue = "AGP ${input.agpMajorMinor}",
                            accentColor = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (!output.isDiskSufficient) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = String.format(Locale.US, "کمبود فضای دیسک: %.1f GB دیگر فضا آزاد کنید.", output.diskDeficitGb),
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonCrit,
                        )
                    }
                    if (output.ramWarning != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = output.ramWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarn,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = output.packagingStrategy,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            item {
                CodeBlockCard(
                    title = "gradle.properties",
                    code = output.generatedGradleProperties,
                    copyTestTag = "copy_gradle_props_button",
                    placementHint = "کجا بگذارم؟ در فایل gradle.properties در ریشه پروژه (کنار settings.gradle.kts)",
                    onCopied = onCopied,
                )
            }

            item {
                CodeBlockCard(
                    title = "app/build.gradle.kts",
                    code = output.generatedPackagingDsl,
                    copyTestTag = "copy_packaging_dsl_button",
                    placementHint = "کجا بگذارم؟ داخل بلوک android { ... } در فایل app/build.gradle.kts",
                    onCopied = onCopied,
                )
            }

            item {
                CodeBlockCard(
                    title = "export_env_ci.sh",
                    code = output.generatedEnvExports,
                    copyTestTag = "copy_env_exports_button",
                    placementHint = "کجا بزنم؟ در ترمینال لینوکس/مک یا اسکریپت CI پیش از اجرای Gradle (نه داخل فایل پروژه)",
                    onCopied = onCopied,
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SteelElevated,
            labelColor = ElectricCyan,
        ),
        border = BorderStroke(1.dp, HairlineBorder),
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun ParameterSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    testTag: String,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = ElectricCyan,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = SteelElevated,
            ),
            modifier = Modifier.testTag(testTag),
        )
    }
}
