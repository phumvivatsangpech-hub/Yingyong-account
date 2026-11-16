package com.maapp.planpay

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicLong

object Ids {
    private val n = AtomicLong(System.currentTimeMillis())
    fun next() = n.incrementAndGet()
}

/** สีที่เลือกได้ของหมวดหมู่และความสำคัญ */
val palette = listOf(
    0xFFD32F2F, 0xFFF57C00, 0xFFFBC02D, 0xFF388E3C, 0xFF00897B,
    0xFF1976D2, 0xFF5E35B1, 0xFFC2185B, 0xFF6D4C41, 0xFF546E7A,
)

/** หมวดหมู่ หรือ ระดับความสำคัญ (ลำดับความสำคัญ = ตำแหน่งใน list) */
@Serializable
data class Tag(val id: Long = Ids.next(), val name: String, val color: Long)

@Serializable
data class Link(
    val id: Long = Ids.next(),
    val url: String,
    val shop: String = "",
    val price: Int? = null,
    val soldOut: Boolean = false,
)

@Serializable
data class SubItem(
    val id: Long = Ids.next(),
    val name: String,
    val unitPrice: Int = 0,
    val qty: Int = 1,
    val done: Boolean = false,
    val stockId: Long? = null, // ใช้ของจากสต็อก qty หน่วย (ไม่คิดเงิน)
    val links: List<Link> = emptyList(),
)

@Serializable
data class Deposit(val id: Long = Ids.next(), val amount: Int, val date: String)

@Serializable
data class Expense(
    val id: Long = Ids.next(),
    val name: String,
    val amount: Int = 0,
    val qty: Int = 1,
    val categoryId: Long? = null,
    val priorityId: Long? = null,
    val due: String? = null, // yyyy-MM-dd
    val note: String = "",
    val paid: Boolean = false,
    val subItems: List<SubItem> = emptyList(),
    val links: List<Link> = emptyList(),
    val deposits: List<Deposit> = emptyList(),
    val restockOf: Long? = null, // รายการซื้อของเข้าสต็อกชิ้นนี้
    val images: List<String> = emptyList(), // ชื่อไฟล์ในโฟลเดอร์ images ของแอพ
)

@Serializable
data class Income(val id: Long = Ids.next(), val name: String, val amount: Int, val date: String, val note: String = "")

@Serializable
data class StockItem(
    val id: Long = Ids.next(),
    val name: String,
    val remaining: Int = 0,
    val unit: String = "",
    val alertAt: Int = 0,
    val unitsPerPack: Int = 1,
    val categoryId: Long? = null,
    val priorityId: Long? = null,
    val links: List<Link> = emptyList(),
)

@Serializable
data class AppData(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Tag> = emptyList(),
    val priorities: List<Tag> = emptyList(),
    val stock: List<StockItem> = emptyList(),
    val incomes: List<Income> = emptyList(),
)

// ---------- ไฟล์ข้อมูล / ไฟล์สำรอง ----------

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun encodeData(d: AppData): String = json.encodeToString(AppData.serializer(), d)

/** อ่านไฟล์ข้อมูลของแอพเอง ช่องที่ไม่มี (ไฟล์จากเวอร์ชันเก่า) ใช้ค่าเริ่มต้น */
fun decodeData(text: String): AppData = json.decodeFromString(AppData.serializer(), text)

/** อ่านไฟล์สำรองจากภายนอก: ต้องมีโครงของไฟล์สำรองจริง ไม่งั้น JSON อะไรก็ได้จะกลายเป็นข้อมูลว่างแล้วทับของเดิม */
fun decodeBackup(text: String): AppData {
    val el = json.parseToJsonElement(text)
    require(el is JsonObject && "expenses" in el && "categories" in el && "priorities" in el) { "not a backup file" }
    return json.decodeFromJsonElement(AppData.serializer(), el)
}

// ---------- คำนวณ ----------

fun AppData.incomeIn(month: YearMonth) = incomes.filter { it.date.startsWith(month.toString()) }.sumOf { it.amount }

