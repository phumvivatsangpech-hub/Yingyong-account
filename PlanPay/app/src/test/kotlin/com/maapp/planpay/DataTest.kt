package com.maapp.planpay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.YearMonth

class DataTest {
    @Test
    fun backupRoundTripKeepsEverything() {
        val cat = Tag(name = "บ้าน", color = palette[0])
        val d = AppData(
            expenses = listOf(Expense(name = "ค่าเช่า", amount = 4500, categoryId = cat.id, images = listOf("1.img"),
                subItems = listOf(SubItem(name = "ก", unitPrice = 5)), links = listOf(Link(url = "https://a.example", price = 9)))),
            categories = listOf(cat),
            stock = listOf(StockItem(name = "ผงซักฟอก", remaining = 3)),
            incomes = listOf(Income(name = "เงินเดือน", amount = 20000, date = "2026-11-01")),
        )
        assertEquals(d, decodeBackup(encodeData(d)))
    }

    @Test
    fun filesThatAreNotBackupsAreRejected() {
        assertThrows(Exception::class.java) { decodeBackup("{}") } // JSON ว่างต้องไม่กลายเป็นข้อมูลว่างแล้วทับของเดิม
        assertThrows(Exception::class.java) { decodeBackup("[1,2]") }
        assertThrows(Exception::class.java) { decodeBackup("ชื่อ,ราคา\nก,1") }
        assertThrows(Exception::class.java) { decodeBackup("""{"expenses":"x","categories":[],"priorities":[]}""") }
    }

    @Test
    fun dataFileFromFirstVersionStillOpens() {
        // ไฟล์รุ่นแรกไม่มี incomes และ images
        val old = """{"expenses":[{"id":1,"name":"ค่าเน็ต","amount":599}],"categories":[],"priorities":[],"stock":[]}"""
        val d = decodeData(old)
        assertEquals(599, d.expenses.single().total)
        assertEquals(emptyList<String>(), d.expenses.single().images)
        assertEquals(emptyList<Income>(), d.incomes)
    }

    @Test
    fun incomeIsSummedPerMonth() {
        val d = AppData(incomes = listOf(
            Income(name = "เงินเดือน", amount = 20000, date = "2026-11-01"),
            Income(name = "ขายของ", amount = 500, date = "2026-11-30"),
            Income(name = "เดือนก่อน", amount = 999, date = "2026-10-31"),
        ))
        assertEquals(20500, d.incomeIn(YearMonth.of(2026, 11)))
        assertEquals(0, d.incomeIn(YearMonth.of(2026, 12)))
    }

    @Test
    fun unpaidIsGroupedByCategoryLargestFirst() {
        val home = Tag(name = "บ้าน", color = palette[0])
        val food = Tag(name = "อาหาร", color = palette[1])
        val d = AppData(categories = listOf(home, food), expenses = listOf(
            Expense(name = "ข้าว", amount = 100, categoryId = food.id),
            Expense(name = "ค่าเช่า", amount = 4500, categoryId = home.id),
            Expense(name = "ค่าไฟ", amount = 500, categoryId = home.id),
            Expense(name = "จ่ายแล้ว", amount = 9999, categoryId = home.id, paid = true),
            Expense(name = "ไม่มีหมวด", amount = 50),
        ))
        assertEquals(listOf(home to 5000, food to 100, null to 50), d.unpaidBy(d.categories) { it.categoryId })
    }
}

class CalendarTest {
    @Test
    fun monthGridStartsOnSunday() {
        val nov = monthCells(YearMonth.of(2026, 11)) // 1 พ.ย. 2026 เป็นวันอาทิตย์
        assertEquals(java.time.LocalDate.of(2026, 11, 1), nov.first())
        assertEquals(30, nov.size)
        val dec = monthCells(YearMonth.of(2026, 12)) // 1 ธ.ค. 2026 เป็นวันอังคาร
        assertEquals(listOf(null, null), dec.take(2))
        assertEquals(java.time.LocalDate.of(2026, 12, 1), dec[2])
        assertEquals(2 + 31, dec.size)
    }

    @Test
    fun dueTotalCountsOnlyUnpaidInThatMonth() {
        val d = AppData(expenses = listOf(
            Expense(name = "ก", amount = 100, due = "2026-11-05"),
            Expense(name = "ข", amount = 200, due = "2026-11-30", paid = true),
            Expense(name = "ค", amount = 400, due = "2026-12-01"),
            Expense(name = "ง", amount = 800),
        ))
        assertEquals(100, d.dueUnpaidIn(YearMonth.of(2026, 11)))
        assertEquals(400, d.dueUnpaidIn(YearMonth.of(2026, 12)))
    }
}

class SaveOnTest {
    private val today = java.time.LocalDate.of(2026, 11, 16)

    @Test
    fun savingIsSpreadFromTodayUntilTheDayBeforeDue() {
        val d = AppData(expenses = listOf(
            Expense(name = "ค่าเช่า", amount = 4500, due = "2026-12-01"),  // 15 วัน วันละ 300
            Expense(name = "วันนี้", amount = 50, due = "2026-11-16"),      // ครบกำหนดวันนี้ เก็บวันนี้ทั้งหมด
            Expense(name = "เลยกำหนด", amount = 999, due = "2026-11-01"),
            Expense(name = "ไม่กำหนด", amount = 999),
            Expense(name = "จ่ายแล้ว", amount = 999, due = "2026-12-01", paid = true),
        ))
        assertEquals(350, d.saveOn(today, today))
        assertEquals(300, d.saveOn(today.plusDays(1), today))
        assertEquals(300, d.saveOn(today.plusDays(14), today))
        assertEquals(0, d.saveOn(today.plusDays(15), today)) // วันครบกำหนด เก็บครบแล้ว
        assertEquals(0, d.saveOn(today.minusDays(1), today))
    }
}

class DayItemTest {
    private val d = AppData(
        expenses = listOf(Expense(name = "ค่าเช่า", amount = 4500, due = "2026-11-30")),
        dayItems = listOf(
            DayItem(name = "ค่าข้าว", amount = 60, date = "2026-11-17"),
            DayItem(name = "ค่ารถ", amount = 40, date = "2026-11-17", done = true),
            DayItem(name = "ของขวัญ", amount = 300, date = "2026-12-02"),
        ),
    )

    @Test
    fun totalsCountDayItemsUntilTheyAreTicked() {
        assertEquals(4500 + 60 + 300, d.totalUnpaid)
        assertEquals(4500 + 60, d.dueUnpaidIn(YearMonth.of(2026, 11)))
        assertEquals(300, d.dueUnpaidIn(YearMonth.of(2026, 12)))
    }

    @Test
    fun dayItemsDoNotChangeDailySaving() {
        val today = java.time.LocalDate.of(2026, 11, 17)
        assertEquals(d.copy(dayItems = emptyList()).saveOn(today, today), d.saveOn(today, today))
    }

    @Test
    fun dayItemsSurviveBackupAndOldFilesHaveNone() {
        assertEquals(d, decodeBackup(encodeData(d)))
        assertEquals(emptyList<DayItem>(), decodeData("""{"expenses":[],"categories":[],"priorities":[]}""").dayItems)
    }
}
