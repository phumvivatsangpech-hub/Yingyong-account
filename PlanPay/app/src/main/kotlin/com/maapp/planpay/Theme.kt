package com.maapp.planpay

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ธีมกลางของแอพ: โทนครีมอุ่น สีเน้นส้มอิฐสีเดียว ทุกหน้าใช้สี รูปทรง และตัวอักษรจากที่นี่
// คู่สีตัวอักษร/พื้นหลังทุกคู่มี contrast อย่างน้อย 4.5:1

private val Light = lightColorScheme(
    primary = Color(0xFFA94A28), onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D9CC), onPrimaryContainer = Color(0xFF4A1D0B),
    secondary = Color(0xFF6B665C), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3D9CC), onSecondaryContainer = Color(0xFF4A1D0B),
    tertiary = Color(0xFF4E6B3A), onTertiary = Color.White,
    background = Color(0xFFF5F1E8), onBackground = Color(0xFF2B2925),
    surface = Color(0xFFFFFDF8), onSurface = Color(0xFF2B2925),
    surfaceVariant = Color(0xFFECE6D9), onSurfaceVariant = Color(0xFF6B665C),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFBF8F1),
    surfaceContainer = Color(0xFFFFFDF8), surfaceContainerHigh = Color(0xFFFFFDF8),
    surfaceContainerHighest = Color(0xFFECE6D9),
    outline = Color(0xFF8A8478), outlineVariant = Color(0xFFDDD6C7),
    error = Color(0xFFB3261E), onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Color(0xFFE08A65), onPrimary = Color(0xFF2B1408),
    primaryContainer = Color(0xFF5A2A16), onPrimaryContainer = Color(0xFFF9DCCF),
    secondary = Color(0xFFB5AFA3), onSecondary = Color(0xFF1F1E1B),
    secondaryContainer = Color(0xFF5A2A16), onSecondaryContainer = Color(0xFFF9DCCF),
    tertiary = Color(0xFFB5CFA0), onTertiary = Color(0xFF1C2A12),
    background = Color(0xFF1F1E1B), onBackground = Color(0xFFECE8DF),
    surface = Color(0xFF2A2926), onSurface = Color(0xFFECE8DF),
    surfaceVariant = Color(0xFF35332F), onSurfaceVariant = Color(0xFFB5AFA3),
    surfaceContainerLowest = Color(0xFF191816), surfaceContainerLow = Color(0xFF252421),
    surfaceContainer = Color(0xFF2A2926), surfaceContainerHigh = Color(0xFF302F2B),
    surfaceContainerHighest = Color(0xFF3A3834),
    outline = Color(0xFF9A9488), outlineVariant = Color(0xFF45423C),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppType = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** ระยะห่างมาตรฐาน (ฐาน 8dp) */
object Space {
    val s = 8.dp
    val m = 16.dp
    val l = 24.dp
}

@Composable
fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (isSystemInDarkTheme()) Dark else Light,
    shapes = AppShapes,
    typography = AppType,
    content = content,
)
