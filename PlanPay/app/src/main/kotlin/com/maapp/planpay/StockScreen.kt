package com.maapp.planpay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun StockScreen(go: (String) -> Unit, back: () -> Unit) {
    val d = Store.data
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var refill by remember { mutableStateOf<StockItem?>(null) }

    Screen(
        stringResource(R.string.stock), back,
        fab = { AddFab(stringResource(R.string.add_stock)) { go("stockEdit") } },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Space.m, Space.m, Space.m, 96.dp)) {
            if (d.stock.isEmpty()) Text(stringResource(R.string.empty), color = muted)
            else Group {
                d.stock.forEachIndexed { i, s ->
                    if (i > 0) RowDivider()
                    GroupRow({ go("stockEdit:${s.id}") }) {
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.stock_remaining, s.remaining, s.unit),
                                color = if (d.isLow(s)) MaterialTheme.colorScheme.error else muted,
                            )
                            val p = d.planned(s.id)
                            if (p > 0) Text(stringResource(R.string.stock_planned, p, s.unit), style = MaterialTheme.typography.bodySmall, color = muted)
                        }
                        SmallAction(stringResource(R.string.refill), strong = true) { refill = s }
                    }
                }
            }
        }
    }

    refill?.let { s ->
        var n by remember(s.id) { mutableStateOf(s.unitsPerPack.toString()) }
        FormDialog("${stringResource(R.string.refill)} ${s.name}", onDismiss = { refill = null }, onSave = {
            val add = n.toIntOrNull() ?: 0
            if (add > 0) {
                Store.update { copy(stock = stock.map { if (it.id == s.id) it.copy(remaining = it.remaining + add) else it }) }
                refill = null
            }
        }) { IntField(n, { n = it }, stringResource(R.string.refill_amount)) }
    }
}

@Composable
fun StockEditScreen(id: Long?, back: () -> Unit) {
    val d = Store.data
    val old = remember { d.stock.find { it.id == id } }
    var name by rememberSaveable { mutableStateOf(old?.name ?: "") }
    var remaining by rememberSaveable { mutableStateOf(old?.remaining?.toString() ?: "") }
    var unit by rememberSaveable { mutableStateOf(old?.unit ?: "") }
    var alertAt by rememberSaveable { mutableStateOf(old?.alertAt?.toString() ?: "") }
    var perPack by rememberSaveable { mutableStateOf(old?.unitsPerPack?.toString() ?: "") }
    var cat by rememberSaveable { mutableStateOf(old?.categoryId) }
    var pri by rememberSaveable { mutableStateOf(old?.priorityId) }
    var links by remember { mutableStateOf(old?.links ?: emptyList()) }
    var confirmDelete by remember { mutableStateOf(false) }

    Screen(
        stringResource(if (old == null) R.string.add_stock else R.string.edit_stock), back,
        actions = { if (old != null) IconButton({ confirmDelete = true }) { Icon(Icons.Default.Delete, stringResource(R.string.delete)) } },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Space.m),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            TextField1(name, { name = it }, stringResource(R.string.name))
            IntField(remaining, { remaining = it }, stringResource(R.string.remaining))
            TextField1(unit, { unit = it }, stringResource(R.string.unit))
            IntField(alertAt, { alertAt = it }, stringResource(R.string.alert_at))
            IntField(perPack, { perPack = it }, stringResource(R.string.units_per_pack))
            Group {
                Picker(stringResource(R.string.category), d.categories, cat) { cat = it }
                RowDivider()
                Picker(stringResource(R.string.priority), d.priorities, pri) { pri = it }
            }
            Group(stringResource(R.string.links)) {
                Column(Modifier.padding(horizontal = Space.m, vertical = 4.dp)) {
                    LinksSection(links, showTitle = false) { links = it }
                }
            }
            PrimaryButton(stringResource(R.string.save), enabled = name.isNotBlank()) {
                val s = (old ?: StockItem(name = "")).copy(
                    name = name.trim(), remaining = remaining.toIntOrNull() ?: 0, unit = unit.trim(),
                    alertAt = alertAt.toIntOrNull() ?: 0, unitsPerPack = maxOf(1, perPack.toIntOrNull() ?: 1),
                    categoryId = cat, priorityId = pri, links = links,
                )
                Store.update { copy(stock = if (old == null) stock + s else stock.map { if (it.id == s.id) s else it }) }
                back()
            }
        }
    }

    if (confirmDelete && old != null) ConfirmDelete(old.name, onDismiss = { confirmDelete = false }, onConfirm = {
        Store.update { deleteStock(old.id) }
        back()
    })
}
