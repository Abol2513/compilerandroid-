package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ReportFormatter
import com.example.domain.model.HostTelemetry
import com.example.domain.model.ScanProgressState
import com.example.domain.model.ScanReport
import com.example.ui.components.ConsoleCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TelemetryMetricTile
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonCrit
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import java.util.Locale

@Composable
fun ConsoleHomeScreen(
    telemetry: HostTelemetry,
    scanState: ScanProgressState,
    history: List<ScanReport>,
    onPickArchive: () -> Unit,
    onInspectSelfApk: () -> Unit,
    onRefreshTelemetry: () -> Unit,
    onDismissScanError: () -> Unit,
    onOpenReport: (ScanReport) -> Unit,
    onDeleteReport: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onOpenGuide: () -> Unit,
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
                ArchiveIntakeHeroCard(
                    isScanning = scanState is ScanProgressState.Scanning,
                    onPickArchive = onPickArchive,
                    onInspectSelfApk = onInspectSelfApk,
                    onOpenGuide = onOpenGuide,
                )
            }

            item {
                AnimatedVisibility(visible = scanState is ScanProgressState.Scanning) {
                    if (scanState is ScanProgressState.Scanning) {
                        StreamingProgressCard(state = scanState)
                    }
                }
                AnimatedVisibility(visible = scanState is ScanProgressState.Error) {
                    if (scanState is ScanProgressState.Error) {
                        ScanErrorCard(
                            message = scanState.message,
                            onDismiss = onDismissScanError,
                        )
                    }
                }
            }

            item {
                DeviceTelemetryCard(
                    telemetry = telemetry,
                    onRefresh = onRefreshTelemetry,
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionHeader(
                        tag = "حافظه محلی",
                        title = "تاریخچه بازرسی‌ها (${history.size})",
                        modifier = Modifier.weight(1f),
                    )
                    if (history.isNotEmpty()) {
                        TextButton(
                            onClick = onClearHistory,
                            modifier = Modifier.testTag("clear_history_button"),
                        ) {
                            Text(
                                text = "پاک‌کردن همه",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }

            if (history.isEmpty()) {
                item {
                    EmptyHistoryCard(onInspectSelfApk = onInspectSelfApk)
                }
            } else {
                items(history, key = { it.id }) { report ->
                    HistoryAuditCard(
                        report = report,
                        onOpen = { onOpenReport(report) },
                        onDelete = { onDeleteReport(report.id) },
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ArchiveIntakeHeroCard(
    isScanning: Boolean,
    onPickArchive: () -> Unit,
    onInspectSelfApk: () -> Unit,
    onOpenGuide: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = GunmetalSurface,
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Subtle cyber blueprint grid + dual-tone radial glow
                    val step = 28.dp.toPx()
                    val gridColor = HairlineBorder.copy(alpha = 0.28f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                        y += step
                    }
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(ElectricCyan.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.1f),
                            radius = size.width * 0.75f,
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(VioletAccent.copy(alpha = 0.14f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.9f),
                            radius = size.width * 0.65f,
                        )
                    )
                }
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldPass),
                    )
                    Icon(
                        imageVector = Icons.Filled.Terminal,
                        contentDescription = "کنسول تیتان‌دروید",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "موتور بازرسی جریانی آرشیو",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan,
                    )
                }
                Surface(
                    onClick = onOpenGuide,
                    shape = RoundedCornerShape(8.dp),
                    color = SteelElevated.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, HairlineBorder),
                    modifier = Modifier.testTag("hero_guide_chip"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = "راهنما",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "شیوه کار",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "تحلیل فوق‌سریع ZIP64، APK، AAB و AAR",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "اسکن بدون مصرف حافظه رم و بدون نیاز به مجوز Storage. تشخیص پشته پروژه (۱۳ فریم‌ورک)، دیوار آتش ZipSlip، مرز ۴ گیگابایت ZIP32 و هم‌ترازی ۱۶ کیلوبایتی ELF.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(12.dp))
            // 3-step pipeline strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PipelineStepChip("۱", "انتخاب فایل", Modifier.weight(1f))
                PipelineStepChip("۲", "ممیزی جریانی", Modifier.weight(1f))
                PipelineStepChip("۳", "تولید پچ و بیلد", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = onPickArchive,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pick_archive_button"),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = ObsidianBg,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "انتخاب فایل آرشیو",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                OutlinedButton(
                    onClick = onInspectSelfApk,
                    enabled = !isScanning,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("inspect_self_apk_button"),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, EmeraldPass.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SteelElevated.copy(alpha = 0.65f),
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Android,
                        contentDescription = null,
                        tint = EmeraldPass,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "خودآزمایی APK",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PipelineStepChip(
    num: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBg.copy(alpha = 0.72f))
            .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(CyanMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = num,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = ElectricCyan,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StreamingProgressCard(state: ScanProgressState.Scanning) {
    ConsoleCard(borderColor = ElectricCyan.copy(alpha = 0.7f), glowAccent = ElectricCyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.phaseLabel,
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan,
            )
            Text(
                text = "${state.entriesProcessed} فایل",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = TextSecondary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = state.fileName,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (state.totalBytesHint > 0L) {
            val progress = (state.bytesProcessed.toFloat() / state.totalBytesHint.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ElectricCyan,
                trackColor = SteelElevated,
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ElectricCyan,
                trackColor = SteelElevated,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "پردازش‌شده: ${ReportFormatter.formatBytes(state.bytesProcessed)}",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = TextMuted,
        )
    }
}

@Composable
private fun ScanErrorCard(
    message: String,
    onDismiss: () -> Unit,
) {
    ConsoleCard(borderColor = CrimsonCrit.copy(alpha = 0.75f), glowAccent = CrimsonCrit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "توقف اسکن جریانی",
                style = MaterialTheme.typography.labelMedium,
                color = CrimsonCrit,
            )
            TextButton(onClick = onDismiss) {
                Text("بستن", color = TextSecondary)
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
        )
    }
}

@Composable
private fun DeviceTelemetryCard(
    telemetry: HostTelemetry,
    onRefresh: () -> Unit,
) {
    ConsoleCard {
        SectionHeader(
            tag = "تله‌متری سخت‌افزار میزبان",
            title = telemetry.deviceModel,
            trailing = {
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_telemetry_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "به‌روزرسانی مشخصات دستگاه",
                        tint = ElectricCyan,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetricTile(
                label = "حافظه رم فیزیکی",
                value = String.format(Locale.US, "%.1f GB", telemetry.totalRamMb / 1024.0),
                subValue = "${telemetry.availableRamMb} MB آزاد",
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1f),
            )
            TelemetryMetricTile(
                label = "فضای آزاد دیسک",
                value = String.format(Locale.US, "%.1f GB", telemetry.freeDiskGb),
                subValue = String.format(Locale.US, "از کل %.0f GB", telemetry.totalDiskGb),
                accentColor = if (telemetry.freeDiskGb >= 15.0) EmeraldPass else AmberWarn,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TelemetryMetricTile(
                label = "پردازنده و معماری",
                value = "${telemetry.cpuCores} هسته",
                subValue = telemetry.primaryAbi,
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TelemetryMetricTile(
                label = "سیستم‌عامل",
                value = "Android ${telemetry.androidRelease}",
                subValue = "API ${telemetry.sdkInt}",
                accentColor = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyHistoryCard(onInspectSelfApk: () -> Unit) {
    ConsoleCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyanMuted)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = ElectricCyan,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "هنوز گزارشی ثبت نشده است",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "یک فایل .zip، .apk، .aab یا .aar انتخاب کنید؛ یا با دکمه زیر همین برنامه را به‌عنوان نمونه واقعی بررسی نمایید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onInspectSelfApk,
            modifier = Modifier.testTag("empty_self_audit_button"),
        ) {
            Icon(
                imageVector = Icons.Filled.Memory,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "اجرای خودآزمایی سریع APK همین الان",
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCyan,
            )
        }
    }
}

@Composable
private fun HistoryAuditCard(
    report: ScanReport,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen)
            .testTag("history_card_${report.id}"),
        shape = MaterialTheme.shapes.large,
        color = GunmetalSurface,
        border = BorderStroke(1.dp, HairlineBorder),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${report.primaryStack.displayName} • ${ReportFormatter.formatTimestamp(report.scannedAtEpochMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(severity = report.overallSeverity)
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_report_${report.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف گزارش ${report.fileName}",
                        tint = TextMuted,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "فایل‌ها: ${report.entryCount}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = ElectricCyan,
                )
                Text(
                    text = "حجم خام: ${ReportFormatter.formatBytes(report.uncompressedBytes)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = TextSecondary,
                )
                if (report.elfResults.isNotEmpty()) {
                    val aligned = report.elfResults.count { it.is16KbAligned }
                    Text(
                        text = "16KB: $aligned/${report.elfResults.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
                        color = if (aligned == report.elfResults.size) EmeraldPass else CrimsonCrit,
                    )
                }
            }
        }
    }
}
