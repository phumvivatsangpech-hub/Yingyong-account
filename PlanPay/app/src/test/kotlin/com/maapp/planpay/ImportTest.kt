package com.maapp.planpay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ImportTest {
    @Test
    fun csvHandlesQuotesBomAndSemicolons() {
        assertEquals(
            listOf(listOf("ชื่อ", "ราคา"), listOf("น้ำมัน, เครื่อง", "1,200"), listOf("พูด \"ดี\"", "5")),
            parseCsv("﻿ชื่อ,ราคา\r\n\"น้ำมัน, เครื่อง\",\"1,200\"\n\n\"พูด \"\"ดี\"\"\",5\n"),
        )
        assertEquals(listOf(listOf("a", "1"), listOf("b", "2")), parseCsv("a;1\nb;2"))
    }

    @Test
    fun thaiExcelCsvIsDecodedFromWindows874() {
        val bytes = "ชื่อ,ราคา\nค่าเช่า,4500".toByteArray(charset("windows-874"))
        assertEquals("ค่าเช่า", readTable(bytes)[1][0])
    }

    @Test
    fun datesInCommonFormats() {
        assertEquals("2026-11-30", parseDate("2026-11-30"))
        assertEquals("2026-11-30", parseDate("30/11/2026"))
        assertEquals("2026-11-30", parseDate("30/11/2569")) // พ.ศ.
        assertEquals("2026-11-30", parseDate("30/11/69"))
        assertEquals("2026-11-30", parseDate("46356")) // เลขวันที่ของ Excel
        assertNull(parseDate("เร็วๆ นี้"))
        assertNull(parseDate("31/02/2026"))
    }

    @Test
    fun rowsBecomeExpensesWithHeaderInAnyOrder() {
        val food = Tag(name = "อาหาร", color = palette[0])
        val rows = listOf(
            listOf("ราคา", "ชื่อ", "หมวดหมู่", "วันครบกำหนด", "ลิงก์", "ความสำคัญ"),
            listOf("1,200.50 ฿", "กรองน้ำมันเครื่อง", "รถ", "30/11/2569", "https://a.example/1 https://b.example/2", "สูง"),
            listOf("80", "ข้าว", "อาหาร", "", "", ""),
            listOf("99", "", "", "", "", ""), // ไม่มีชื่อ ข้าม
        )
        val (d, added) = AppData(categories = listOf(food)).importRows(rows)
        assertEquals(listOf("กรองน้ำมันเครื่อง", "ข้าว"), added.map { it.name })
        assertEquals(1201, added[0].total)
        assertEquals("2026-11-30", added[0].due)
        assertEquals(2, added[0].links.size)
        assertEquals(listOf("อาหาร", "รถ"), d.categories.map { it.name }) // หมวดใหม่ถูกสร้าง หมวดเดิมไม่ซ้ำ
        assertEquals(food.id, added[1].categoryId)
        assertEquals("สูง", d.priorities.single().name)
    }

    @Test
    fun subItemRowsGroupUnderTheirMainItem() {
        val soap = StockItem(name = "ผงซักฟอก", remaining = 5)
        val rows = listOf(
            listOf("ชื่อ", "รายการย่อย", "จำนวนเงิน", "จำนวน", "ของใช้", "หมวดหมู่", "ลิงก์"),
            listOf("ซักผ้า", "หยอดเหรียญ", "20", "2", "", "บ้าน", ""),
            listOf("", "ผงซักฟอก", "", "2", "ผงซักฟอก", "", ""), // ชื่อว่าง = รายการหลักแถวก่อนหน้า
            listOf("ค่าเน็ต", "", "599", "", "", "", ""),
            listOf("ซักผ้า", "น้ำยาปรับผ้านุ่ม", "59", "", "", "", "https://a.example/1"),
            listOf("", "ไม้แขวน", "30", "3", "ไม้แขวนเสื้อ", "", ""), // ของใช้ที่ยังไม่มี ถูกสร้างให้
        )
        val (d, added) = AppData(stock = listOf(soap)).importRows(rows)
        assertEquals(listOf("ซักผ้า", "ค่าเน็ต"), added.map { it.name })
        val wash = added[0]
        assertEquals(listOf("หยอดเหรียญ", "ผงซักฟอก", "น้ำยาปรับผ้านุ่ม", "ไม้แขวน"), wash.subItems.map { it.name })
        assertEquals(40 + 0 + 59 + 0, wash.total) // ของจากสต็อกไม่คิดเงิน
        assertEquals(soap.id, wash.subItems[1].stockId)
        assertEquals(2, wash.subItems[1].qty)
        assertEquals(1, wash.subItems[2].links.size)
        assertEquals(0, wash.links.size) // ลิงก์ของแถวรายการย่อยไม่ไปอยู่ที่รายการหลัก
        assertEquals("บ้าน", d.categories.single().name)
        assertEquals(listOf("ผงซักฟอก", "ไม้แขวนเสื้อ"), d.stock.map { it.name })
        assertEquals(599, added[1].total)
    }

    @Test
    fun rowsWithoutHeaderUseDefaultOrder() {
        val (_, added) = AppData().importRows(listOf(listOf("ค่าเน็ต", "599", "บิล")))
        assertEquals("ค่าเน็ต", added.single().name)
        assertEquals(599, added.single().total)
    }

    @Test
    fun xlsxFirstSheetWithSharedStringsAndGaps() {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { z ->
            fun put(name: String, xml: String) { z.putNextEntry(ZipEntry(name)); z.write(xml.toByteArray()); z.closeEntry() }
            put("xl/sharedStrings.xml", "<sst><si><t>ชื่อ</t></si><si><t>ราคา</t></si><si><r><t>ค่า</t></r><r><t>เช่า</t></r></si></sst>")
            put(
                "xl/worksheets/sheet1.xml",
                "<worksheet><sheetData>" +
                    "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"C1\" t=\"s\"><v>1</v></c></row>" +
                    "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>2</v></c><c r=\"C2\"><v>4500</v></c></row>" +
                    "</sheetData></worksheet>",
            )
        }
        val rows = readTable(zip.toByteArray())
        assertEquals(listOf(listOf("ชื่อ", "", "ราคา"), listOf("ค่าเช่า", "", "4500")), rows)
        assertEquals(4500, AppData().importRows(rows).second.single().total)
    }
}
