package com.maapp.planpay

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun HomeScreen(go: (String) -> Unit) {
    val d = Store.data
    val today = LocalDate.now()
    var showPaid by rememberSaveable { mutableStateOf(false) }
    var cat by rememberSaveable { mutableStateOf<Long?>(null) }
    val shown = d.expenses
        .filter { it.paid == showPaid && (cat == null || it.categoryId == cat) }
        .sortedWith(compareBy<Expense>(
            { e -> d.priorities.indexOfFirst { it.id == e.priorityId }.let { if (it < 0) Int.MAX_VALUE else it } },
            { it.due ?: "9999" },
        ))

    Screen(
        stringResource(R.string.home_title),
        fab = { AddFab(stringResource(R.string.add_expense)) { go("edit") } },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item(key = "hero") { Hero(d, today) }
            item(key = "filters") {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Space.m, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(Space.s), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pill(stringResource(R.string.tab_unpaid), !showPaid) { showPaid = false }
                    Pill(stringResource(R.string.tab_paid), showPaid) { showPaid = true }
                    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp))
                    Pill(stringResource(R.string.all), cat == null) { cat = null }
                    d.categories.forEach { c -> Pill(c.name, cat == c.id) { cat = c.id } }
                }
            }
            if (shown.isEmpty()) item(key = "empty") {
                Text(
                    stringResource(if (d.expenses.isEmpty()) R.string.empty_home else R.string.empty),
                    Modifier.animateItem().padding(horizontal = 20.dp, vertical = Space.m), color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // animateItem: แถวที่เพิ่ม ลบ หรือเปลี่ยนลำดับ (รวมตอนสลับตัวกรอง) เลื่อนเข้าที่ ไม่กระโดด
            items(shown, key = { it.id }) { e ->
                ExpenseRow(e, d, today, Modifier.animateItem().padding(horizontal = Space.m, vertical = 5.dp)) { go("detail:${e.id}") }
            }
        }
    }
}

/** ตัวเลขหลักของแอพ: วันนี้ต้องเก็บเท่าไร พร้อมยอดค้างจ่าย รายรับ และคงเหลือ */
@Composable
private fun Hero(d: AppData, today: LocalDate) {
    val open = d.expenses.filter { !it.paid }
    val overdue = open.filter { it.overdue(today) }
    val unpaid = d.totalUnpaid
    val income = d.incomeIn(YearMonth.from(today))
    HeroBlock {
        Text(stringResource(R.string.today_save), style = MaterialTheme.typography.labelLarge, color = Brand.onHeaderMuted)
        Money(open.sumOf { it.daily(today) ?: 0 }, MaterialTheme.typography.displayMedium, key = "home.daily")
        if (overdue.isNotEmpty()) Surface(color = Brand.alert, contentColor = Brand.onAlert, shape = CircleShape, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                stringResource(R.string.overdue_summary, overdue.size, baht(overdue.sumOf { it.need })),
                Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeroStat(stringResource(R.string.unpaid_short), unpaid, "home.unpaid", Modifier.weight(1f))
            HeroStat(stringResource(R.string.income_this_month), income, "home.income", Modifier.weight(1f))
            HeroStat(stringResource(R.string.balance_short), income - unpaid, "home.balance", Modifier.weight(1f))
        }
    }
}

/** แถวรายการ: นับถอยหลังทางซ้าย พื้นหลังเติมสีตามสัดส่วนเงินที่เก็บได้แล้ว */
@Composable
private fun ExpenseRow(e: Expense, d: AppData, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val target = if (!e.paid && e.total > 0) (e.saved.toFloat() / e.total).coerceIn(0f, 1f) else 0f
    val saved = animatedValue("row.${e.id}.fill", target) // กลับจากหน้ารายละเอียดหลังเก็บเงิน สีจะเลื่อนจากค่าเดิมไปค่าใหม่
    val fill = Brand.fill
    val small = MaterialTheme.typography.bodySmall
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth().pressScale(source), interactionSource = source,
    ) {
        Row(
            Modifier.drawBehind { drawRect(fill, size = Size(size.width * saved, size.height)) }.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Countdown(e, today)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DotTag(d.categories.find { it.id == e.categoryId })
                    DotTag(d.priorities.find { it.id == e.priorityId })
                }
                val daily = e.daily(today)
                when {
                    e.overdue(today) -> Text(stringResource(R.string.overdue_short, baht(e.need)), style = small, color = MaterialTheme.colorScheme.error)
                    daily != null -> Text(stringResource(R.string.per_day, baht(daily)), style = small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Money(if (e.paid) e.total else e.unpaid, key = "row.${e.id}.amount")
                if (target > 0f) Text(
                    stringResource(R.string.saved_pct, (target * 100).toInt()),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
