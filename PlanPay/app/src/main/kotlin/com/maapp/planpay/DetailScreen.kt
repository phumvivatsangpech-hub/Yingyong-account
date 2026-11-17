package com.maapp.planpay

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import java.io.File
import java.time.LocalDate

@Composable
fun DetailScreen(id: Long, go: (String) -> Unit, back: () -> Unit) {
    val d = Store.data
    val e = d.expenses.find { it.id == id }
    if (e == null) { // เพิ่งถูกลบ: หน้านี้กำลังเลื่อนออก
        Screen("", back) {}
        return
    }
    val today = LocalDate.now()
    val ctx = LocalContext.current
    val small = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val error = MaterialTheme.colorScheme.error
    var confirmDelete by remember { mutableStateOf(false) }
    var depositOpen by remember { mutableStateOf(false) }
    var sub by remember { mutableStateOf<SubItem?>(null) }

    Screen(e.name, back, actions = {
        IconButton({ go("edit:$id") }) { Icon(Icons.Default.Edit, stringResource(R.string.edit)) }
        IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) }
    }) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            Hero(e, d, today, onDeposit = { depositOpen = true }, onTogglePaid = { Store.update { setPaid(id, !e.paid) } })

            Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.l)) {
                if (e.note.isNotBlank()) Group(stringResource(R.string.note)) { Text(e.note, Modifier.padding(Space.m)) }

                Group(stringResource(R.string.sub_items)) {
                    e.subItems.forEach { s ->
                        Row(
                            Modifier.fillMaxWidth().clickable { sub = s }.heightIn(min = 56.dp).padding(end = Space.m),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(s.done, { Store.update { toggleSub(id, s.id) } })
                            Column(Modifier.weight(1f).padding(vertical = Space.s)) {
                                StrikeText(s.name, s.done)
                                val st = d.stock.find { it.id == s.stockId }
                                if (st != null) {
                                    Text(stringResource(R.string.from_stock, st.name, s.qty, st.unit), style = small, color = muted)
                                    val short = d.planned(st.id) - st.remaining
                                    if (!s.done && short > 0) Text(stringResource(R.string.stock_short, short, st.unit), color = error, style = small)
                                } else {
                                    Text("${baht(s.price)} × ${s.qty}", style = small, color = muted)
                                    if (allSoldOut(s.links)) Text(stringResource(R.string.all_sold_out), color = error, style = small)
                                }
                            }
                            Money(s.total, color = if (s.done) muted else MaterialTheme.colorScheme.onSurface)
                        }
                        RowDivider()
                    }
                    Box(Modifier.padding(horizontal = Space.m, vertical = 4.dp)) {
                        SmallAction(stringResource(R.string.add_sub), strong = true) { sub = SubItem(name = "") }
                    }
                }

                Group(stringResource(R.string.links)) {
                    Column(Modifier.padding(horizontal = Space.m, vertical = 4.dp)) {
                        LinksSection(e.links, showTitle = false) { Store.update { saveExpense(e.copy(links = it)) } }
                    }
                }

                ImagesSection(e)

                if (e.deposits.isNotEmpty()) Group(stringResource(R.string.deposit_history)) {
                    e.deposits.forEachIndexed { i, dep ->
                        if (i > 0) RowDivider()
                        GroupRow {
                            Text(fmtDate(dep.date), Modifier.weight(1f), color = muted)
                            Money(dep.amount)
                            IconButton({ Store.update { saveExpense(e.copy(deposits = e.deposits - dep)) } }) {
                                Icon(Icons.Default.Close, stringResource(R.string.delete))
                            }
                        }
                    }
                }

                OutlinedButton({
                    Store.update { copyNextMonth(id) }
                    Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show()
                }, Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
                    Text(stringResource(R.string.copy_next_month), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    if (confirmDelete) ConfirmDelete(e.name, onDismiss = { confirmDelete = false }, onConfirm = {
        // ไม่ตรวจสต็อกตอนลบ ไม่งั้นรายการซื้อที่เพิ่งลบจะถูกสร้างกลับมาทันที
        Store.update(checkStock = false) { copy(expenses = expenses.filter { it.id != id }) }
        e.images.forEach { File(imagesDir(ctx), it).delete() }
        back()
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

/** ยอดของรายการ กำหนดจ่าย ความคืบหน้าของเงินเก็บ และปุ่มหลักสองปุ่ม */
@Composable
private fun Hero(e: Expense, d: AppData, today: LocalDate, onDeposit: () -> Unit, onTogglePaid: () -> Unit) {
    val small = MaterialTheme.typography.bodySmall
    val days = e.daysLeft(today)
    val haptic = LocalHapticFeedback.current
    val key = "exp.${e.id}"
    // จ่ายแล้ว: ยอดเงินเปลี่ยนเป็นสีสถานะดี พร้อมเครื่องหมายถูกที่วาดตัวเอง (PaidBadge) และแถบที่วิ่งจนเต็ม
    val amountColor by animateColorAsState(if (e.paid) Brand.goodOnHeader else Brand.onHeader, Motion.spec(Motion.reveal), label = "paidColor")
    HeroBlock {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DotTag(d.categories.find { it.id == e.categoryId }, Brand.onHeaderMuted)
                    DotTag(d.priorities.find { it.id == e.priorityId }, Brand.onHeaderMuted)
                }
                Money(e.total, MaterialTheme.typography.displayMedium, amountColor, key = "$key.total")
            }
            PaidBadge(e.paid, Modifier.size(64.dp))
        }
        if (e.qty > 1) Text(stringResource(R.string.packs, e.qty), style = small, color = Brand.onHeaderMuted)
        if (!e.paid && e.unpaid != e.total) Text(
            stringResource(R.string.remaining_to_pay, baht(animatedInt("$key.unpaid", e.unpaid))), color = Brand.onHeader,
        )
        when {
            e.paid -> Text(stringResource(R.string.tab_paid), color = Brand.onHeaderMuted)
            days == null -> {}
            days < 0 -> Surface(color = Brand.alert, contentColor = Brand.onAlert, shape = CircleShape, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    stringResource(R.string.overdue_line, fmtDate(e.due!!), baht(e.need)),
                    Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium,
                )
            }
            else -> Text(stringResource(R.string.due_in, fmtDate(e.due!!), days.toInt()), color = Brand.onHeaderMuted)
        }

        Spacer(Modifier.height(20.dp))
        Bar(if (e.paid) 1f else if (e.total > 0) e.saved.toFloat() / e.total else 0f, Brand.accent, Brand.headerTrack)
        Row(Modifier.padding(top = 6.dp).heightIn(min = 20.dp)) {
            if (!e.paid) {
                Text(
                    stringResource(R.string.saved_of, baht(animatedInt("$key.saved", e.saved)), baht(e.total)),
                    Modifier.weight(1f), style = small, color = Brand.onHeaderMuted,
                )
                val daily = e.daily(today)
                if (daily != null) Text(stringResource(R.string.per_day, baht(animatedInt("$key.daily", daily))), style = small, color = Brand.onHeader)
            }
        }
        Spacer(Modifier.height(Space.m))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            val depositSource = remember { MutableInteractionSource() }
            val paidSource = remember { MutableInteractionSource() }
            if (!e.paid) Button(
                onDeposit, Modifier.weight(1f).height(48.dp).pressScale(depositSource), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.accent, contentColor = Brand.onAccent),
                interactionSource = depositSource,
            ) { Text(stringResource(R.string.add_deposit), style = MaterialTheme.typography.titleSmall) }
            Button(
                {
                    if (!e.paid) haptic.performHapticFeedback(HapticFeedbackType.LongPress) // สั่นเบาๆ หนึ่งครั้งตอนจ่ายสำเร็จ
                    onTogglePaid()
                },
                Modifier.weight(1f).height(48.dp).pressScale(paidSource), shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.onHeader, contentColor = Brand.header),
                interactionSource = paidSource,
            ) { Text(stringResource(if (e.paid) R.string.mark_unpaid else R.string.mark_paid), style = MaterialTheme.typography.titleSmall) }
        }
    }
}
