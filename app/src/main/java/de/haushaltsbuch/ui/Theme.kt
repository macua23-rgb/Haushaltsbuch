package de.haushaltsbuch.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Design "Pegel": ruhiges Petrol, Mint als Akzent. Alle Textfarben erfüllen mindestens Kontrast AA.
val Lagune = Color(0xFF0F5C6E)
val Tinte = Color(0xFF0B2A33)
val Schaum = Color(0xFFF2F8F7)
val Brandung = Color(0xFF8AD7C0)
val Gedaempft = Color(0xFF4A6670)
val Linie = Color(0xFFD5E4E1)

val Income = Color(0xFF0B6B54)
val Expense = Color(0xFFB3302A)
val Warning = Color(0xFFA34A00)
val IncomeBar = Color(0xFF2A9479)   // Balkenfarbe für Einnahmen, heller als Petrol (Ausgaben)

private val PegelColors = lightColorScheme(
    primary = Lagune, onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EEE7), onPrimaryContainer = Tinte,
    secondary = Gedaempft, onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEBE3), onSecondaryContainer = Tinte,
    tertiary = Income, onTertiary = Color.White,
    background = Schaum, onBackground = Tinte,
    surface = Schaum, onSurface = Tinte,
    surfaceVariant = Color(0xFFDCEAE7), onSurfaceVariant = Gedaempft,
    outline = Color(0xFF6B8089), outlineVariant = Linie,
    error = Expense, onError = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEEF5F3),
    surfaceContainerHighest = Color.White
)

/** Ziffern in gleicher Breite, damit Beträge in Listen sauber untereinander stehen. */
private fun TextStyle.tnum() = copy(fontFeatureSettings = "tnum")

private val PegelTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.tnum().copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.tnum().copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.tnum().copy(fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.tnum().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.tnum(),
        bodyMedium = t.bodyMedium.tnum(),
        bodySmall = t.bodySmall.tnum(),
        labelLarge = t.labelLarge.tnum(),
        labelMedium = t.labelMedium.tnum()
    )
}

private val PegelShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp)
)

/** Das Design ist bewusst nur hell; ein Dunkelmodus folgt als eigener Schritt. */
@Composable
fun HaushaltsbuchTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PegelColors, typography = PegelTypography, shapes = PegelShapes, content = content)
}
