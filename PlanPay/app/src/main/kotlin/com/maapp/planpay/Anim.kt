package com.maapp.planpay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

// ชิ้นส่วน animation ที่ใช้ร่วมกัน ค่าจริงใน Store เปลี่ยนทันทีเสมอ ที่นี่เป็นแค่การแสดงผล

/** ค่าที่แสดงล่าสุดของแต่ละจุด จำข้ามหน้าไว้ เพื่อให้กลับมาหน้าเดิมแล้วเลื่อนจากค่าเก่าไปค่าใหม่ได้ */
private val lastShown = HashMap<String, Float>()

/**
 * ค่าที่เลื่อนจากค่าที่เคยแสดงไปค่าใหม่เมื่อข้อมูลเปลี่ยน
 * เปิดหน้าโดยที่ค่าไม่เปลี่ยน = ไม่มี animation (ไม่นับใหม่ทุกครั้งที่เปิดหน้า)
 */
@Composable
fun animatedValue(key: String, target: Float): Float {
    val anim = remember(key) { Animatable(lastShown[key] ?: target) }
    LaunchedEffect(target) {
        lastShown[key] = target
        anim.animateTo(target, Motion.spec())
    }
    return anim.value
}

/** ตัวเลขที่นับจากค่าเดิมไปค่าใหม่ */
@Composable
fun animatedInt(key: String, target: Int): Int {
    val v = animatedValue(key, target.toFloat())
    return if (v == target.toFloat()) target else v.roundToInt() // จบแล้วใช้ค่าจริง ไม่ผ่าน Float (เลขใหญ่จะเพี้ยน)
}

/** ย่อลงเล็กน้อยตอนกดค้าง แล้วเด้งกลับ */
@Composable
fun Modifier.pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, Motion.press, label = "press")
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

/** ข้อความที่เส้นขีดฆ่าลากผ่านจากซ้ายไปขวาเมื่อ done (ทีละบรรทัดถ้าข้อความยาวหลายบรรทัด) */
@Composable
fun StrikeText(text: String, done: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (done) 1f else 0f, Motion.spec(), label = "strike")
    val color by animateColorAsState(
        if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, Motion.spec(), label = "strikeColor",
    )
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text, color = color, onTextLayout = { layout = it },
        modifier = modifier.drawWithContent {
            drawContent()
            val l = layout ?: return@drawWithContent
            for (i in 0 until l.lineCount) {
                val part = (progress * l.lineCount - i).coerceIn(0f, 1f)
                if (part <= 0f) break
                val y = (l.getLineTop(i) + l.getLineBottom(i)) / 2 + 1.dp.toPx()
                val x0 = l.getLineLeft(i)
                drawLine(color, Offset(x0, y), Offset(x0 + (l.getLineRight(i) - x0) * part, y), 1.5.dp.toPx())
            }
        },
    )
}

private fun easeOutBack(t: Float): Float {
    val c1 = 1.70158f
    val u = t - 1f
    return 1f + (c1 + 1f) * u * u * u + c1 * u * u
}

/**
 * เครื่องหมาย "จ่ายแล้ว": วงกลมเด้งขึ้นมา วงแหวนขยายออกแล้วจาง เครื่องหมายถูกวาดตัวเอง
 * เล่นเฉพาะตอนสถานะเปลี่ยนเป็นจ่ายแล้ว เปิดรายการที่จ่ายไปแล้วจะเห็นภาพนิ่ง ไม่จ่าย = ไม่วาดอะไร
 */
@Composable
fun PaidBadge(paid: Boolean, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(if (paid) 1f else 0f) }
    LaunchedEffect(paid) {
        if (paid) anim.animateTo(1f, tween(Motion.celebrate, easing = LinearEasing)) else anim.snapTo(0f)
    }
    val disc = Brand.accent
    val mark = Brand.onAccent
    val label = stringResource(R.string.tab_paid)
    Canvas(if (paid) modifier.semantics { contentDescription = label } else modifier) {
        val p = anim.value
        if (p <= 0f) return@Canvas
        val r = size.minDimension / 2
        // วงกลม: 0–45% ของเวลา เด้งเกินขนาดเล็กน้อยแล้วกลับ
        drawCircle(disc, r * 0.76f * easeOutBack((p / 0.45f).coerceIn(0f, 1f)))
        // วงแหวน: 10–80% ขยายออกแล้วจางหาย
        val ring = ((p - 0.1f) / 0.7f).coerceIn(0f, 1f)
        if (ring > 0f && ring < 1f) drawCircle(disc, r * (0.76f + 0.24f * ring), alpha = 1f - ring, style = Stroke(2.dp.toPx()))
        // เครื่องหมายถูก: 35–80% วาดจากซ้ายไปขวา
        val draw = ((p - 0.35f) / 0.45f).coerceIn(0f, 1f)
        if (draw > 0f) {
            val w = size.width
            val h = size.height
            val full = Path().apply { moveTo(w * 0.32f, h * 0.52f); lineTo(w * 0.45f, h * 0.65f); lineTo(w * 0.69f, h * 0.38f) }
            val shown = Path()
            PathMeasure().apply { setPath(full, false) }.let { it.getSegment(0f, it.length * draw, shown, true) }
            drawPath(shown, mark, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
