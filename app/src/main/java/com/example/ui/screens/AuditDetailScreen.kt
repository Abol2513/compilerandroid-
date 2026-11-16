package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ReportFormatter
import com.example.domain.model.ElfAlignmentResult
import com.example.domain.model.FirewallFinding
import com.example.domain.model.ScanReport
import com.example.domain.model.Severity
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.ConsoleCard
import com.example.ui.components.PayloadDistributionBar
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

private enum class DetailSection(val label: String) {
    OVERVIEW("نمای کلی"),
    FIREWALL("دیوار آتش"),
    TOOLCHAIN("زنجیره ابزار و JDK"),
    PAYLOAD_ELF("حجم و ELF 16KB"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditDetailScreen(
    report: ScanReport,
    onBack: () -> Unit,
    onSendToArchitect: () -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedSection by rememberSaveable { mutableStateOf(DetailSection.OVERVIEW) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg,
                    titleContentColor = TextPrimary,
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("audit_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت به تحلیلگر",
                            tint = TextPrimary,
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = report.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = report.archiveKind.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "گزارش تیتان‌دروید: ${report.fileName}")
                                putExtra(Intent.EXTRA_TEXT, ReportFormatter.toMarkdown(report))
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری گزارش ممیزی"))
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("share_report_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "اشتراک‌گذاری گزارش Markdown",
                            tint = ElectricCyan,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DetailSection.entries.forEach { section ->
                            val selected = selectedSection == section
                            FilterChip(
                                selected = selected,
                                onClick = { selectedSection = section },
                                label = {
                                    Text(
                                        text = section.label,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanMuted,
                                    selectedLabelColor = ElectricCyan,
                                    containerColor = SteelElevated,
                                    labelColor = TextSecondary,
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ElectricCyan else HairlineBorder,
                                ),
                                modifier = Modifier.testTag("detail_tab_${section.name.lowercase()}"),
                            )
                        }
                    }
                }

                when (selectedSection) {
                    DetailSection.OVERVIEW -> {
                        item { OverviewSummaryCard(report = report, onSendToArchitect = onSendToArchitect) }
                        if (report.apkMetadata != null) {
                            item { ApkMetadataCard(report = report) }
                        }
                        item { RemediationChecklistCard(report = report) }
                        item {
                            CodeBlockCard(
                                title = "خروجی تله‌متری JSON",
                                code = ReportFormatter.toJson(report),
                                copyTestTag = "copy_json_report_button",
                                placementHint = "برای ارسال به سیستم‌های CI یا بایگانی گزارش ممیزی کپی کنید",
                                onCopied = onNotify,
                            )
                        }
                    }

                    DetailSection.FIREWALL -> {
                        items(report.firewallFindings, key = { it.id }) { finding ->
                            FirewallFindingCard(finding = finding)
                        }
                    }

                    DetailSection.TOOLCHAIN -> {
                        item { ToolchainMatrixCard(report = report, onSendToArchitect = onSendToArchitect) }
                    }

                    DetailSection.PAYLOAD_ELF -> {
                        item { PayloadBreakdownCard(report = report) }
                        item { Elf16KbCard(elfResults = report.elfResults) }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun OverviewSummaryCard(
    report: ScanReport,
    onSendToArchitect: () -> Unit,
) {
    ConsoleCard(glowAccent = ElectricCyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SectionHeader(
                tag = "نتیجه نهایی اسکن",
                title = report.primaryStack.displayName,
                modifier = Modifier.weight(1f),
            )
            StatusBadge(severity = report.overallSeverity)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetricTile(
                label = "حجم فشرده",
                value = ReportFormatter.formatBytes(report.compressedBytes),
                subValue = "${report.entryCount} فایل",
                modifier = Modifier.weight(1f),
            )
            TelemetryMetricTile(
                label = "حجم غیرفشرده",
                value = ReportFormatter.formatBytes(report.uncompressedBytes),
                subValue = String.format(Locale.US, "ضریب %.2fx", report.compressionRatio),
                accentColor = AmberWarn,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "فریم‌ورک‌های شناسایی‌شده",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            report.detectedStacks.forEach { stack ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SteelElevated)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stack.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = report.zip32RiskSummary,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSendToArchitect,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("send_to_architect_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = CyanMuted,
                contentColor = ElectricCyan,
            ),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
        ) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ارسال حجم و AGP به معمار بیلد",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun ApkMetadataCard(report: ScanReport) {
    val apk = report.apkMetadata ?: return
    ConsoleCard {
        SectionHeader(
            tag = "شناسنامه بسته نصبی",
            title = apk.packageName ?: "بسته باینری اندروید",
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetricTile(
                label = "نسخه برنامه",
                value = apk.versionName ?: "نامشخص",
                subValue = apk.versionCode?.let { "کد $it" } ?: "کد نامشخص",
                modifier = Modifier.weight(1f),
            )
            TelemetryMetricTile(
                label = "بازه SDK",
                value = "Min ${apk.minSdk ?: "?"} / Tgt ${apk.targetSdk ?: "?"}",
                subValue = "${apk.dexCount} DEX • ${if (apk.hasSigningBlockOrCert) "امضادار" else "V2/V3 یا بدون امضا"}",
                accentColor = if (apk.isDebuggable) AmberWarn else EmeraldPass,
                modifier = Modifier.weight(1f),
            )
        }
        if (apk.nativeAbis.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "معماری‌های بومی (ABI): ${apk.nativeAbis.joinToString("، ")}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun RemediationChecklistCard(report: ScanReport) {
    ConsoleCard {
        SectionHeader(
            tag = "استراتژی پیشنهادی بیلد",
            title = "نقشه اقدام و بسته‌بندی",
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = report.recommendedArtifactStrategy,
            style = MaterialTheme.typography.bodyMedium,
            color = ElectricCyan,
        )
        Spacer(modifier = Modifier.height(10.dp))
        report.remediationSteps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = String.format(Locale.US, "%02d", index + 1),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                    color = ElectricCyan,
                )
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun FirewallFindingCard(finding: FirewallFinding) {
    val border = when (finding.severity) {
        Severity.CRITICAL -> CrimsonCrit.copy(alpha = 0.6f)
        Severity.WARN -> AmberWarn.copy(alpha = 0.5f)
        Severity.PASS -> HairlineBorder
    }
    ConsoleCard(borderColor = border) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = finding.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(severity = finding.severity)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = finding.detail,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        if (finding.affectedSamples.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(ObsidianBg)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                finding.affectedSamples.forEach { sample ->
                    Text(
                        text = sample,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolchainMatrixCard(
    report: ScanReport,
    onSendToArchitect: () -> Unit,
) {
    val tc = report.toolchain
    ConsoleCard {
        SectionHeader(
            tag = "ماتریس سازگاری ابزارها",
            title = "تنظیم دقیق نسخه JDK و SDK",
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetricTile(
                label = "JDK پیشنهادی",
                value = tc.recommendedJdk,
                subValue = tc.notes,
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetricTile(
                label = "AGP / Gradle",
                value = tc.agpVersion ?: "در ریشه یافت نشد",
                subValue = tc.gradleVersion?.let { "Gradle $it" } ?: "Wrapper نامشخص",
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TelemetryMetricTile(
                label = "کاتلین / SDK",
                value = tc.kotlinVersion ?: "نامشخص",
                subValue = "Compile ${tc.compileSdk ?: "?"} • Target ${tc.targetSdk ?: "?"}",
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        if (tc.ndkVersion != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "نسخه پین‌شده NDK: ${tc.ndkVersion}",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldPass,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSendToArchitect,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricCyan,
                contentColor = ObsidianBg,
            ),
        ) {
            Text(
                text = "ساخت خودکار gradle.properties در معمار بیلد",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun PayloadBreakdownCard(report: ScanReport) {
    ConsoleCard {
        SectionHeader(
            tag = "تحلیل حجم و مرز 4GB",
            title = "سهم‌بندی محتوای آرشیو",
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (report.payloadSegments.isEmpty()) {
            Text(
                text = "هیچ فایلی در جریان آرشیو ثبت نشد.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        } else {
            PayloadDistributionBar(segments = report.payloadSegments)
        }
        if (report.largeAssetsNeedingNoCompress.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "فایل‌های بالای 10MB — نیازمند ثبت در noCompress:",
                style = MaterialTheme.typography.labelSmall,
                color = AmberWarn,
            )
            Spacer(modifier = Modifier.height(4.dp))
            report.largeAssetsNeedingNoCompress.forEach { asset ->
                Text(
                    text = "• $asset",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Elf16KbCard(elfResults: List<ElfAlignmentResult>) {
    ConsoleCard {
        SectionHeader(
            tag = "ممیزی باینری‌های بومی",
            title = "هم‌ترازی 16KB در هدر ELF (${elfResults.size} فایل .so)",
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (elfResults.isEmpty()) {
            Text(
                text = "هیچ کتابخانه بومی .so در مسیر lib/ یا jniLibs/ یافت نشد.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                elfResults.forEach { elf ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SteelElevated)
                            .border(
                                1.dp,
                                if (elf.is16KbAligned) HairlineBorder else CrimsonCrit.copy(alpha = 0.6f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = elf.soPath.substringAfterLast('/'),
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${elf.abi} • ${if (elf.is64Bit) "ELF64" else "ELF32"} • align=${elf.minLoadAlignBytes} B",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = TextSecondary,
                            )
                        }
                        StatusBadge(
                            severity = if (elf.is16KbAligned) Severity.PASS else Severity.CRITICAL,
                            customLabel = if (elf.is16KbAligned) "16KB تایید" else "4KB خطا",
                        )
                    }
                }
            }
        }
    }
}
