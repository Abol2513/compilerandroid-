package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TitanDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = CyanMuted,
    onPrimaryContainer = CyanSoft,
    secondary = AmberWarn,
    onSecondary = ObsidianBg,
    secondaryContainer = AmberDim,
    onSecondaryContainer = AmberWarn,
    tertiary = VioletAccent,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = GunmetalSurface,
    onSurface = TextPrimary,
    surfaceVariant = SteelElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = GunmetalSurface,
    surfaceContainer = SteelElevated,
    surfaceContainerHigh = SteelHigh,
    outline = HairlineBorder,
    outlineVariant = HairlineBright,
    error = CrimsonCrit,
    onError = TextPrimary,
    errorContainer = CrimsonDim,
    onErrorContainer = CrimsonCrit,
)

private val TitanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun TitanDroidTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TitanDarkColorScheme,
        typography = TitanTypography,
        shapes = TitanShapes,
        content = content,
    )
}
