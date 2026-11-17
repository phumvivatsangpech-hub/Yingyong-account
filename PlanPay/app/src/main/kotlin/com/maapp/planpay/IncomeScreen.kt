package com.maapp.planpay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun IncomeScreen() {
    val d = Store.data
    val today = LocalDate.now()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var editing by remember { mutableStateOf<Income?>(null) }

    Screen(
        stringResource(R.string.income),
        fab = { AddFab(stringResource(R.string.add_income)) { editing = Income(name = "", amount = 0, date = today.toString()) } },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
            HeroBlock {
                Text(stringResource(R.string.income_this_month), style = MaterialTheme.typography.labelLarge, color = Brand.onHeaderMuted)
                Money(d.incomeIn(YearMonth.from(today)), MaterialTheme.typography.displayMedium, key = "income.month")
            }
            Column(Modifier.padding(Space.m)) {
                if (d.incomes.isEmpty()) Text(stringResource(R.string.empty_income), color = muted)
                else Group {
                    d.incomes.sortedByDescending { it.date }.forEachIndexed { i, inc ->
                        if (i > 0) RowDivider()
                        GroupRow({ editing = inc }) {
                            Column(Modifier.weight(1f)) {
                                Text(inc.name, style = MaterialTheme.typography.titleMedium)
                                Text(fmtDate(inc.date), style = MaterialTheme.typography.bodySmall, color = muted)
                                if (inc.note.isNotBlank()) Text(inc.note, style = MaterialTheme.typography.bodySmall, color = muted)
                            }
                            Money(inc.amount, color = Brand.good)
                        }
                    }
                }
            }
        }
    }

    editing?.let { inc ->
        val exists = d.incomes.any { it.id == inc.id }
        var name by remember(inc.id) { mutableStateOf(inc.name) }
        var amount by remember(inc.id) { mutableStateOf(inc.amount.takeIf { it > 0 }?.toString() ?: "") }
        var date by remember(inc.id) { mutableStateOf(inc.date) }
        var note by remember(inc.id) { mutableStateOf(inc.note) }
        FormDialog(
            stringResource(R.string.income),
            onDismiss = { editing = null },
            onSave = {
                val n = amount.toIntOrNull() ?: 0
                if (name.isNotBlank() && n > 0) {
                    val new = inc.copy(name = name.trim(), amount = n, date = date, note = note.trim())
                    Store.update(checkStock = false) {
                        copy(incomes = if (exists) incomes.map { if (it.id == inc.id) new else it } else incomes + new)
                    }
                    editing = null
                }
            },
            onDelete = if (exists) ({
                Store.update(checkStock = false) { copy(incomes = incomes.filter { it.id != inc.id }) }
                editing = null
            }) else null,
        ) {
            TextField1(name, { name = it }, stringResource(R.string.name))
            IntField(amount, { amount = it }, stringResource(R.string.amount))
            DateField(stringResource(R.string.date), date, clearable = false) { if (it != null) date = it }
            TextField1(note, { note = it }, stringResource(R.string.note))
        }
    }
}
