package com.maapp.planpay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        enableEdgeToEdge()
        setContent { App() }
    }
}

/** ไอคอนกราฟแท่ง (ชุดไอคอนพื้นฐานไม่มีให้) */
private val BarsIcon: ImageVector = ImageVector.Builder("Bars", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 13f); horizontalLineToRelative(4f); verticalLineToRelative(7f); horizontalLineToRelative(-4f); close()
        moveTo(10f, 4f); horizontalLineToRelative(4f); verticalLineToRelative(16f); horizontalLineToRelative(-4f); close()
        moveTo(16f, 9f); horizontalLineToRelative(4f); verticalLineToRelative(11f); horizontalLineToRelative(-4f); close()
    }
}.build()

private val tabs = listOf(
    Triple("home", R.string.home_title, Icons.AutoMirrored.Filled.List),
    Triple("income", R.string.income, Icons.Default.AddCircle),
    Triple("summary", R.string.tab_summary, BarsIcon),
    Triple("stock", R.string.stock, Icons.Default.ShoppingCart),
    Triple("more", R.string.tab_more, Icons.Default.Menu),
)

@Composable
fun App() = AppTheme {
    // เส้นทางหน้าจอเป็นข้อความ "ชื่อหน้า:id" เช่น "detail:123" ตัวแรกของ stack คือแท็บที่เลือก
    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() })
    ) { mutableStateListOf("home") }
    val holder = rememberSaveableStateHolder() // จำค่าตัวกรองของแต่ละหน้าไว้ตอนไปหน้าอื่นแล้วกลับมา
    val go: (String) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) holder.removeState(stack.removeAt(stack.lastIndex)) }
    BackHandler(stack.size > 1, back)
    val atRoot = stack.size == 1

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = stack.size to stack.last(),
            // แถบแท็บด้านล่างกันพื้นที่ปุ่มนำทางของระบบให้แล้ว หน้าข้างในไม่ต้องเว้นซ้ำ
            modifier = Modifier.weight(1f).then(if (atRoot) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
            transitionSpec = {
                val dir = targetState.first.compareTo(initialState.first) // 1 = เข้าหน้าลึกขึ้น, -1 = ย้อนกลับ, 0 = สลับแท็บ
                if (dir == 0) fadeIn(tween(150)) togetherWith fadeOut(tween(100))
                else (slideInHorizontally(tween(220)) { it / 5 * dir } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 5 * dir } + fadeOut(tween(120)))
            },
            label = "nav",
        ) { (_, route) ->
            holder.SaveableStateProvider(route) {
                val parts = route.split(":")
                val id = parts.getOrNull(1)?.toLongOrNull()
                when (parts[0]) {
                    "detail" -> DetailScreen(id ?: 0, go, back)
                    "edit" -> EditScreen(id, go, back)
                    "categories" -> TagsScreen(isPriority = false, back)
                    "priorities" -> TagsScreen(isPriority = true, back)
                    "stockEdit" -> StockEditScreen(id, back)
                    "income" -> IncomeScreen()
                    "summary" -> SummaryScreen()
                    "stock" -> StockScreen(go)
                    "more" -> MoreScreen(go)
                    else -> HomeScreen(go)
                }
            }
        }
        if (atRoot) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            tabs.forEach { (route, label, icon) ->
                NavigationBarItem(
                    selected = stack[0] == route,
                    onClick = { stack[0] = route },
                    icon = { Icon(icon, null) },
                    label = { Text(stringResource(label), maxLines = 1) },
                )
            }
        }
    }
}
