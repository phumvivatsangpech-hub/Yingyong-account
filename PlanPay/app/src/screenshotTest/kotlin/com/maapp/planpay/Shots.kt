package com.maapp.planpay

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityOptionsCompat
import com.android.tools.screenshot.PreviewTest
import java.time.LocalDate

// รูปหน้าจอสำหรับตรวจดีไซน์ในคอม: gradlew updateDebugScreenshotTest
// ผลลัพธ์อยู่ที่ app/src/screenshotTestDebug/reference

private val owner = object : ActivityResultRegistryOwner {
    override val activityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {}
    }
}

private val today = LocalDate.now()
private val cats = listOf("ที่อยู่อาศัย", "อาหาร", "เดินทาง", "บิล/ค่าน้ำไฟเน็ต", "อื่นๆ").mapIndexed { i, n -> Tag(100L + i, n, palette[(i * 2) % palette.size]) }
private val pris = listOf(Tag(200, "สูง", palette[0]), Tag(201, "กลาง", palette[2]), Tag(202, "ต่ำ", palette[3]))
private val soap = StockItem(300, "ผงซักฟอก", remaining = 1, unit = "รอบ", unitsPerPack = 10)
private val wash = Expense(
    2, "ซักผ้า 2 รอบ", categoryId = 104, priorityId = 201, due = today.plusDays(3).toString(), note = "ซักผ้าที่ค้างอยู่ให้หมด",
    deposits = listOf(Deposit(1, 40, today.minusDays(1).toString())),
    subItems = listOf(
        SubItem(10, "หยอดเหรียญเครื่องซัก", 20, 2, done = true),
        SubItem(11, "ผงซักฟอก", qty = 2, stockId = 300),
        SubItem(12, "น้ำยาปรับผ้านุ่ม", 59, 1, links = listOf(Link(20, "https://shopee.co.th/x", "Shopee", 59))),
    ),
    links = listOf(Link(21, "https://shopee.co.th/a", "ร้านแรก", 180, soldOut = true), Link(22, "https://lazada.co.th/b", "ร้านสอง", 210)),
)
private val sample = AppData(
    expenses = listOf(
        Expense(1, "ค่าเช่าห้อง", 4500, categoryId = 100, priorityId = 200, due = today.plusDays(14).toString(),
            deposits = listOf(Deposit(2, 1500, today.minusDays(3).toString()))),
        wash,
        Expense(3, "กรองน้ำมันเครื่อง", 180, categoryId = 102, priorityId = 201, due = today.plusDays(29).toString()),
        Expense(4, "ค่าเน็ต", 599, categoryId = 103, priorityId = 200, due = today.minusDays(2).toString()),
        Expense(5, "รองเท้าวิ่ง", 2390, categoryId = 104, priorityId = 202),
        Expense(6, "ค่าไฟเดือนที่แล้ว", 820, categoryId = 103, priorityId = 200, paid = true),
    ),
    categories = cats, priorities = pris, stock = listOf(soap, StockItem(301, "น้ำยาล้างจาน", remaining = 8, unit = "ครั้ง", alertAt = 2)),
    incomes = listOf(Income(400, "เงินเดือน", 18000, today.withDayOfMonth(1).toString()), Income(401, "ขายของมือสอง", 650, today.toString(), "เสื้อ 2 ตัว")),
    dayItems = listOf(DayItem(500, "ค่าข้าวกลางวัน", 60, today.toString()), DayItem(501, "ค่ารถ", 40, today.toString(), done = true), DayItem(502, "ของขวัญวันเกิด", 300, today.plusDays(4).toString())),
)

@Composable
private fun Shot(content: @Composable () -> Unit) {
    Store.previewData(sample)
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) { AppTheme(content) }
}

@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun Home() = Shot { HomeScreen {} }
@PreviewTest @Preview(widthDp = 392, heightDp = 1500, locale = "th") @Composable fun Detail() = Shot { DetailScreen(2, {}, {}) }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun Edit() = Shot { EditScreen(1, {}, {}) }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun Income() = Shot { IncomeScreen() }
@PreviewTest @Preview(widthDp = 392, heightDp = 1000, locale = "th") @Composable fun Summary() = Shot { SummaryScreen() }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun Stock() = Shot { StockScreen({}, {}) }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun More() = Shot { MoreScreen {} }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES) @Composable fun HomeDark() = Shot { HomeScreen {} }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun Tags() = Shot { TagsScreen(true) {} }
@PreviewTest @Preview(widthDp = 392, heightDp = 850, locale = "th") @Composable fun WithTabs() = Shot {
    androidx.compose.foundation.layout.Column {
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) { IncomeScreen() }
        TabBar("income") {}
    }
}
@PreviewTest @Preview(widthDp = 392, heightDp = 620, locale = "th") @Composable fun DetailPaid() = Shot { DetailScreen(6, {}, {}) }
@PreviewTest @Preview(widthDp = 392, heightDp = 1250, locale = "th") @Composable fun Calendar() = Shot { CalendarScreen {} }
