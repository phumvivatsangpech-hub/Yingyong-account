package com.maapp.planpay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun SummaryScreen() {
    val d = Store.data
    val today = LocalDate.now()
    val unpaid = d.totalUnpaid
    val income = d.incomeIn(YearMonth.from(today))

    Screen(stringResource(R.string.tab_summary)) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            HeroBlock {
                Text(stringResource(R.string.balance_after), style = MaterialTheme.typography.labelLarge, color = Brand.onHeaderMuted)
                Money(income - unpaid, MaterialTheme.typography.displayMedium, key = "summary.balance")
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroStat(stringResource(R.string.income_this_month), income, "summary.income", Modifier.weight(1f))
                    HeroStat(stringResource(R.string.unpaid_short), unpaid, "summary.unpaid", Modifier.weight(1f))
                    HeroStat(stringResource(R.string.tab_paid), d.expenses.filter { it.paid }.sumOf { it.total } + d.dayItems.filter { it.done }.sumOf { it.amount }, "summary.paid", Modifier.weight(1f))
                }
            }
            Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.l)) {
                Breakdown(stringResource(R.string.by_category), d.unpaidBy(d.categories) { it.categoryId })
                Breakdown(stringResource(R.string.by_priority), d.unpaidBy(d.priorities) { it.priorityId })
            }
        }
    }
}

/** แถบสัดส่วน + รายการชื่อ เปอร์เซ็นต์ และยอด (ตัวเลขบอกครบ ไม่ต้องอาศัยสีอย่างเดียว) */
@Composable
private fun Breakdown(title: String, parts: List<Pair<Tag?, Int>>) {
    val total = parts.sumOf { it.second }
    val none = stringResource(R.string.none)
    val noneColor = MaterialTheme.colorScheme.outline
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // แถบขยายจากซ้ายเมื่อเปิดหน้า (ตอน render รูปตัวอย่างให้เต็มตั้งแต่แรก)
    val preview = LocalInspectionMode.current
    val grow = remember { Animatable(if (preview) 1f else 0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, Motion.spec(Motion.reveal)) }
    Group(title) {
        Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (total == 0) Text(stringResource(R.string.empty), color = muted)
            else Row(Modifier.fillMaxWidth(grow.value).height(20.dp).clip(RoundedCornerShape(6.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                parts.forEach { (t, n) ->
                    Box(Modifier.weight(n.toFloat()).fillMaxHeight().background(t?.let { Color(it.color) } ?: noneColor))
                }
            }
            if (total > 0) parts.forEach { (t, n) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    Box(Modifier.size(10.dp).background(t?.let { Color(it.color) } ?: noneColor, CircleShape))
                    Text(t?.name ?: none, Modifier.weight(1f))
                    Text("${n * 100L / total}%", color = muted, style = MaterialTheme.typography.bodySmall)
                    Money(n, modifier = Modifier.widthIn(min = 84.dp).wrapContentWidth(Alignment.End))
                }
            }
        }
    }
}
