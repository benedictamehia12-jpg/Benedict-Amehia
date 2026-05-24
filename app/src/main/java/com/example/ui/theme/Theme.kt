package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CustomDarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = RiskPurple,
    tertiary = ApprovedGreen,
    background = CarbonBlack,
    surface = ObsidianCard,
    error = BlockedRed,
    onPrimary = CarbonBlack,
    onSecondary = CarbonBlack,
    onTertiary = CarbonBlack,
    onBackground = OffWhiteText,
    onSurface = OffWhiteText,
    outline = BorderGrey
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CustomDarkColorScheme,
        typography = Typography,
        content = content
    )
}
