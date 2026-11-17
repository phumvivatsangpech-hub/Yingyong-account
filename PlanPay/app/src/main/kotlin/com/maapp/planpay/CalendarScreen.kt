package com.maapp.planpay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

private val weekdays = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)

/** ปฏิทินรายเดือน: จุดสีบอกวันที่มีรายจ่ายครบกำหนด แตะวันเพื่อดูรายการของวันนั้น */
@Composable
fun CalendarScreen(go: (String) -> Unit) {
    val d = Store.data
    val today = LocalDate.now()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthText)
    val byDay = remember(d.expenses) { d.expenses.filter { it.due != null }.groupBy { it.due!! } }
    val incomeByDay = remember(d.incomes) { d.incomes.groupBy { it.date } }
    val itemsByDay = remember(d.dayItems) { d.dayItems.groupBy { it.date } }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val ctx = LocalContext.current
    var deleting by remember { mutableStateOf<Expense?>(null) }
    var editingItem by remember { mutableStateOf<DayItem?>(null) }
    fun move(months: Long) {
        val m = month.plusMonths(months)
        monthText = m.toString()
        selectedText = (if (m == YearMonth.from(today)) today else m.atDay(1)).toString()
    }

    Screen(
        stringResource(R.string.calendar),
        fab = { AddFab(stringResource(R.string.add_day_item)) { editingItem = DayItem(name = "", amount = 0, date = selectedText) } },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            HeroBlock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.prev_month)) }
                    Text(fmtMonth(month), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                    IconButton({ move(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_month)) }
                }
                Spacer(Modifier.height(Space.s))
                Text(stringResource(R.string.due_this_month), style = MaterialTheme.typography.labelLarge, color = Brand.onHeaderMuted)
                Money(d.dueUnpaidIn(month), MaterialTheme.typography.displaySmall, key = "calendar.month")
            }

            Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.l)) {
                Group {
                    Column(Modifier.padding(horizontal = 6.dp, vertical = 10.dp)) {
                        Row {
                            weekdays.forEach {
                                Text(
                                    it.getDisplayName(TextStyle.SHORT, dateLocale), Modifier.weight(1f).padding(bottom = 4.dp),
                                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = muted,
                                )
                            }
                        }
                        // เปลี่ยนเดือน: ตารางเลื่อนไปทางเดียวกับทิศที่กด
                        AnimatedContent(
                            month,
                            transitionSpec = {
                                val dir = targetState.compareTo(initialState)
                                (slideInHorizontally(Motion.spec()) { it / 4 * dir } + fadeIn(Motion.spec())) togetherWith
                                    (slideOutHorizontally(Motion.spec()) { -it / 4 * dir } + fadeOut(Motion.spec(Motion.quick)))
                            },
                            label = "month",
                        ) { m ->
                            Column {
                                monthCells(m).chunked(7).forEach { week ->
                                    Row {
                                        week.forEach { day ->
                                            if (day == null) Spacer(Modifier.weight(1f))
                                            else DayCell(
                                                day, day == today, day.toString() == selectedText,
                                                byDay[day.toString()].orEmpty(), itemsByDay[day.toString()].orEmpty().count { !it.done },
                                                day.toString() in incomeByDay, d.priorities,
                                            ) { selectedText = day.toString() }
                                        }
                                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.calendar_legend), style = MaterialTheme.typography.bodySmall, color = muted, modifier = Modifier.padding(horizontal = 4.dp))

                val dayExpenses = byDay[selectedText].orEmpty()
                val dayIncomes = incomeByDay[selectedText].orEmpty()
                Group(fmtDate(selectedText)) {
                    // ต้องเก็บเท่าไรในวันที่เลือก ตามแผนที่คิดจากวันนี้ (วันที่ผ่านมาแล้วไม่แสดง)
                    val selected = LocalDate.parse(selectedText)
                    if (selected >= today) {
                        GroupRow {
                            Text(stringResource(R.string.save_on_day), Modifier.weight(1f))
                            Money(d.saveOn(selected, today), MaterialTheme.typography.titleLarge, key = "calendar.save")
                        }
                        RowDivider()
                    }
                    if (dayExpenses.isEmpty()) Text(stringResource(R.string.no_items_day), Modifier.padding(Space.m), color = muted)
                    dayExpenses.forEachIndexed { i, e ->
                        if (i > 0) RowDivider()
                        // แตะแถว = ดูรายละเอียด ปุ่มท้ายแถว = แก้ไข / ลบ
                        Row(
                            Modifier.fillMaxWidth().clickable { go("detail:${e.id}") }.heightIn(min = 56.dp).padding(start = Space.m, top = 10.dp, bottom = 10.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(e.name, style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DotTag(d.categories.find { it.id == e.categoryId })
                                    DotTag(d.priorities.find { it.id == e.priorityId })
                                }
                                if (e.paid) Text(stringResource(R.string.tab_paid), style = MaterialTheme.typography.bodySmall, color = Brand.good)
                            }
                            Money(if (e.paid) e.total else e.unpaid, color = if (e.paid) muted else Color.Unspecified)
                            IconButton({ go("edit:${e.id}") }) { Icon(Icons.Default.Edit, stringResource(R.string.edit), tint = muted) }
                            IconButton({ deleting = e }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = muted) }
                        }
                    }
                    dayIncomes.forEach { inc ->
                        RowDivider()
                        GroupRow {
                            Column(Modifier.weight(1f)) {
                                Text(inc.name, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.income), style = MaterialTheme.typography.bodySmall, color = Brand.good)
                            }
                            Money(inc.amount, color = Brand.good)
                        }
                    }
                }

                // รายการเฉพาะวัน: แยกจากรายจ่ายหลัก ติ๊กเมื่อจ่ายแล้ว แตะแถวเพื่อแก้ไขหรือลบ
                val dayItems = itemsByDay[selectedText].orEmpty()
                Group(stringResource(R.string.day_items)) {
                    dayItems.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { editingItem = item }.heightIn(min = 56.dp).padding(end = Space.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(item.done, { done ->
                                Store.update(checkStock = false) { copy(dayItems = this.dayItems.map { if (it.id == item.id) it.copy(done = done) else it }) }
                            })
                            StrikeText(item.name, item.done, Modifier.weight(1f))
                            Money(item.amount, color = if (item.done) muted else Color.Unspecified)
                        }
                        RowDivider()
                    }
                    Box(Modifier.padding(horizontal = Space.m, vertical = 4.dp)) {
                        SmallAction(stringResource(R.string.add_day_item), strong = true) {
                            editingItem = DayItem(name = "", amount = 0, date = selectedText)
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { item ->
        val exists = d.dayItems.any { it.id == item.id }
        var name by remember(item.id) { mutableStateOf(item.name) }
        var amount by remember(item.id) { mutableStateOf(item.amount.takeIf { it > 0 }?.toString() ?: "") }
        FormDialog(
            "${stringResource(R.string.day_item)} ${fmtDate(item.date)}",
            onDismiss = { editingItem = null },
            onSave = {
                val n = amount.toIntOrNull() ?: 0
                if (name.isNotBlank() && n > 0) {
                    val new = item.copy(name = name.trim(), amount = n)
                    Store.update(checkStock = false) {
                        copy(dayItems = if (exists) dayItems.map { if (it.id == item.id) new else it } else dayItems + new)
                    }
                    editingItem = null
                }
            },
            onDelete = if (exists) ({
                Store.update(checkStock = false) { copy(dayItems = dayItems.filter { it.id != item.id }) }
                editingItem = null
            }) else null,
        ) {
            TextField1(name, { name = it }, stringResource(R.string.name))
            IntField(amount, { amount = it }, stringResource(R.string.amount))
        }
    }

    deleting?.let { e ->
        ConfirmDelete(e.name, onDismiss = { deleting = null }, onConfirm = {
            deleteExpense(ctx, e)
            deleting = null
        })
    }
}

@Composable
private fun RowScope.DayCell(
    day: LocalDate, isToday: Boolean, isSelected: Boolean,
    expenses: List<Expense>, openItems: Int, hasIncome: Boolean, priorities: List<Tag>, onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val desc = stringResource(R.string.calendar_day_desc, fmtDate(day.toString()), expenses.size + openItems)
    Column(
        Modifier.weight(1f).height(56.dp).clip(MaterialTheme.shapes.small)
            .selectable(isSelected, onClick = onClick)
            .semantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(34.dp)
                .background(if (isSelected) scheme.onBackground else Color.Transparent, CircleShape)
                .then(if (isToday && !isSelected) Modifier.border(1.5.dp, scheme.primary, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.dayOfMonth.toString(), color = if (isSelected) scheme.background else scheme.onSurface,
                style = MaterialTheme.typography.bodyMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
        // จุด = รายจ่ายที่ครบกำหนด (สีตามความสำคัญ จ่ายแล้วเป็นสีจาง) จุดเทา = รายการเฉพาะวันที่ยังไม่ติ๊ก ขีดเขียว = รายรับ รวมไม่เกิน 3 จุด
        Row(Modifier.height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            val dots = expenses.map { e ->
                if (e.paid) scheme.outlineVariant else priorities.find { it.id == e.priorityId }?.let { Color(it.color) } ?: scheme.onSurfaceVariant
            } + List(openItems) { scheme.outline }
            dots.take(3).forEach { Box(Modifier.size(5.dp).background(it, CircleShape)) }
            if (hasIncome) Box(Modifier.size(width = 9.dp, height = 3.dp).background(Brand.good, CircleShape))
        }
    }
}
