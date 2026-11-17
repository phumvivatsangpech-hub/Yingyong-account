package com.maapp.planpay

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

object Store {
    private lateinit var app: Context
    private val file get() = File(app.filesDir, "data.json")

    var data by mutableStateOf(AppData())
        private set

    fun init(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        data = load()
    }

    private fun load(): AppData {
        if (!file.exists()) return seed()
        return try {
            decodeData(file.readText())
        } catch (e: Exception) {
            // เก็บไฟล์ที่อ่านไม่ได้ไว้ ไม่เขียนทับ
            file.copyTo(File(app.filesDir, "data.bad.${System.currentTimeMillis()}.json"))
            seed()
        }
    }

    private fun seed() = AppData(
        categories = app.resources.getStringArray(R.array.default_categories)
            .mapIndexed { i, n -> Tag(name = n, color = palette[i % palette.size]) },
        priorities = app.resources.getStringArray(R.array.default_priorities)
            .zip(listOf(palette[0], palette[2], palette[3])) { n, c -> Tag(name = n, color = c) },
    )

    // shortcut: whole state is rewritten as one JSON file on the main thread, move to Room when it grows past a few thousand items
    fun update(checkStock: Boolean = true, f: AppData.() -> AppData) {
        val d = data.f()
        data = if (checkStock) d.restock { app.getString(R.string.buy_item, it) } else d
        val tmp = File(app.filesDir, "data.json.tmp")
        tmp.writeText(encodeData(data))
        tmp.renameTo(file)
    }

    /** ใช้เฉพาะตอน render รูปหน้าจอตัวอย่าง (Shots.kt): ใส่ข้อมูลโดยไม่แตะไฟล์ */
    fun previewData(d: AppData) { data = d }

    /** กู้คืนจากไฟล์สำรอง: เก็บข้อมูลก่อนกู้คืนไว้อีกชุดเผื่อเลือกไฟล์ผิด */
    fun replace(d: AppData) {
        if (file.exists()) file.copyTo(File(app.filesDir, "data.before-restore.json"), overwrite = true)
        update(checkStock = false) { d }
    }
}
