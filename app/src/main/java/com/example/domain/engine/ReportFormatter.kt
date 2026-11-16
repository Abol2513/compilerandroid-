package com.example.domain.engine

import com.example.domain.model.ScanReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object ReportFormatter {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroup = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroup.toDouble())
        return if (digitGroup == 0) {
            "$bytes B"
        } else {
            String.format(Locale.US, "%.2f %s", value, units[digitGroup])
        }
    }

    fun formatTimestamp(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(epochMs))
    }

    fun toJson(report: ScanReport): String {
        fun esc(s: String): String = s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        return buildString {
            appendLine("{")
            appendLine("  \"fileName\": \"${esc(report.fileName)}\",")
            appendLine("  \"archiveKind\": \"${report.archiveKind.name}\",")
            appendLine("  \"overallSeverity\": \"${report.overallSeverity.name}\",")
            appendLine("  \"compressedBytes\": ${report.compressedBytes},")
            appendLine("  \"uncompressedBytes\": ${report.uncompressedBytes},")
            appendLine("  \"compressionRatio\": ${String.format(Locale.US, "%.2f", report.compressionRatio)},")
            appendLine("  \"entryCount\": ${report.entryCount},")
            appendLine("  \"primaryStack\": \"${esc(report.primaryStack.displayName)}\",")
            appendLine("  \"detectedStacks\": [${report.detectedStacks.joinToString(", ") { "\"${esc(it.displayName)}\"" }}],")
            appendLine("  \"toolchain\": {")
            appendLine("    \"agpVersion\": ${report.toolchain.agpVersion?.let { "\"${esc(it)}\"" } ?: "null"},")
            appendLine("    \"gradleVersion\": ${report.toolchain.gradleVersion?.let { "\"${esc(it)}\"" } ?: "null"},")
            appendLine("    \"kotlinVersion\": ${report.toolchain.kotlinVersion?.let { "\"${esc(it)}\"" } ?: "null"},")
            appendLine("    \"compileSdk\": ${report.toolchain.compileSdk ?: "null"},")
            appendLine("    \"targetSdk\": ${report.toolchain.targetSdk ?: "null"},")
            appendLine("    \"minSdk\": ${report.toolchain.minSdk ?: "null"},")
            appendLine("    \"recommendedJdk\": \"${esc(report.toolchain.recommendedJdk)}\"")
            appendLine("  },")
            appendLine("  \"elf16KbAudit\": [")
            report.elfResults.forEachIndexed { idx, elf ->
                val comma = if (idx < report.elfResults.lastIndex) "," else ""
                appendLine("    {\"soPath\": \"${esc(elf.soPath)}\", \"abi\": \"${esc(elf.abi)}\", \"is64Bit\": ${elf.is64Bit}, \"minLoadAlignBytes\": ${elf.minLoadAlignBytes}, \"is16KbAligned\": ${elf.is16KbAligned}}$comma")
            }
            appendLine("  ],")
            appendLine("  \"firewallFindings\": [")
            report.firewallFindings.forEachIndexed { idx, f ->
                val comma = if (idx < report.firewallFindings.lastIndex) "," else ""
                appendLine("    {\"id\": \"${esc(f.id)}\", \"severity\": \"${f.severity.name}\", \"title\": \"${esc(f.title)}\"}$comma")
            }
            appendLine("  ],")
            appendLine("  \"recommendedArtifactStrategy\": \"${esc(report.recommendedArtifactStrategy)}\"")
            append("}")
        }
    }

    fun toMarkdown(report: ScanReport): String = buildString {
        appendLine("# گزارش ممیزی تیتان‌دروید: ${report.fileName}")
        appendLine("- **وضعیت نهایی**: ${report.overallSeverity.label}")
        appendLine("- **نوع بسته**: ${report.archiveKind.label}")
        appendLine("- **پشته اصلی تشخیص‌داده‌شده**: ${report.primaryStack.displayName} (${report.primaryStack.buildSystem})")
        appendLine("- **تعداد فایل‌ها**: ${report.entryCount} | **فشرده**: ${formatBytes(report.compressedBytes)} | **غیرفشرده**: ${formatBytes(report.uncompressedBytes)} (${String.format(Locale.US, "%.2f", report.compressionRatio)}x)")
        appendLine("- **نسخه JDK پیشنهادی**: ${report.toolchain.recommendedJdk}")
        appendLine()
        appendLine("## دیوار آتش امنیتی و بهداشت آرشیو")
        report.firewallFindings.forEach { f ->
            appendLine("- **[${f.severity.label}]** ${f.title}: ${f.detail}")
        }
        if (report.elfResults.isNotEmpty()) {
            appendLine()
            appendLine("## ممیزی هم‌ترازی 16KB کتابخانه‌های بومی ELF")
            report.elfResults.forEach { elf ->
                val status = if (elf.is16KbAligned) "تایید" else "خطا"
                appendLine("- [$status] `${elf.soPath}` (${elf.abi}, align=${elf.minLoadAlignBytes} B)")
            }
        }
        if (report.remediationSteps.isNotEmpty()) {
            appendLine()
            appendLine("## اقدامات اصلاحی پیشنهادی")
            report.remediationSteps.forEach { step ->
                appendLine("1. $step")
            }
        }
    }
}
