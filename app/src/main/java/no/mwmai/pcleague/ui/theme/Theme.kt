package no.mwmai.pcleague.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Classic DOS / EGA-VGA 16-colour palette. */
object Dos {
    val Black = Color(0xFF000000)
    val Blue = Color(0xFF0000AA)
    val Green = Color(0xFF00AA00)
    val Cyan = Color(0xFF00AAAA)
    val Red = Color(0xFFAA0000)
    val Magenta = Color(0xFFAA00AA)
    val Brown = Color(0xFFAA5500)
    val LightGray = Color(0xFFAAAAAA)
    val DarkGray = Color(0xFF555555)
    val BrightBlue = Color(0xFF5555FF)
    val BrightGreen = Color(0xFF55FF55)
    val BrightCyan = Color(0xFF55FFFF)
    val BrightRed = Color(0xFFFF5555)
    val BrightMagenta = Color(0xFFFF55FF)
    val Yellow = Color(0xFFFFFF55)
    val White = Color(0xFFFFFFFF)

    // semantic
    val Background = Color(0xFF000080)   // deep DOS blue
    val Panel = Blue
    val Frame = BrightCyan
    val Heading = Yellow
    val Text = LightGray
    val TextHi = White
    val Accent = BrightGreen
    val Bad = BrightRed
    val StatusBg = Cyan
    val StatusFg = Black
}

val Mono = FontFamily.Monospace

private val PclColors = darkColorScheme(
    primary = Dos.BrightCyan,
    onPrimary = Dos.Black,
    secondary = Dos.Yellow,
    background = Dos.Background,
    onBackground = Dos.LightGray,
    surface = Dos.Blue,
    onSurface = Dos.LightGray,
    error = Dos.BrightRed,
)

private fun mono(size: Int, w: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = Mono, fontSize = size.sp, fontWeight = w, letterSpacing = 0.5.sp)

private val PclType = Typography(
    displayLarge = mono(28, FontWeight.Bold),
    titleLarge = mono(20, FontWeight.Bold),
    titleMedium = mono(16, FontWeight.Bold),
    bodyLarge = mono(15),
    bodyMedium = mono(14),
    bodySmall = mono(12),
    labelLarge = mono(14, FontWeight.Bold),
    labelSmall = mono(11),
)

@Composable
fun PcLeagueTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = PclColors, typography = PclType, content = content)
}
