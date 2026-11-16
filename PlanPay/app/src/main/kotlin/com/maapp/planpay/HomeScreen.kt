package com.maapp.planpay

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
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
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(Space.m, Space.s, Space.m, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Summary(d, today) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(!showPaid, { showPaid = false }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.tab_unpaid)) }
                    SegmentedButton(showPaid, { showPaid = true }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.tab_paid)) }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    FilterChip(cat == null, { cat = null }, { Text(stringResource(R.string.all)) })
                    d.categories.forEach { c -> FilterChip(cat == c.id, { cat = c.id }, { Text(c.name) }) }
                }
            }
            if (shown.isEmpty()) item {
                Text(stringResource(R.string.empty), Modifier.padding(Space.m), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(shown, key = { it.id }) { e -> ExpenseRow(e, d, today) { go("detail:${e.id}") } }
        }
    }
}

@Composable
private fun Summary(d: AppData, today: LocalDate) {
    val open = d.expenses.filter { !it.paid }
    val overdue = open.filter { it.overdue(today) }
    val unpaid = open.sumOf { it.unpaid }
    val income = d.incomeIn(YearMonth.from(today))
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Group {
        Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.total_unpaid), style = MaterialTheme.typography.labelLarge, color = muted)
            Money(unpaid, MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.save_per_day_total, baht(open.sumOf { it.daily(today) ?: 0 })), color = muted)
            if (overdue.isNotEmpty()) Text(
                stringResource(R.string.overdue_summary, overdue.size, baht(overdue.sumOf { it.need })),
                color = MaterialTheme.colorScheme.error,
            )
        }
        RowDivider()
        GroupRow {
            Text(stringResource(R.string.income_this_month), Modifier.weight(1f))
            Money(income)
        }
        RowDivider()
        GroupRow {
            Text(stringResource(R.string.balance_after), Modifier.weight(1f))
            // ติดลบมีเครื่องหมายลบนำหน้า ไม่ได้บอกด้วยสีอย่างเดียว
            Money(income - unpaid, color = if (income >= unpaid) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ExpenseRow(e: Expense, d: AppData, today: LocalDate, onClick: () -> Unit) {
    val pri = d.priorities.find { it.id == e.priorityId }
    Surface(
        onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(pri?.let { Color(it.color) } ?: Color.Transparent, RoundedCornerShape(50)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.name, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TagChip(d.categories.find { it.id == e.categoryId })
                    TagChip(pri)
                }
                DueLine(e, today)
            }
            Money(if (e.paid) e.total else e.unpaid)
        }
    }
}
