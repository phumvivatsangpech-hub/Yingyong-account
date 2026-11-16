package com.maapp.planpay

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

/** อ่านไฟล์ CSV หรือ .xlsx เป็นตารางข้อความ (ดูจากเนื้อไฟล์ ไม่ดูนามสกุล) */
fun readTable(bytes: ByteArray): List<List<String>> = when {
    bytes.size >= 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> readXlsx(bytes)
    bytes.size >= 2 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte() -> throw UnsupportedOperationException("xls")
    else -> parseCsv(decodeText(bytes))
}

/** Excel ภาษาไทยมักบันทึก CSV เป็น Windows-874 ไม่ใช่ UTF-8 */
fun decodeText(b: ByteArray): String = try {
    Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(b)).toString()
} catch (e: CharacterCodingException) {
    String(b, runCatching { Charset.forName("windows-874") }.getOrDefault(Charsets.ISO_8859_1))
}

fun parseCsv(text: String): List<List<String>> {
    val s = text.removePrefix("﻿")
    val first = s.lineSequence().firstOrNull() ?: return emptyList()
    val sep = listOf(',', ';', '\t').maxBy { c -> first.count { it == c } }
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            quoted && c == '"' && s.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
            c == '"' -> quoted = !quoted
            !quoted && c == sep -> { row.add(cell.toString()); cell.clear() }
            !quoted && (c == '\n' || c == '\r') -> {
                if (c == '\r' && s.getOrNull(i + 1) == '\n') i++
                row.add(cell.toString()); cell.clear()
                rows.add(row); row = mutableListOf()
            }
            else -> cell.append(c)
        }
        i++
    }
    if (cell.isNotEmpty() || row.isNotEmpty()) { row.add(cell.toString()); rows.add(row) }
    return rows.filter { r -> r.any { it.isNotBlank() } }
}

private const val MAX_ENTRY = 20 * 1024 * 1024 // กันไฟล์ zip ที่ขยายแล้วใหญ่ผิดปกติ

private fun InputStream.readLimited(): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(8192)
    while (true) {
        val n = read(buf)
        if (n < 0) break
        out.write(buf, 0, n)
        require(out.size() <= MAX_ENTRY) { "file too large" }
    }
    return out.toByteArray()
}

private fun NodeList.elements() = (0 until length).map { item(it) as Element }

/** อ่านชีตแรกของ .xlsx (ไฟล์ zip ที่มี XML ข้างใน) โดยไม่ใช้ไลบรารีเพิ่ม */
fun readXlsx(bytes: ByteArray): List<List<String>> {
    val files = HashMap<String, ByteArray>()
    ZipInputStream(bytes.inputStream()).use { z ->
        while (true) {
            val e = z.nextEntry ?: break
            if (e.name == "xl/sharedStrings.xml" || e.name.startsWith("xl/worksheets/sheet")) files[e.name] = z.readLimited()
        }
    }
    val factory = DocumentBuilderFactory.newInstance()
    runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } // ไฟล์จากภายนอก ไม่รับ DTD
    fun dom(b: ByteArray) = factory.newDocumentBuilder().parse(b.inputStream())
    fun Element.texts() = getElementsByTagName("t").elements().joinToString("") { it.textContent }

    val shared = files["xl/sharedStrings.xml"]?.let { b -> dom(b).getElementsByTagName("si").elements().map { it.texts() } } ?: emptyList()
    val sheet = files.keys.filter { it.startsWith("xl/worksheets/sheet") }.minOrNull() ?: return emptyList()
    return dom(files.getValue(sheet)).getElementsByTagName("row").elements().map { r ->
        val cells = ArrayList<String>()
        r.getElementsByTagName("c").elements().forEach { c ->
            // เซลล์ว่างไม่ถูกเขียนลงไฟล์ ต้องดูตำแหน่งคอลัมน์จากชื่อเซลล์ เช่น "C5"
            val col = c.getAttribute("r").takeWhile { it.isLetter() }.fold(0) { a, ch -> a * 26 + (ch.uppercaseChar() - 'A' + 1) } - 1
            val v = c.getElementsByTagName("v").elements().firstOrNull()?.textContent ?: ""
            val text = when (c.getAttribute("t")) {
                "s" -> shared.getOrElse(v.toIntOrNull() ?: -1) { "" }
                "inlineStr" -> c.texts()
                else -> v
            }
            while (cells.size < col) cells.add("")
            cells.add(text)
        }
        cells
    }.filter { r -> r.any { it.isNotBlank() } }
}

/** รับ 2026-11-30, 30/11/2026, 30/11/2569, 30/11/69 และเลขวันที่ของ Excel */
fun parseDate(raw: String): String? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    t.toDoubleOrNull()?.let { n ->
        return if (n in 20000.0..80000.0) LocalDate.of(1899, 12, 30).plusDays(n.toLong()).toString() else null
    }
    val iso = runCatching { LocalDate.parse(t.take(10)) }.getOrNull()
    if (iso != null) return iso.toString()
    val m = Regex("""(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})""").find(t) ?: return null
    var y = m.groupValues[3].toInt()
    if (y < 100) y += if (y >= 43) 2500 else 2000 // ปี 2 หลัก: 69 = พ.ศ. 2569, 26 = ค.ศ. 2026
    if (y > 2400) y -= 543
    return runCatching { LocalDate.of(y, m.groupValues[2].toInt(), m.groupValues[1].toInt()).toString() }.getOrNull()
}