/** ยอดค้างจ่ายแยกตามหมวดหมู่หรือความสำคัญ เรียงจากมากไปน้อย (null = ไม่ระบุ) */
fun AppData.unpaidBy(tags: List<Tag>, key: (Expense) -> Long?): List<Pair<Tag?, Int>> =
    expenses.filter { it.unpaid > 0 }
        .groupBy { e -> tags.find { it.id == key(e) } }
        .map { (tag, list) -> tag to list.sumOf { it.unpaid } }
        .sortedByDescending { it.second }

/** ราคาของลิงก์แรกที่ยังไม่หมด ถ้าหมดทุกลิงก์ใช้ราคาล่าสุดที่รู้ */
fun linkPrice(links: List<Link>): Int? =
    (links.firstOrNull { !it.soldOut && it.price != null } ?: links.lastOrNull { it.price != null })?.price

fun allSoldOut(links: List<Link>) = links.isNotEmpty() && links.all { it.soldOut }

val SubItem.price get() = linkPrice(links) ?: unitPrice
val SubItem.total get() = if (stockId != null) 0 else price * qty

val Expense.total get() = if (subItems.isNotEmpty()) subItems.sumOf { it.total } else (linkPrice(links) ?: amount) * qty
val Expense.unpaid
    get() = when {
        paid -> 0
        subItems.isNotEmpty() -> subItems.filter { !it.done }.sumOf { it.total }
        else -> total
    }
val Expense.saved get() = deposits.sumOf { it.amount }

/** เงินที่ยังต้องหาเพิ่ม: ไม่เกินยอดที่ยังไม่จ่าย และหักเงินที่เก็บไว้แล้ว */
val Expense.need get() = maxOf(0, minOf(total - saved, unpaid))

fun Expense.daysLeft(today: LocalDate): Long? = due?.let { ChronoUnit.DAYS.between(today, LocalDate.parse(it)) }
fun Expense.overdue(today: LocalDate) = !paid && (daysLeft(today) ?: 0) < 0

/** ต้องเก็บวันละเท่าไร (ปัดขึ้น) null = ไม่มีวันครบกำหนด จ่ายแล้ว หรือเลยกำหนด */
fun Expense.daily(today: LocalDate): Int? {
    val d = daysLeft(today) ?: return null
    if (paid || d < 0) return null
    val n = maxOf(1, d.toInt())
    return (need + n - 1) / n
}

// ---------- แก้ไขข้อมูล ----------

private fun AppData.mapExpense(id: Long, f: (Expense) -> Expense) =
    copy(expenses = expenses.map { if (it.id == id) f(it) else it })

private fun AppData.addStock(id: Long?, delta: Int) =
    if (id == null) this
    else copy(stock = stock.map { if (it.id == id) it.copy(remaining = maxOf(0, it.remaining + delta)) else it })

fun AppData.saveExpense(e: Expense) =
    if (expenses.any { it.id == e.id }) mapExpense(e.id) { e } else copy(expenses = expenses + e)

/** ติ๊ก/ยกเลิกรายการย่อย พร้อมตัดหรือคืนสต็อก */
fun AppData.toggleSub(expenseId: Long, subId: Long): AppData {
    val s = expenses.find { it.id == expenseId }?.subItems?.find { it.id == subId } ?: return this
    return mapExpense(expenseId) { e -> e.copy(subItems = e.subItems.map { if (it.id == subId) it.copy(done = !it.done) else it }) }
        .addStock(s.stockId, if (s.done) s.qty else -s.qty)
}

fun AppData.saveSub(expenseId: Long, s: SubItem): AppData {
    val old = expenses.find { it.id == expenseId }?.subItems?.find { it.id == s.id }
    val wasDone = old?.done == true
    // คืนสต็อกของค่าเดิมก่อน แล้วค่อยตัดใหม่ตามค่าที่แก้
    var d = if (wasDone) toggleSub(expenseId, s.id) else this
    d = d.mapExpense(expenseId) { e ->
        val n = s.copy(done = false)
        e.copy(subItems = if (old == null) e.subItems + n else e.subItems.map { if (it.id == s.id) n else it })
    }
    return if (wasDone) d.toggleSub(expenseId, s.id) else d
}

