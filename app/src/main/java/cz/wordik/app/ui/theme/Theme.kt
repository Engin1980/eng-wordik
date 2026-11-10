package cz.wordik.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.wordik.app.R
import cz.wordik.app.domain.SizeScale

/** Rozměry závislé na zvolené škále velikosti (%). */
data class Sizes(
    /** Minimální výška tlačítek. */
    val buttonHeight: Dp,
    /** Násobek velikosti písma v celé aplikaci. */
    val textScale: Float,
    /** Velikost kláves s písmeny v režimu skládání. */
    val tileSize: Dp,
)

fun sizesFor(percent: Int): Sizes {
    val scale = percent / 100f
    return Sizes(buttonHeight = 56.dp * scale, textScale = scale, tileSize = 52.dp * scale)
}

val LocalSizes = staticCompositionLocalOf { sizesFor(SizeScale.DEFAULT_PERCENT) }

/** Barvy zpětné vazby, které Material schéma nemá. */
data class FeedbackColors(
    val correct: Color,
    val onCorrect: Color,
    val wrong: Color,
    val onWrong: Color,
)

val LocalFeedbackColors = staticCompositionLocalOf {
    FeedbackColors(
        correct = Color(0xFFC8E6C9),
        onCorrect = Color(0xFF14391A),
        wrong = Color(0xFFFFCDD2),
        onWrong = Color(0xFF5F1117),
    )
}

// Veselé, ale střídmé barvy: modrá hlavní, oranžový akcent, jemné světlé pozadí.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6FD9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E6FF),
    onPrimaryContainer = Color(0xFF0B2B5C),
    secondary = Color(0xFFF08A00),
    onSecondary = Color(0xFF2B1700),
    secondaryContainer = Color(0xFFFFE2B8),
    onSecondaryContainer = Color(0xFF3A2000),
    tertiary = Color(0xFF2E9E5B),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF2),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEEE8DA),
    onSurfaceVariant = Color(0xFF4A4639),
    outline = Color(0xFF7B7667),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF5F1117),
)

/** Písmo pro celou aplikaci; musí jasně rozlišovat znaky "l", "I" a "1". */
val AtkinsonHyperlegible = FontFamily(
    Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal),
    Font(R.font.atkinson_hyperlegible_bold, FontWeight.Bold),
)

/** Převod názvu fontu z konfigurace na rodinu písma; neznámý název dá systémové písmo. */
fun fontFamilyFor(name: String): FontFamily =
    if (name.equals("Atkinson Hyperlegible", ignoreCase = true)) AtkinsonHyperlegible else FontFamily.Default

private fun TextStyle.scaled(family: FontFamily, scale: Float) = copy(
    fontFamily = family,
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale,
)

fun scaledTypography(family: FontFamily, scale: Float): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.scaled(family, scale),
        displayMedium = base.displayMedium.scaled(family, scale),
        displaySmall = base.displaySmall.scaled(family, scale),
        headlineLarge = base.headlineLarge.scaled(family, scale),
        headlineMedium = base.headlineMedium.scaled(family, scale),
        headlineSmall = base.headlineSmall.scaled(family, scale),
        titleLarge = base.titleLarge.scaled(family, scale),
        titleMedium = base.titleMedium.scaled(family, scale),
        titleSmall = base.titleSmall.scaled(family, scale),
        bodyLarge = base.bodyLarge.scaled(family, scale),
        bodyMedium = base.bodyMedium.scaled(family, scale),
        bodySmall = base.bodySmall.scaled(family, scale),
        labelLarge = base.labelLarge.scaled(family, scale),
        labelMedium = base.labelMedium.scaled(family, scale),
        labelSmall = base.labelSmall.scaled(family, scale),
    )
}

@Composable
fun WordikTheme(
    sizePercent: Int = SizeScale.DEFAULT_PERCENT,
    fontName: String = "Atkinson Hyperlegible",
    content: @Composable () -> Unit,
) {
    val sizes = sizesFor(sizePercent)
    // Tmavý režim zatím není požadován, vždy světlé schéma.
    CompositionLocalProvider(LocalSizes provides sizes) {
        MaterialTheme(
            colorScheme = LightColors,
            typography = scaledTypography(fontFamilyFor(fontName), sizes.textScale),
            content = content,
        )
    }
}
