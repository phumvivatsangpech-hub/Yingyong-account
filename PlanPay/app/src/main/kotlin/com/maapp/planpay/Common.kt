package com.maapp.planpay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.chrono.ThaiBuddhistChronology
import java.time.format.DateTimeFormatter
import java.util.Locale

fun baht(n: Int): String = "%,d ฿".format(n)

/** ภาษาของวันที่ ตามภาษาของข้อความในแอพ (strings.xml: locale_tag) ไม่ใช่ภาษาของเครื่อง จะได้ไม่ปนไทยกับอังกฤษ */
var dateLocale: Locale = Locale.forLanguageTag("th-TH")

fun fmtDate(iso: String): String {
    val loc = dateLocale
    val f = DateTimeFormatter.ofPattern("d MMM yyyy", loc)
    // ภาษาไทยแสดงปี พ.ศ.
    return LocalDate.parse(iso).format(if (loc.language == "th") f.withChronology(ThaiBuddhistChronology.INSTANCE) else f)
}

/** ชื่อเดือนและปี เช่น "พฤศจิกายน 2569" */
fun fmtMonth(month: java.time.YearMonth): String {
    val f = DateTimeFormatter.ofPattern("MMMM yyyy", dateLocale)
    return month.atDay(1).format(if (dateLocale.language == "th") f.withChronology(ThaiBuddhistChronology.INSTANCE) else f)
}

// ---------- โครงหน้า ----------

/** หน้าจอมาตรฐาน: แถบหัวสีเขียวเข้มเหมือนกันทุกหน้า เนื้อหาบนพื้นกระดาษ */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) = Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
        TopAppBar(
            title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                if (onBack != null) IconButton(onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Brand.header, titleContentColor = Brand.onHeader,
                navigationIconContentColor = Brand.onHeader, actionIconContentColor = Brand.onHeader,
            ),
        )
    },
    floatingActionButton = fab,
    content = content,
)

/** บล็อกตัวเลขหลักของหน้า ต่อเนื่องจากแถบหัว (วางเป็นชิ้นแรกของเนื้อหา ไม่มีขอบซ้ายขวา) */
@Composable
fun HeroBlock(content: @Composable ColumnScope.() -> Unit) = Surface(
    color = Brand.header, contentColor = Brand.onHeader,
    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
    modifier = Modifier.fillMaxWidth(),
) { Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp), content = content) }

/** ตัวเลขรองในบล็อกหลัก: ป้ายเล็กด้านบน ตัวเลขด้านล่าง */
@Composable
fun HeroStat(label: String, value: Int, key: String, modifier: Modifier = Modifier) = Column(modifier) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = Brand.onHeaderMuted, maxLines = 1)
    Money(value, MaterialTheme.typography.titleMedium, Brand.onHeader, key = key)
}

@Composable
fun AddFab(label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick, Modifier.pressScale(source), shape = MaterialTheme.shapes.large,
        containerColor = Brand.accent, contentColor = Brand.onAccent, interactionSource = source,
    ) { Icon(Icons.Default.Add, label) }
}

/** กลุ่มเนื้อหา: หัวข้อ แล้วตามด้วยพื้นขาวมุมโค้ง */
@Composable
fun Group(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) Text(
            title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, bottom = Space.s),
        )
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            // เพิ่มหรือลบแถวข้างใน กลุ่มจะค่อยๆ ยืดหด ไม่กระโดด
            Column(Modifier.animateContentSize(Motion.spec()), content = content)
        }
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
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Button(
        onClick, Modifier.fillMaxWidth().height(52.dp).pressScale(source), enabled = enabled,
        shape = MaterialTheme.shapes.medium, interactionSource = source,
    ) { Text(text, style = MaterialTheme.typography.titleSmall) }
}

