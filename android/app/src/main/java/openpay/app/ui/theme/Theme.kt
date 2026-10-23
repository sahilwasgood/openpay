package openpay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import openpay.app.R

private val Ink = Color(0xFF0D0F0E)
private val Paper = Color(0xFFFAFAF7)
private val CardWhite = Color(0xFFFFFFFF)
private val Ink2 = Color(0xFF4B524D)
private val Ink3 = Color(0xFF7A807B)
private val Line = Color(0xFFE3E1D8)
private val Green = Color(0xFF0E7C46)
private val GreenDeep = Color(0xFF0A5C34)
private val GreenSoft = Color(0xFFE4F2E8)
private val Amber = Color(0xFFB45309)
private val AmberSoft = Color(0xFFF6ECD9)
private val DangerRed = Color(0xFFB3261E)

val SignalGreen = Green
val WarnAmber = Amber
val FaintInk = Ink3

@OptIn(ExperimentalTextApi::class)
private fun variableFont(res: Int, weight: FontWeight) = Font(
    res,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

private val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Medium),
    variableFont(R.font.space_grotesk, FontWeight.SemiBold),
    variableFont(R.font.space_grotesk, FontWeight.Bold)
)

private val Inter = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold)
)

private fun scaled(style: TextStyle, m: Float): TextStyle = style.copy(
    fontSize = style.fontSize * m,
    lineHeight = if (style.lineHeight.isSpecified) style.lineHeight * m else style.lineHeight
)

private fun typography(m: Float): Typography = Typography(
    displayLarge = scaled(TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, letterSpacing = (-0.5).sp), m),
    headlineMedium = scaled(TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = 0.sp), m),
    headlineSmall = scaled(TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = 0.sp), m),
    titleMedium = scaled(TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 17.sp), m),
    titleSmall = scaled(TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp), m),
    bodyLarge = scaled(TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp), m),
    bodyMedium = scaled(TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp), m),
    bodySmall = scaled(TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp), m),
    labelLarge = scaled(TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 15.sp), m),
    labelMedium = scaled(TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 13.sp), m),
    labelSmall = scaled(TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.4.sp), m)
)

private val LightColors = lightColorScheme(
    primary = Green,
    onPrimary = Color.White,
    primaryContainer = GreenSoft,
    onPrimaryContainer = GreenDeep,
    secondary = Ink2,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2F2EC),
    onSurfaceVariant = Ink2,
    outline = Line,
    outlineVariant = Line,
    error = DangerRed,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2FBF71),
    onPrimary = Ink,
    primaryContainer = Color(0xFF14402A),
    onPrimaryContainer = Color(0xFFB7E9CC),
    background = Color(0xFF0F1110),
    onBackground = Color(0xFFE7EAE7),
    surface = Color(0xFF161A18),
    onSurface = Color(0xFFF2F4F1),
    surfaceVariant = Color(0xFF1E2320),
    onSurfaceVariant = Color(0xFF9BA19B),
    outline = Color(0xFF2E342F),
    error = Color(0xFFF2B8B5)
)

@Composable
fun OpenPayTheme(elderly: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = typography(if (elderly) 1.28f else 1f),
        content = content
    )
}
