package com.maapp.planpay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private fun <T> List<T>.swap(a: Int, b: Int) = toMutableList().also { val t = it[a]; it[a] = it[b]; it[b] = t }

/** จัดการหมวดหมู่ หรือ ระดับความสำคัญ (ความสำคัญเรียงลำดับได้) */
@Composable
fun TagsScreen(isPriority: Boolean, back: () -> Unit) {
    val d = Store.data
    val tags = if (isPriority) d.priorities else d.categories
    val set: (List<Tag>) -> Unit = { l -> Store.update { if (isPriority) copy(priorities = l) else copy(categories = l) } }
    var editing by remember { mutableStateOf<Tag?>(null) }

    Screen(
        stringResource(if (isPriority) R.string.priorities else R.string.categories), back,
        fab = { AddFab(stringResource(R.string.add)) { editing = Tag(name = "", color = palette[0]) } },
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Space.m, Space.m, Space.m, 96.dp)) {
            if (tags.isEmpty()) Text(stringResource(R.string.empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else Group {
                tags.forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    GroupRow({ editing = t }) {
                        Box(Modifier.size(20.dp).background(Color(t.color), CircleShape))
                        Text(t.name, Modifier.weight(1f))
                        if (isPriority) {
                            IconButton({ set(tags.swap(i, i - 1)) }, enabled = i > 0) {
                                Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.move_up))
                            }
                            IconButton({ set(tags.swap(i, i + 1)) }, enabled = i < tags.lastIndex) {
                                Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.move_down))
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { t ->
        val exists = tags.any { it.id == t.id }
        var name by remember(t.id) { mutableStateOf(t.name) }
        var color by remember(t.id) { mutableStateOf(t.color) }
        FormDialog(
            stringResource(if (isPriority) R.string.priority else R.string.category),
            onDismiss = { editing = null },
            onSave = {
                if (name.isNotBlank()) {
                    val n = t.copy(name = name.trim(), color = color)
                    set(if (exists) tags.map { if (it.id == t.id) n else it } else tags + n)
                    editing = null
                }
            },
            onDelete = if (exists) ({
                Store.update { if (isPriority) deletePriority(t.id) else deleteCategory(t.id) }
                editing = null
            }) else null,
        ) {
            TextField1(name, { name = it }, stringResource(R.string.name))
            palette.chunked(5).forEachIndexed { row, colors ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                    colors.forEachIndexed { i, c ->
                        val label = stringResource(R.string.color_n, row * 5 + i + 1)
                        Box(
                            Modifier.size(48.dp)
                                .semantics { contentDescription = label }
                                .selectable(color == c, role = Role.RadioButton) { color = c }
                                .padding(4.dp)
                                .background(Color(c), CircleShape)
                                .border(if (color == c) 3.dp else 0.dp, if (color == c) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
