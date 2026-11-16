package com.maapp.planpay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.util.Locale

fun baht(n: Int): String = "%,d ฿".format(n)

fun fmtDate(iso: String): String {
    val loc = Locale.getDefault()
    val f = DateTimeFormatter.ofPattern("d MMM yyyy", loc)
    // ภาษาไทยแสดงปี พ.ศ.
    return LocalDate.parse(iso).format(if (loc.language == "th") f.withChronology(ThaiBuddhistChronology.INSTANCE) else f)
}

// ---------- โครงหน้า ----------

/** หน้าจอมาตรฐาน: หัวข้อใหญ่ชิดซ้ายที่ย่อลงเมื่อเลื่อน */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bg = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = Modifier.nestedScroll(behavior.nestedScrollConnection),
        containerColor = bg,
        topBar = {
            LargeTopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = actions,
                scrollBehavior = behavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, scrolledContainerColor = bg),
            )
        },
        floatingActionButton = fab,
        content = content,
    )
}

@Composable
fun AddFab(label: String, onClick: () -> Unit) = FloatingActionButton(
    onClick, shape = CircleShape,
    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
) { Icon(Icons.Default.Add, label) }

/** การ์ดกลุ่มแบบ inset grouped list: หัวข้อเล็กด้านบน เนื้อหาในการ์ดมุมโค้ง */
@Composable
fun Group(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) Text(
            title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Space.m, bottom = 6.dp),
        )
        Surface(
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth(),
        ) { Column(content = content) }
    }
}

