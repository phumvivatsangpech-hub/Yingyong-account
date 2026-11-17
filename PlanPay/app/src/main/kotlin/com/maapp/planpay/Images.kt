package com.maapp.planpay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_IMAGE_BYTES = 20L * 1024 * 1024

fun imagesDir(ctx: Context) = File(ctx.filesDir, "images").apply { mkdirs() }

/** คัดลอกรูปที่เลือกเข้าพื้นที่ของแอพ คืนชื่อไฟล์ หรือ null ถ้าอ่านไม่ได้หรือใหญ่เกิน */
private fun saveImage(ctx: Context, uri: Uri): String? = runCatching {
    val name = "${Ids.next()}.img"
    val out = File(imagesDir(ctx), name)
    ctx.contentResolver.openInputStream(uri)!!.use { input -> out.outputStream().use { input.copyTo(it) } }
    if (out.length() == 0L || out.length() > MAX_IMAGE_BYTES) { out.delete(); null } else name
}.getOrNull()

/** ถอดรูปแบบย่อขนาด (ไม่โหลดรูปเต็มเข้าหน่วยความจำ) และหมุนตามข้อมูลกล้อง */
private fun loadBitmap(file: File, maxPx: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
    val b = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val deg = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    return if (deg == 0f) b else Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(deg) }, true)
}

@Composable
private fun rememberBitmap(file: File, maxPx: Int): ImageBitmap? = produceState<ImageBitmap?>(null, file.path, maxPx) {
    value = withContext(Dispatchers.IO) { runCatching { loadBitmap(file, maxPx)?.asImageBitmap() }.getOrNull() }
}.value

/** แถวรูปของรายการ: เพิ่มจากแกลเลอรีได้หลายรูป แตะเพื่อดูเต็มจอและลบ */
@Composable
fun ImagesSection(e: Expense) {
    val ctx = LocalContext.current
    val dir = remember { imagesDir(ctx) }
    var viewing by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        // shortcut: files are copied on the main thread, move to a background thread if picking many large photos feels slow
        val added = uris.mapNotNull { saveImage(ctx, it) }
        if (added.isNotEmpty()) Store.update(checkStock = false) {
            copy(expenses = expenses.map { if (it.id == e.id) it.copy(images = it.images + added) else it })
        }
    }
    Group(stringResource(R.string.images)) {
        Column(Modifier.padding(12.dp)) {
            if (e.images.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                e.images.forEachIndexed { i, name ->
                    val bmp = rememberBitmap(File(dir, name), 256)
                    Box(
                        Modifier.size(96.dp).clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClickLabel = stringResource(R.string.open)) { viewing = name },
                    ) {
                        if (bmp != null) Image(bmp, stringResource(R.string.image_n, i + 1), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                }
            }
            SmallAction(stringResource(R.string.add_image), strong = true) {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }

    viewing?.let { name ->
        val bmp = rememberBitmap(File(dir, name), 2048)
        Dialog({ viewing = null }, DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (bmp != null) Image(bmp, stringResource(R.string.images), Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(Space.s)) {
                    IconButton({
                        Store.update(checkStock = false) {
                            copy(expenses = expenses.map { if (it.id == e.id) it.copy(images = it.images - name) else it })
                        }
                        File(dir, name).delete()
                        viewing = null
                    }) { Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = Color.White) }
                    IconButton({ viewing = null }) { Icon(Icons.Default.Close, stringResource(R.string.close), tint = Color.White) }
                }
            }
        }
    }
}

/** ลบรายการพร้อมไฟล์รูปของมัน (ไม่ตรวจสต็อกตอนลบ ไม่งั้นรายการซื้อที่เพิ่งลบจะถูกสร้างกลับมาทันที) */
fun deleteExpense(ctx: Context, e: Expense) {
    Store.update(checkStock = false) { copy(expenses = expenses.filter { it.id != e.id }) }
    e.images.forEach { File(imagesDir(ctx), it).delete() }
}
