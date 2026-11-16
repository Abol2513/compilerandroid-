package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.domain.engine.ReportFormatter
import com.example.domain.model.PayloadSegment
import com.example.domain.model.Severity
import com.example.ui.theme.AmberDim
import com.example.ui.theme.AmberWarn
import com.example.ui.theme.CrimsonCrit
import com.example.ui.theme.CrimsonDim
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldDim
import com.example.ui.theme.EmeraldPass
import com.example.ui.theme.GunmetalSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.HairlineBright
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletAccent
import java.util.Locale

@Composable
fun ConsoleCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HairlineBorder,
    containerColor: Color = GunmetalSurface,
    glowAccent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (glowAccent != null) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(glowAccent.copy(alpha = 0.14f), Color.Transparent),
                                center = Offset(size.width * 0.15f, 0f),
                                radius = size.width * 0.9f,
                            )
                        )
                    }
                    // Precision top accent line
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                (glowAccent ?: HairlineBright).copy(alpha = 0.5f),
                                Color.Transparent,
                            )
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f,
                    )
                }
                .padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(
    tag: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan),
                )
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun StatusBadge(
    severity: Severity,
    modifier: Modifier = Modifier,
    customLabel: String? = null,
) {
    val (bg, fg, icon) = when (severity) {
        Severity.PASS -> Triple(EmeraldDim, EmeraldPass, Icons.Filled.CheckCircle)
        Severity.WARN -> Triple(AmberDim, AmberWarn, Icons.Filled.Warning)
        Severity.CRITICAL -> Triple(CrimsonDim, CrimsonCrit, Icons.Filled.Error)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = severity.label,
            tint = fg,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = customLabel ?: severity.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg,
        )
    }
}

@Composable
fun TelemetryMetricTile(
    label: String,
    value: String,
    subValue: String? = null,
    accentColor: Color = ElectricCyan,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(SteelElevated, GunmetalSurface),
                )
            )
            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
            ),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subValue.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CapacityArcGauge(
    label: String,
    ratio: Float,
    centerText: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val clamped = ratio.coerceIn(0.03f, 1f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SteelElevated)
            .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(90.dp)) {
                val stroke = 8.dp.toPx()
                drawArc(
                    color = ObsidianBg,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor.copy(alpha = 0.65f), accentColor, VioletAccent),
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * clamped,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = accentColor,
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subText,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun PayloadDistributionBar(
    segments: List<PayloadSegment>,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.bytes }.coerceAtLeast(1L)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(ObsidianBg)
        ) {
            var xOffset = 0f
            val gapPx = 2.dp.toPx()
            val usableWidth = (size.width - gapPx * (segments.size - 1).coerceAtLeast(0)).coerceAtLeast(1f)
            segments.forEach { seg ->
                val ratio = (seg.bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val segWidth = (usableWidth * ratio).coerceAtLeast(if (seg.bytes > 0) 4f else 0f)
                if (segWidth > 0f) {
                    drawRoundRect(
                        color = Color(seg.colorToken),
                        topLeft = Offset(xOffset, 0f),
                        size = Size(segWidth, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                    xOffset += segWidth + gapPx
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            segments.forEach { seg ->
                val pct = (seg.bytes * 100.0) / total.toDouble()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(seg.colorToken)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${seg.category} (${seg.entryCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${ReportFormatter.formatBytes(seg.bytes)} (${String.format(Locale.US, "%.1f", pct)}%)",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                        color = TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    title: String,
    code: String,
    copyTestTag: String,
    onCopied: (String) -> Unit,
    placementHint: String? = null,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianBg)
            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SteelElevated)
                .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                color = ElectricCyan,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    onCopied("کپی شد: $title")
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag(copyTestTag),
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "کپی $title",
                    tint = ElectricCyan,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (!placementHint.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyanMuted.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.FolderCopy,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = placementHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                )
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                    ),
                    color = TextPrimary,
                )
            }
        }
    }
}
