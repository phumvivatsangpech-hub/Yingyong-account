package com.maapp.planpay

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ธีมกลางของแอพ ทุกหน้าใช้สี รูปทรง และตัวอักษรจากที่นี่
// แนวคิด: เขียวเข้มแบบหมึก (สีหลักและแถบหัว) บนพื้นกระดาษขาวอมเทา เน้นด้วยเหลืองดาวเรืองสีเดียว
// คู่สีตัวอักษร/พื้นหลังทุกคู่มี contrast อย่างน้อย 4.5:1 (เหลืองดาวเรืองใช้เป็นพื้นเท่านั้น ไม่ใช้เป็นสีตัวอักษรบนพื้นขาว)

private val Ink = Color(0xFF14342B)

private val Light = lightColorScheme(
    primary = Ink, onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E6DC), onPrimaryContainer = Ink,
    secondary = Color(0xFF55665E), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7B2), onSecondaryContainer = Ink,
    tertiary = Color(0xFFF2A81D), onTertiary = Ink,
    background = Color(0xFFF1F4EF), onBackground = Color(0xFF14251F),
    surface = Color.White, onSurface = Color(0xFF14251F),
    surfaceVariant = Color(0xFFE3E8E2), onSurfaceVariant = Color(0xFF55665E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
    surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE3E8E2),
    outline = Color(0xFF7B8A82), outlineVariant = Color(0xFFDDE3DC),
    error = Color(0xFFBA2B25), onError = Color.White,
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD3B4), onPrimary = Color(0xFF0B2A1F),
    primaryContainer = Color(0xFF23483B), onPrimaryContainer = Color(0xFFD5E6DC),
    secondary = Color(0xFF9DB0A6), onSecondary = Color(0xFF0E1A16),
    secondaryContainer = Color(0xFF4A3D17), onSecondaryContainer = Color(0xFFFCE7B2),
    tertiary = Color(0xFFF2B544), onTertiary = Color(0xFF14251F),
    background = Color(0xFF0E1A16), onBackground = Color(0xFFE6EDE8),
    surface = Color(0xFF16261F), onSurface = Color(0xFFE6EDE8),
    surfaceVariant = Color(0xFF22352C), onSurfaceVariant = Color(0xFF9DB0A6),
    surfaceContainerLowest = Color(0xFF0A1410), surfaceContainerLow = Color(0xFF13211B),
    surfaceContainer = Color(0xFF16261F), surfaceContainerHigh = Color(0xFF1B2D25),
    surfaceContainerHighest = Color(0xFF22352C),
    outline = Color(0xFF7F9188), outlineVariant = Color(0xFF263A31),
    error = Color(0xFFFF8A80), onError = Color(0xFF5C100C),
)

/** สีเฉพาะของแอพที่ Material ไม่มีช่องให้ */
object Brand {
    private val dark @Composable get() = isSystemInDarkTheme()

    /** แถบหัวและบล็อกตัวเลขหลักของแต่ละหน้า */
    val header @Composable get() = if (dark) Color(0xFF173027) else Ink
    val onHeader @Composable get() = if (dark) Color(0xFFE6EDE8) else Color.White
    val onHeaderMuted = Color(0xFFA9C1B6)
    val headerTrack @Composable get() = if (dark) Color(0xFF2A4A3D) else Color(0xFF2B4F43)

    /** เหลืองดาวเรือง: ความคืบหน้าของเงินเก็บ และปุ่มเพิ่ม */
    val accent @Composable get() = if (dark) Color(0xFFF2B544) else Color(0xFFF2A81D)
    val onAccent = Color(0xFF14251F)

    /** พื้นของแถวรายการส่วนที่เก็บเงินได้แล้ว */
    val fill @Composable get() = if (dark) Color(0xFF3F3515) else Color(0xFFFCEBC0)

    /** ยอดเป็นบวก / จ่ายแล้ว */
    val good @Composable get() = if (dark) Color(0xFF7FD1A3) else Color(0xFF1F7A4D)
    /** สีสถานะดีบนแถบหัวสีเข้ม */
    val goodOnHeader = Color(0xFF8FD3B4)

    /** ป้ายเตือนเลยกำหนดบนแถบหัว */
    val alert = Color(0xFFFFDAD6)
    val onAlert = Color(0xFF7A1512)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Plex = FontFamily(
    Font(R.font.plex_regular, FontWeight.Normal),
    Font(R.font.plex_medium, FontWeight.Medium),
    Font(R.font.plex_semibold, FontWeight.SemiBold),
    Font(R.font.plex_bold, FontWeight.Bold),
)

private fun TextStyle.plex(weight: FontWeight = fontWeight ?: FontWeight.Normal) = copy(fontFamily = Plex, fontWeight = weight)

private val AppType = Typography().run {
    Typography(
        displayLarge = displayLarge.plex(FontWeight.Bold),
        displayMedium = displayMedium.plex(FontWeight.Bold),
        displaySmall = displaySmall.plex(FontWeight.Bold),
        headlineLarge = headlineLarge.plex(FontWeight.Bold),
        headlineMedium = headlineMedium.plex(FontWeight.Bold),
        headlineSmall = headlineSmall.plex(FontWeight.SemiBold),
        titleLarge = titleLarge.plex(FontWeight.SemiBold),
        titleMedium = titleMedium.plex(FontWeight.SemiBold),
        titleSmall = titleSmall.plex(FontWeight.SemiBold),
        bodyLarge = bodyLarge.plex(),
        bodyMedium = bodyMedium.plex(),
        bodySmall = bodySmall.plex(),
        labelLarge = labelLarge.plex(FontWeight.Medium),
        labelMedium = labelMedium.plex(FontWeight.Medium),
        labelSmall = labelSmall.plex(FontWeight.Medium),
    )
}

/**
 * จังหวะการเคลื่อนไหวทั้งแอพ ทุก animation อ้างค่าจากที่นี่
 * Compose คูณระยะเวลาด้วยค่า "อัตราเร็วภาพเคลื่อนไหว" ของเครื่องให้เอง: ถ้าผู้ใช้ปิด animation ในระบบ ทุกอย่างจะเปลี่ยนทันที
 */
object Motion {
    const val quick = 160      // สี ปุ่ม การกด
    const val standard = 300   // ตัวเลขนับ แถบเลื่อน เปลี่ยนหน้า
    const val reveal = 450     // แถบสัดส่วนขยายตอนเปิดหน้า
    const val celebrate = 800  // จ่ายแล้ว (จังหวะเด่นที่เดียวของแอพ)
    val ease = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> spec(ms: Int = standard) = tween<T>(ms, easing = ease)
    val press = spring<Float>(dampingRatio = 0.55f, stiffness = 700f)
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