@Composable
fun RowDivider() = HorizontalDivider(Modifier.padding(start = Space.m), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
fun GroupRow(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) = Row(
    Modifier.fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .heightIn(min = 56.dp)
        .padding(horizontal = Space.m, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    content = content,
)

/** แถวที่แตะแล้วไปหน้าอื่นหรือเปิดตัวเลือก: ชื่อทางซ้าย ค่าปัจจุบันทางขวา */
@Composable
fun NavRow(label: String, value: String = "", onClick: () -> Unit) = GroupRow(onClick) {
    Text(label, Modifier.weight(1f))
    if (value.isNotEmpty()) Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) =
    Button(onClick, Modifier.fillMaxWidth().height(52.dp), enabled = enabled, shape = MaterialTheme.shapes.medium) { Text(text) }

// ---------- ชิ้นส่วนแสดงผล ----------

/** ตัวเลขเงิน: หนากว่าข้อความรอบข้าง ตัวเลขกว้างเท่ากันทุกหลัก */
@Composable
fun Money(
    n: Int,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) = Text(baht(n), modifier, color = color, style = style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), maxLines = 1)

@Composable
fun TagChip(tag: Tag?) {
    if (tag == null) return
    val c = Color(tag.color)
    Surface(color = c, shape = RoundedCornerShape(50)) {
        Text(
            tag.name,
            color = if (c.luminance() > 0.5f) Color.Black else Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
fun DueLine(e: Expense, today: LocalDate) {
    val days = e.daysLeft(today) ?: return
    val date = fmtDate(e.due!!)
    val small = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when {
        e.paid -> Text(stringResource(R.string.due_on, date), style = small, color = muted)
        days < 0 -> Text(stringResource(R.string.overdue_line, date, baht(e.need)), color = MaterialTheme.colorScheme.error, style = small)
        else -> Text(stringResource(R.string.due_line, date, days.toInt(), baht(e.daily(today) ?: 0)), style = small, color = muted)
    }
}

// ---------- ช่องกรอก ----------

@Composable
fun IntField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) =
    OutlinedTextField(
        value, { v -> onChange(v.filter(Char::isDigit).take(9)) }, // 9 หลัก ไม่ล้น Int
        label = { Text(label) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
    )

@Composable
fun TextField1(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) =
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = singleLine,
        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
    )

@Composable
fun Picker(label: String, options: List<Tag>, selected: Long?, onSelect: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val none = stringResource(R.string.none)
    Box {
        NavRow(label, options.find { it.id == selected }?.name ?: none) { open = true }
        DropdownMenu(open, { open = false }) {
            DropdownMenuItem({ Text(none) }, { onSelect(null); open = false })
            options.forEach { o -> DropdownMenuItem({ Text(o.name) }, { onSelect(o.id); open = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, iso: String?, clearable: Boolean = true, onChange: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    GroupRow({ open = true }) {
        Text(label, Modifier.weight(1f))
        Text(iso?.let(::fmtDate) ?: stringResource(R.string.none), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (iso != null && clearable) IconButton({ onChange(null) }) { Icon(Icons.Default.Close, stringResource(R.string.clear)) }
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
    }
    if (open) {
        val start = iso?.let(LocalDate::parse) ?: LocalDate.now()
        val state = rememberDatePickerState(start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                    open = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton({ open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state) }
    }
}

// ---------- กล่องโต้ตอบ ----------

@Composable
fun FormDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.s), content = content) },
    confirmButton = { TextButton(onSave) { Text(stringResource(R.string.save)) } },
    dismissButton = {
        Row {
            if (onDelete != null) TextButton(onDelete) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            TextButton(onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    },
)

@Composable
fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit, destructive: Boolean = true) = AlertDialog(
    onDismissRequest = onDismiss,
    text = { Text(text) },
    confirmButton = {
        TextButton(onConfirm) { Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else Color.Unspecified) }
    },
    dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
)

@Composable
fun ConfirmDelete(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) =
    ConfirmDialog(stringResource(R.string.confirm_delete, name), stringResource(R.string.delete), onConfirm, onDismiss)

// ---------- ลิงก์สินค้า ----------

/** รายการลิงก์สินค้า ใช้ร่วมกันทั้งรายการหลัก รายการย่อย และของใช้ */
@Composable
fun LinksSection(links: List<Link>, showTitle: Boolean = true, onChange: (List<Link>) -> Unit) {
    val uri = LocalUriHandler.current
    var editing by remember { mutableStateOf<Link?>(null) }
    val soldOut = stringResource(R.string.sold_out)
    val small = MaterialTheme.typography.bodySmall
    Column {
        if (showTitle) Text(stringResource(R.string.links), style = MaterialTheme.typography.titleSmall)
        if (allSoldOut(links)) Text(stringResource(R.string.all_sold_out), color = MaterialTheme.colorScheme.error, style = small)
        links.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { editing = l }.heightIn(min = 48.dp).padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        l.shop.ifBlank { l.url }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textDecoration = if (l.soldOut) TextDecoration.LineThrough else null,
                    )
                    val sub = listOfNotNull(l.price?.let(::baht), soldOut.takeIf { l.soldOut }).joinToString(" · ")
                    if (sub.isNotEmpty()) Text(sub, style = small, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton({ runCatching { uri.openUri(l.url) } }) { Text(stringResource(R.string.open)) }
                TextButton({ onChange(links.map { if (it.id == l.id) it.copy(soldOut = !it.soldOut) else it }) }) {
                    Text(stringResource(if (l.soldOut) R.string.in_stock else R.string.sold_out))
                }
            }
        }
        TextButton({ editing = Link(url = "") }) { Text(stringResource(R.string.add_link)) }
    }
    editing?.let { l ->
        val exists = links.any { it.id == l.id }
        var url by remember(l.id) { mutableStateOf(l.url) }
        var shop by remember(l.id) { mutableStateOf(l.shop) }
        var price by remember(l.id) { mutableStateOf(l.price?.toString() ?: "") }
        FormDialog(
            stringResource(R.string.links),
            onDismiss = { editing = null },
            onSave = {
                if (url.isNotBlank()) {
                    // ข้อความแชร์จากแอพร้านค้ามักมีคำอื่นปนมา ดึงเฉพาะ URL
                    val clean = Regex("""https?://\S+""").find(url)?.value ?: ("https://" + url.trim())
                    val n = l.copy(url = clean, shop = shop.trim(), price = price.toIntOrNull())
                    onChange(if (exists) links.map { if (it.id == l.id) n else it } else links + n)
                    editing = null
                }
            },
            onDelete = if (exists) ({ onChange(links.filter { it.id != l.id }); editing = null }) else null,
        ) {
            TextField1(url, { url = it }, stringResource(R.string.url), singleLine = false)
            TextField1(shop, { shop = it }, stringResource(R.string.shop))
            IntField(price, { price = it }, stringResource(R.string.price))
        }
    }
}
