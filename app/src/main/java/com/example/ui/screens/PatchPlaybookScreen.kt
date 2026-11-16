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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.PatchCategory
import com.example.domain.model.PatchRule
import com.example.domain.model.Severity
import com.example.ui.components.CodeBlockCard
import com.example.ui.components.ConsoleCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.SteelElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PatchPlaybookScreen(
    logInput: String,
    selectedCategory: PatchCategory,
    diagnosedRules: List<PatchRule>,
    rules: List<PatchRule>,
    onLogInputChange: (String) -> Unit,
    onSelectCategory: (PatchCategory) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConsoleCard(glowAccent = ElectricCyan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionHeader(
                            tag = "عیب‌یابی خودکار خطای بیلد",
                            title = "تشخیص‌دهنده لاگ Gradle / NDK",
                            modifier = Modifier.weight(1f),
                        )
                        if (logInput.isNotEmpty()) {
                            TextButton(
                                onClick = { onLogInputChange("") },
                                modifier = Modifier.testTag("clear_log_button"),
                            ) {
                                Text("پاک‌کردن", color = TextSecondary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = logInput,
                        onValueChange = onLogInputChange,
                        placeholder = {
                            Text(
                                text = "متن خطای Gradle، CMake یا Manifest Merger را اینجا پیست کنید...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        },
                        textStyle = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        minLines = 3,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = ElectricCyan,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_diagnoser_input"),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "نمونه خطاهای پرتکرار برای تست سریع:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SampleErrorChip("خطای Namespace") {
                            onLogInputChange("Namespace not specified. Specify a namespace in the module's build file.")
                        }
                        SampleErrorChip("هم‌ترازی 16KB") {
                            onLogInputChange("libnative.so does not support 16 KB ELF alignment: LOAD segment not aligned at 16384")
                        }
                        SampleErrorChip("خطای jcenter()") {
                            onLogInputChange("Could not find method jcenter() or Received status code 403 from jcenter.bintray.com")
                        }
                        SampleErrorChip("خطای exported") {
                            onLogInputChange("Manifest merger failed: android:exported needs to be explicitly specified")
                        }
                    }
                    if (logInput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (diagnosedRules.isNotEmpty()) {
                            Text(
                                text = "${diagnosedRules.size} پچ دقیق متناسب با خطای شما پیدا شد:",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan,
                            )
                        } else {
                            Text(
                                text = "الگوی دقیقی در لاگ پیدا نشد. دسته‌بندی‌های زیر را مرور کنید یا نسخه JDK را بررسی نمایید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AmberWarn,
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
                    PatchCategory.entries.forEach { category ->
                        val selected = selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectCategory(category) },
                            label = { Text(category.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanMuted,
                                selectedLabelColor = ElectricCyan,
                                containerColor = SteelElevated,
                                labelColor = TextSecondary,
                            ),
                            border = BorderStroke(1.dp, if (selected) ElectricCyan else HairlineBorder),
                            modifier = Modifier.testTag("patch_cat_${category.name.lowercase()}"),
                        )
                    }
                }
            }

            items(rules, key = { it.id }) { rule ->
                val isMatched = diagnosedRules.any { it.id == rule.id }
                PatchRuleCard(
                    rule = rule,
                    isMatched = isMatched,
                    onCopied = onCopied,
                )
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SampleErrorChip(
    label: String,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SteelElevated,
            labelColor = TextSecondary,
        ),
        border = BorderStroke(1.dp, HairlineBorder),
    )
}

@Composable
private fun PatchRuleCard(
    rule: PatchRule,
    isMatched: Boolean,
    onCopied: (String) -> Unit,
) {
    ConsoleCard(
        borderColor = if (isMatched) ElectricCyan else HairlineBorder,
        glowAccent = if (isMatched) ElectricCyan else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rule.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
            }
            if (isMatched) {
                Spacer(modifier = Modifier.widthIn(min = 8.dp))
                StatusBadge(severity = Severity.PASS, customLabel = "تطبیق دقیق")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = rule.rootCause,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(10.dp))
        CodeBlockCard(
            title = "تغییر پیشنهادی (Patch Diff)",
            code = rule.minimalPatchDiff,
            copyTestTag = "copy_patch_${rule.id}",
            placementHint = "محل قرارگیری: ${rule.targetFileLocation}",
            onCopied = onCopied,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "تایید نهایی: ${rule.verificationHint}",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
        )
    }
}