private fun parseAmount(raw: String) =
    (Regex("[^0-9.]").replace(raw, "").toDoubleOrNull() ?: 0.0).coerceIn(0.0, 999_999_999.0).roundToInt()

// ลำดับสำคัญ: "รายการย่อย" ต้องตรวจก่อน "รายการ" และ "จำนวนเงิน" ก่อน "จำนวน"
private val columnNames = listOf(
    "sub" to listOf("ย่อย", "sub"),
    "name" to listOf("ชื่อ", "รายการ", "name", "item"),
    "amount" to listOf("จำนวนเงิน", "ราคา", "ยอด", "amount", "price"),
    "qty" to listOf("จำนวน", "qty", "quantity"),
    "stock" to listOf("ของใช้", "สต็อก", "stock"),
    "category" to listOf("หมวด", "category"),
    "priority" to listOf("ความสำคัญ", "priority"),
    "due" to listOf("กำหนด", "วันที่", "due", "date"),
    "note" to listOf("หมายเหตุ", "note"),
    "link" to listOf("ลิงก์", "ลิ้ง", "link", "url"),
)
private val defaultOrder = listOf("name", "amount", "category", "priority", "due", "note", "link")

/**
 * แปลงตารางเป็นรายการรายจ่าย แถวแรกเป็นหัวคอลัมน์ (ไทยหรืออังกฤษ สลับลำดับได้)
 * ถ้าไม่มีหัวคอลัมน์ ใช้ลำดับ: ชื่อ, จำนวนเงิน, หมวดหมู่, ความสำคัญ, วันครบกำหนด, หมายเหตุ, ลิงก์
 *
 * แถวที่มีคอลัมน์ "รายการย่อย" จะเป็นรายการย่อยของรายการหลักชื่อเดียวกัน (ชื่อว่าง = รายการหลักแถวก่อนหน้า)
 * โดย จำนวนเงิน = ราคาต่อหน่วย, จำนวน = จำนวนหน่วย, ของใช้ = ชื่อของในสต็อกที่จะใช้
 * หมวดหมู่ ความสำคัญ และของใช้ที่ยังไม่มีจะถูกสร้างให้
 */
fun AppData.importRows(rows: List<List<String>>): Pair<AppData, List<Expense>> {
    if (rows.isEmpty()) return this to emptyList()
    val header = rows.first().map { it.trim().lowercase() }
    val byHeader = header.mapIndexedNotNull { i, h ->
        columnNames.firstOrNull { (_, keys) -> keys.any { h.contains(it) } }?.let { it.first to i }
    }.reversed().toMap() // ถ้าชื่อซ้ำ ใช้คอลัมน์ซ้ายสุด
    val hasHeader = "name" in byHeader
    val cols = if (hasHeader) byHeader else defaultOrder.mapIndexed { i, c -> c to i }.toMap()

    var categories = categories
    var priorities = priorities
    var stock = stock
    fun tag(list: List<Tag>, name: String, add: (Tag) -> Unit): Long? {
        if (name.isBlank()) return null
        list.find { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        return Tag(name = name, color = palette[list.size % palette.size]).also(add).id
    }
    fun stockId(name: String): Long? {
        if (name.isBlank()) return null
        stock.find { it.name.equals(name, ignoreCase = true) }?.let { return it.id }
        return StockItem(name = name).also { stock = stock + it }.id
    }

    val added = mutableListOf<Expense>()
    var current = -1 // ตำแหน่งรายการหลักล่าสุดใน added
    rows.drop(if (hasHeader) 1 else 0).forEach { r ->
        fun cell(key: String) = cols[key]?.let { r.getOrNull(it) }?.trim() ?: ""
        val name = cell("name")
        val sub = cell("sub")
        val links = Regex("""https?://\S+""").findAll(cell("link")).map { Link(url = it.value) }.toList()
        if (name.isEmpty() && (sub.isEmpty() || current < 0)) return@forEach

        // รายการหลัก: แถวธรรมดาสร้างใหม่เสมอ แถวรายการย่อยใช้รายการหลักชื่อเดียวกันที่มีอยู่ในไฟล์
        val existing = if (sub.isEmpty()) -1 else if (name.isEmpty()) current else added.indexOfLast { it.name.equals(name, ignoreCase = true) }
        current = if (existing >= 0) existing else {
            added += Expense(
                name = name,
                amount = if (sub.isEmpty()) parseAmount(cell("amount")) else 0,
                categoryId = tag(categories, cell("category")) { categories = categories + it },
                priorityId = tag(priorities, cell("priority")) { priorities = priorities + it },
                due = parseDate(cell("due")),
                note = cell("note"),
                links = if (sub.isEmpty()) links else emptyList(),
            )
            added.lastIndex
        }
        if (sub.isNotEmpty()) {
            val sid = stockId(cell("stock"))
            val item = SubItem(
                name = sub,
                unitPrice = parseAmount(cell("amount")),
                qty = maxOf(1, parseAmount(cell("qty"))),
                stockId = sid,
                links = if (sid == null) links else emptyList(),
            )
            added[current] = added[current].let { it.copy(subItems = it.subItems + item) }
        }
    }
    return copy(expenses = expenses + added, categories = categories, priorities = priorities, stock = stock) to added
}
