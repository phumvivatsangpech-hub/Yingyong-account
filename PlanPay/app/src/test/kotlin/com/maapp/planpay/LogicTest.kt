package com.maapp.planpay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    private val today = LocalDate.of(2026, 11, 16)
    private fun AppData.exp(id: Long) = expenses.first { it.id == id }

    @Test
    fun linkPriceSkipsSoldOutAndFallsBackToLastKnown() {
        val a = Link(url = "a", price = 180, soldOut = true)
        val b = Link(url = "b", price = 210)
        assertEquals(210, linkPrice(listOf(a, b)))
        assertEquals(210, linkPrice(listOf(a, b.copy(soldOut = true)))) // หมดทุกลิงก์ ใช้ราคาล่าสุด
        assertNull(linkPrice(listOf(Link(url = "c"))))
    }

    @Test
    fun dailySavingUsesRemainingAfterDeposits() {
        val e = Expense(name = "ค่าเช่า", amount = 4500, due = "2026-12-01")
        assertEquals(300, e.daily(today)) // 4500 / 15 วัน
        assertEquals(234, e.copy(deposits = listOf(Deposit(amount = 1000, date = "2026-11-16"))).daily(today)) // 3500/15 ปัดขึ้น
        assertNull(e.copy(due = "2026-11-01").daily(today)) // เลยกำหนด
        assertEquals(true, e.copy(due = "2026-11-01").overdue(today))
        assertNull(e.copy(due = null).daily(today))
    }

    @Test
    fun subItemsTotalAndStockFlow() {
        val soap = StockItem(name = "ผงซักฟอก", remaining = 1, unit = "รอบ", unitsPerPack = 10,
            links = listOf(Link(url = "x", price = 60)))
        val coin = SubItem(name = "หยอดเหรียญ", unitPrice = 20, qty = 2)
        val use = SubItem(name = "ผงซักฟอก", qty = 2, stockId = soap.id)
        val wash = Expense(name = "ซักผ้า", subItems = listOf(coin, use))
        var d = AppData(expenses = listOf(wash), stock = listOf(soap)).restock { "ซื้อ $it" }

        assertEquals(40, d.exp(wash.id).total) // ของจากสต็อกไม่คิดเงิน
        // สต็อก 1 แต่จะใช้ 2 -> สร้างรายการซื้อ 1 รายการ ราคาจากลิงก์
        val buy = d.expenses.single { it.restockOf == soap.id }
        assertEquals("ซื้อ ผงซักฟอก", buy.name)
        assertEquals(60, buy.total)
        d = d.restock { "ซื้อ $it" }
        assertEquals(1, d.expenses.count { it.restockOf == soap.id }) // ไม่สร้างซ้ำ

        d = d.setPaid(buy.id, true) // ซื้อแล้ว เติมสต็อก +10
        assertEquals(11, d.stock.single().remaining)

        d = d.toggleSub(wash.id, use.id) // ใช้ไป 2
        assertEquals(9, d.stock.single().remaining)
        assertEquals(40, d.exp(wash.id).unpaid)
        d = d.toggleSub(wash.id, use.id) // ยกเลิก คืนสต็อก
        assertEquals(11, d.stock.single().remaining)

        d = d.setPaid(wash.id, true) // จ่ายทั้งรายการ ปิดรายการย่อยที่เหลือและตัดสต็อก
        assertEquals(9, d.stock.single().remaining)
        assertEquals(0, d.exp(wash.id).unpaid)
    }

    @Test
    fun copyNextMonthResetsState() {
        val e = Expense(name = "ค่าเน็ต", amount = 599, due = "2026-11-30", paid = true,
            deposits = listOf(Deposit(amount = 599, date = "2026-11-10")))
        val c = AppData(expenses = listOf(e)).copyNextMonth(e.id).expenses.last()
        assertEquals("2026-12-30", c.due)
        assertEquals(false, c.paid)
        assertEquals(0, c.saved)
    }
}