/** ปุ่มเลือกแบบเม็ดยา ใช้กับตัวกรองและตัวสลับ สีค่อยๆ เปลี่ยนเมื่อถูกเลือก */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) scheme.onBackground else scheme.surface, Motion.spec(Motion.quick), label = "pillBg")
    val fg by animateColorAsState(if (selected) scheme.background else scheme.onSurface, Motion.spec(Motion.quick), label = "pillFg")
    Surface(
        selected = selected, onClick = onClick, modifier = Modifier.pressScale(source), shape = CircleShape,
        color = bg, contentColor = fg, interactionSource = source,
    ) { Text(text, Modifier.padding(horizontal = 16.dp, vertical = 9.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
}

/** ปุ่มรองขนาดเล็กในเนื้อหา (strong = การเพิ่มของใหม่) มีพื้นสีให้เห็นว่ากดได้ */
@Composable
fun SmallAction(text: String, strong: Boolean = false, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick, modifier = Modifier.pressScale(source), shape = CircleShape,
        color = if (strong) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (strong) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        interactionSource = source,
    ) { Text(text, Modifier.padding(horizontal = 14.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
}

// ---------- ชิ้นส่วนแสดงผล ----------

/**
 * ตัวเลขเงิน: หนากว่าข้อความรอบข้าง ตัวเลขกว้างเท่ากันทุกหลัก
 * ใส่ key เพื่อให้ตัวเลขนับจากค่าเดิมไปค่าใหม่เมื่อข้อมูลเปลี่ยน (key ต้องไม่ซ้ำกันในแอพ)
 */
@Composable
fun Money(
    n: Int,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    key: String? = null,
) = Text(
    baht(if (key == null) n else animatedInt(key, n)), modifier, color = color,
    style = style.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), maxLines = 1,
)

/** หมวดหมู่หรือความสำคัญ: จุดสีที่ผู้ใช้เลือก + ชื่อ */
@Composable
fun DotTag(tag: Tag?, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    if (tag == null) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(Color(tag.color), CircleShape))
        Text(tag.name, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1)
    }
}

/** แถบความคืบหน้า เลื่อนไปค่าใหม่เมื่อค่าเปลี่ยน */
@Composable
fun Bar(fraction: Float, color: Color, track: Color, height: Dp = 8.dp, modifier: Modifier = Modifier) {
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), Motion.spec(Motion.reveal), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxWidth(shown).fillMaxHeight().background(color, CircleShape))
    }
}

/** ตัวเลขนับถอยหลังถึงวันครบกำหนด: ติดลบ = เลยกำหนด, ขีด = ไม่กำหนดวัน, เครื่องหมายถูก = จ่ายแล้ว */
@Composable
fun Countdown(e: Expense, today: LocalDate) {
    val days = e.daysLeft(today)
    Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            e.paid -> Icon(Icons.Default.Check, stringResource(R.string.tab_paid), Modifier.size(28.dp), tint = Brand.good)
            days == null -> Text("–", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.outline)
            else -> {
                val c = if (days < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                Text(days.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = c, maxLines = 1)
                Text(stringResource(R.string.day_unit), style = MaterialTheme.typography.labelSmall, color = if (days < 0) c else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------- ช่องกรอก ----------

@Composable
fun IntField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) =
    OutlinedTextField(
        value, { v -> onChange(v.filter(Char::isDigit).take(9)) }, // 9 หลัก ไม่ล้น Int
        label = { Text(label) }, enabled = enabled, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium, colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
    )

@Composable
fun TextField1(value: String, onChange: (String) -> Unit, label: String, singleLine: Boolean = true) =
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = singleLine,
        shape = MaterialTheme.shapes.medium, colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
    )

/** ช่องกรอกพื้นโปร่ง (ป้ายชื่อบนเส้นขอบจะกลืนกับพื้นหลังทั้งในหน้าและในกล่องโต้ตอบ) */
@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline)

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
    val small = MaterialTheme.typography.bodySmall
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        if (showTitle) Text(stringResource(R.string.links), style = MaterialTheme.typography.titleSmall)
        if (allSoldOut(links)) Text(stringResource(R.string.all_sold_out), color = MaterialTheme.colorScheme.error, style = small)
        links.forEach { l ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable { editing = l }.heightIn(min = 48.dp).padding(vertical = 6.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        l.shop.ifBlank { l.url }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        color = if (l.soldOut) muted else Color.Unspecified,
                        textDecoration = if (l.soldOut) TextDecoration.LineThrough else null,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        if (l.price != null) Text(baht(l.price), style = small, color = muted)
                        if (l.soldOut) Text(stringResource(R.string.sold_out), style = small, color = MaterialTheme.colorScheme.error)
                    }
                }
                SmallAction(stringResource(R.string.open)) { runCatching { uri.openUri(l.url) } }
                Spacer(Modifier.width(6.dp))
                SmallAction(stringResource(if (l.soldOut) R.string.in_stock else R.string.sold_out)) {
                    onChange(links.map { if (it.id == l.id) it.copy(soldOut = !it.soldOut) else it })
                }
            }
        }
        SmallAction(stringResource(R.string.add_link), strong = true) { editing = Link(url = "") }
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
