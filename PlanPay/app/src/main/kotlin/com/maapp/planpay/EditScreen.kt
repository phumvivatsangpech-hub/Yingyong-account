package com.maapp.planpay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun EditScreen(id: Long?, go: (String) -> Unit, back: () -> Unit) {
    val d = Store.data
    val old = remember { d.expenses.find { it.id == id } }
    var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var amount by rememberSaveable { mutableStateOf(old?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var cat by rememberSaveable { mutableStateOf(old?.categoryId) }
    var pri by rememberSaveable { mutableStateOf(old?.priorityId) }
    var due by rememberSaveable { mutableStateOf(old?.due) }
    var note by rememberSaveable { mutableStateOf(old?.note ?: "") }
    val computed = old != null && (old.subItems.isNotEmpty() || linkPrice(old.links) != null)

    Screen(stringResource(if (old == null) R.string.add_expense else R.string.edit_expense), back) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            TextField1(name, { name = it }, stringResource(R.string.name))
            IntField(amount, { amount = it }, stringResource(R.string.amount), enabled = !computed)
            if (computed) Text(
                stringResource(R.string.amount_computed),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Group {
                Picker(stringResource(R.string.category), d.categories, cat) { cat = it }
                RowDivider()
                Picker(stringResource(R.string.priority), d.priorities, pri) { pri = it }
                RowDivider()
                DateField(stringResource(R.string.due), due) { due = it }
            }
            TextField1(note, { note = it }, stringResource(R.string.note), singleLine = false)
            PrimaryButton(stringResource(R.string.save), enabled = name.isNotBlank()) {
                // แก้รายการเดิม: ใช้ข้อมูลล่าสุดจาก Store เป็นฐาน ไม่ทับรายการย่อย/รูปที่อาจเปลี่ยนไป
                val base = Store.data.expenses.find { it.id == id } ?: Expense(name = "")
                val e = base.copy(
                    name = name.trim(), amount = amount.toIntOrNull() ?: 0,
                    categoryId = cat, priorityId = pri, due = due, note = note.trim(),
                )
                Store.update { saveExpense(e) }
                back()
                if (old == null) go("detail:${e.id}") // รายการใหม่: ไปใส่รายการย่อยและลิงก์ต่อ
            }
        }
    }
}
