package com.maapp.planpay

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun DetailScreen(id: Long, go: (String) -> Unit, back: () -> Unit) {
    val d = Store.data
    val e = d.expenses.find { it.id == id }
    if (e == null) {
        LaunchedEffect(Unit) { back() }
        return
    }
    val today = LocalDate.now()
    val ctx = LocalContext.current
    val small = MaterialTheme.typography.bodySmall
    val error = MaterialTheme.colorScheme.error
    var confirmDelete by remember { mutableStateOf(false) }
    var depositOpen by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf<SubItem?>(null) }

    Screen(e.name, back, actions = {
        IconButton({ go("edit:$id") }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
        IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
    }) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(baht(e.total), style = MaterialTheme.typography.headlineMedium)
            if (e.qty > 1) Text(stringResource(R.string.packs, e.qty), style = small)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TagChip(d.categories.find { it.id == e.categoryId })
                TagChip(d.priorities.find { it.id == e.priorityId })
            }
            DueLine(e, today)
            if (e.note.isNotBlank()) Text(e.note)

            // เก็บเงิน
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.saved_of, baht(e.saved), baht(e.total)), style = MaterialTheme.typography.titleSmall)
                    LinearProgressIndicator(
                        progress = { if (e.total > 0) (e.saved.toFloat() / e.total).coerceIn(0f, 1f) else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    e.deposits.forEach { dep ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${fmtDate(dep.date)}   ${baht(dep.amount)}", Modifier.weight(1f))
                            IconButton({ Store.update { saveExpense(e.copy(deposits = e.deposits - dep)) } }) {
                                Icon(Icons.Default.Close, stringResource(R.string.delete))
                            }
                        }
                    }
                    TextButton({ depositOpen = true }) { Text(stringResource(R.string.add_deposit)) }
                }
            }

            // รายการย่อย
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.sub_items), style = MaterialTheme.typography.titleSmall)
                    e.subItems.forEach { s ->
                        Row(Modifier.fillMaxWidth().clickable { sub = s }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(s.done, { Store.update { toggleSub(id, s.id) } })
                            Column(Modifier.weight(1f)) {
                                Text(s.name, textDecoration = if (s.done) TextDecoration.LineThrough else null)
                                val st = d.stock.find { it.id == s.stockId }
                                if (st != null) {
                                    Text(stringResource(R.string.from_stock, st.name, s.qty, st.unit), style = small)
                                    val short = d.planned(st.id) - st.remaining
                                    if (!s.done && short > 0) Text(stringResource(R.string.stock_short, short, st.unit), color = error, style = small)
                                } else {
                                    Text("${baht(s.price)} × ${s.qty}", style = small)
                                    if (allSoldOut(s.links)) Text(stringResource(R.string.all_sold_out), color = error, style = small)
                                }
                            }
                            Text(baht(s.total))
                        }
                    }
                    TextButton({ sub = SubItem(name = "") }) { Text(stringResource(R.string.add_sub)) }
                }
            }

            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    LinksSection(e.links) { Store.update { saveExpense(e.copy(links = it)) } }
                }
            }

            Button({ Store.update { setPaid(id, !e.paid) } }, Modifier.fillMaxWidth()) {
                Text(stringResource(if (e.paid) R.string.mark_unpaid else R.string.mark_paid))
            }
            OutlinedButton({
                Store.update { copyNextMonth(id) }
                Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
            }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.copy_next_month)) }
        }
    }

    if (confirmDelete) ConfirmDelete(e.name, onDismiss = { confirmDelete = false }, onConfirm = {
        // ไม่ตรวจสต็อกตอนลบ ไม่งั้นรายการซื้อที่เพิ่งลบจะถูกสร้างกลับมาทันที
        Store.update(checkStock = false) { copy(expenses = expenses.filter { it.id != id }) }
    })

    if (depositOpen) {
        var amount by rememberSaveable { mutableStateOf("") }
        FormDialog(stringResource(R.string.add_deposit), onDismiss = { depositOpen = false }, onSave = {
            val n = amount.toIntOrNull() ?: 0
            if (n > 0) {
                Store.update { saveExpense(e.copy(deposits = e.deposits + Deposit(amount = n, date = today.toString()))) }
                depositOpen = false
            }
        }) { IntField(amount, { amount = it }, stringResource(R.string.deposit_amount)) }
    }

    sub?.let { s ->
        val exists = e.subItems.any { it.id == s.id }
        var name by remember(s.id) { mutableStateOf(s.name) }
        var price by remember(s.id) { mutableStateOf(s.unitPrice.takeIf { it > 0 }?.toString() ?: "") }
        var qty by remember(s.id) { mutableStateOf(s.qty.toString()) }
        var stockId by remember(s.id) { mutableStateOf(s.stockId) }
        var links by remember(s.id) { mutableStateOf(s.links) }
        FormDialog(
            stringResource(R.string.sub_items),
            onDismiss = { sub = null },
            onSave = {
                if (name.isNotBlank()) {
                    val n = s.copy(
                        name = name.trim(), unitPrice = price.toIntOrNull() ?: 0, qty = maxOf(1, qty.toIntOrNull() ?: 1),
                        stockId = stockId, links = if (stockId == null) links else emptyList(),
                    )
                    Store.update { saveSub(id, n) }
                    sub = null
                }
            },
            onDelete = if (exists) ({ Store.update { deleteSub(id, s.id) }; sub = null }) else null,
        ) {
            TextField1(name, { name = it }, stringResource(R.string.name))
            if (d.stock.isNotEmpty()) Picker(stringResource(R.string.use_stock), d.stock.map { Tag(it.id, it.name, 0) }, stockId) { stockId = it }
            if (stockId == null) IntField(price, { price = it }, stringResource(R.string.unit_price))
            IntField(qty, { qty = it }, stringResource(R.string.qty))
            if (stockId == null) LinksSection(links) { links = it }
        }
    }
}