fun AppData.deleteSub(expenseId: Long, subId: Long) =
    mapExpense(expenseId) { e -> e.copy(subItems = e.subItems.filter { it.id != subId }) }

/** จ่ายแล้ว: ปิดรายการย่อยที่เหลือ และถ้าเป็นรายการซื้อของเข้าสต็อกให้เติมสต็อก */
fun AppData.setPaid(id: Long, paid: Boolean): AppData {
    val e = expenses.find { it.id == id } ?: return this
    if (e.paid == paid) return this
    var d = this
    if (paid) e.subItems.filter { !it.done }.forEach { d = d.toggleSub(id, it.id) }
    d = d.mapExpense(id) { it.copy(paid = paid) }
    val pack = stock.find { it.id == e.restockOf }?.unitsPerPack ?: 0
    return d.addStock(e.restockOf, (if (paid) 1 else -1) * pack * e.qty)
}

fun AppData.copyNextMonth(id: Long): AppData {
    val e = expenses.find { it.id == id } ?: return this
    return copy(
        expenses = expenses + e.copy(
            id = Ids.next(), paid = false, deposits = emptyList(),
            images = emptyList(), // ไม่ใช้ไฟล์รูปร่วมกับรายการเดิม ไม่งั้นลบรายการหนึ่งแล้วรูปของอีกรายการหาย
            due = e.due?.let { LocalDate.parse(it).plusMonths(1).toString() },
            links = e.links.map { it.copy(id = Ids.next()) },
            subItems = e.subItems.map { s -> s.copy(id = Ids.next(), done = false, links = s.links.map { it.copy(id = Ids.next()) }) },
        )
    )
}

fun AppData.deleteCategory(id: Long) = copy(
    categories = categories.filter { it.id != id },
    expenses = expenses.map { if (it.categoryId == id) it.copy(categoryId = null) else it },
    stock = stock.map { if (it.categoryId == id) it.copy(categoryId = null) else it },
)

fun AppData.deletePriority(id: Long) = copy(
    priorities = priorities.filter { it.id != id },
    expenses = expenses.map { if (it.priorityId == id) it.copy(priorityId = null) else it },
    stock = stock.map { if (it.priorityId == id) it.copy(priorityId = null) else it },
)

fun AppData.deleteStock(id: Long) = copy(
    stock = stock.filter { it.id != id },
    expenses = expenses.map { e ->
        e.copy(
            restockOf = e.restockOf.takeIf { it != id },
            subItems = e.subItems.map { if (it.stockId == id) it.copy(stockId = null) else it },
        )
    },
)

// ---------- สต็อก ----------

/** จำนวนที่วางแผนจะใช้ (รายการย่อยที่ยังไม่เสร็จ) */
fun AppData.planned(stockId: Long) =
    expenses.filter { !it.paid }.flatMap { it.subItems }.filter { !it.done && it.stockId == stockId }.sumOf { it.qty }

fun AppData.isLow(s: StockItem) = s.remaining < planned(s.id) || s.remaining <= s.alertAt

/** ของที่ไม่พอหรือใกล้หมด: สร้างรายการซื้อให้ ถ้ายังไม่มีรายการซื้อที่ค้างจ่ายอยู่ */
fun AppData.restock(buyName: (String) -> String): AppData {
    val buys = stock.filter { s -> isLow(s) && expenses.none { !it.paid && it.restockOf == s.id } }.map { s ->
        val per = maxOf(1, s.unitsPerPack)
        val short = planned(s.id) - s.remaining
        Expense(
            name = buyName(s.name),
            amount = linkPrice(s.links) ?: 0,
            qty = maxOf(1, (short + per - 1) / per),
            categoryId = s.categoryId,
            priorityId = s.priorityId,
            links = s.links.map { it.copy(id = Ids.next()) },
            restockOf = s.id,
        )
    }
    return if (buys.isEmpty()) this else copy(expenses = expenses + buys)
}
