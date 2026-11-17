package com.maapp.planpay

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.ByteArrayOutputStream
import java.time.LocalDate

private const val MAX_FILE = 5 * 1024 * 1024

/** อ่านไฟล์ที่ผู้ใช้เลือก ไม่เกิน MAX_FILE (ไฟล์ใหญ่กว่านั้นถือว่าอ่านไม่ได้) */
private fun readFile(ctx: Context, uri: Uri): ByteArray {
    val out = ByteArrayOutputStream()
    ctx.contentResolver.openInputStream(uri)!!.use { input ->
        val buf = ByteArray(8192)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
            require(out.size() <= MAX_FILE) { "file too large" }
        }
    }
    return out.toByteArray()
}

/** หน้าเพิ่มเติม: จัดการหมวดหมู่/ความสำคัญ นำเข้าไฟล์ สำรองและกู้คืนข้อมูล */
@Composable
fun MoreScreen(go: (String) -> Unit) {
    val d = Store.data
    val ctx = LocalContext.current
    fun toast(id: Int) = Toast.makeText(ctx, id, Toast.LENGTH_LONG).show()
    var importing by remember { mutableStateOf<List<List<String>>?>(null) }
    var restoring by remember { mutableStateOf<AppData?>(null) }

    // shortcut: files are read and written on the main thread (capped at 5 MB), move to a background thread if that ever feels slow
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val rows = readTable(readFile(ctx, uri))
            if (d.importRows(rows).second.isEmpty()) toast(R.string.import_empty) else importing = rows
        } catch (e: UnsupportedOperationException) {
            toast(R.string.import_xls)
        } catch (e: Exception) {
            toast(R.string.import_failed)
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching { ctx.contentResolver.openOutputStream(uri)!!.use { it.write(encodeData(Store.data).toByteArray()) } }.isSuccess
        toast(if (ok) R.string.backup_done else R.string.backup_failed)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // ตรวจไฟล์ให้ผ่านก่อน ยังไม่แตะข้อมูลเดิมจนกว่าผู้ใช้จะยืนยัน
        try {
            restoring = decodeBackup(String(readFile(ctx, uri)))
        } catch (e: Exception) {
            toast(R.string.restore_invalid)
        }
    }

    Screen(stringResource(R.string.tab_more)) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(Space.m, Space.m, Space.m, Space.l),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Group(stringResource(R.string.manage)) {
                NavRow(stringResource(R.string.stock), d.stock.size.toString()) { go("stock") }
                RowDivider()
                NavRow(stringResource(R.string.categories), d.categories.size.toString()) { go("categories") }
                RowDivider()
                NavRow(stringResource(R.string.priorities), d.priorities.size.toString()) { go("priorities") }
            }
            Group(stringResource(R.string.data)) {
                NavRow(stringResource(R.string.import_file)) { importPicker.launch(arrayOf("*/*")) }
                RowDivider()
                NavRow(stringResource(R.string.backup_export)) { exportPicker.launch("planpay-backup-${LocalDate.now()}.json") }
                RowDivider()
                NavRow(stringResource(R.string.backup_restore)) { restorePicker.launch(arrayOf("*/*")) }
            }
            Text(
                stringResource(R.string.backup_note),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Space.m),
            )
        }
    }

    importing?.let { rows ->
        val found = d.importRows(rows).second
        ConfirmDialog(
            stringResource(R.string.import_found, found.size, found.take(5).joinToString(", ") { it.name }),
            stringResource(R.string.import_confirm), destructive = false,
            onDismiss = { importing = null },
            onConfirm = {
                Store.update { importRows(rows).first }
                importing = null
                Toast.makeText(ctx, ctx.getString(R.string.import_done, found.size), Toast.LENGTH_SHORT).show()
            },
        )
    }

    restoring?.let { backup ->
        ConfirmDialog(
            stringResource(R.string.restore_confirm, d.expenses.size, backup.expenses.size),
            stringResource(R.string.restore_action),
            onDismiss = { restoring = null },
            onConfirm = {
                Store.replace(backup)
                restoring = null
                toast(R.string.restore_done)
            },
        )
    }
}
