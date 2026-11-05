package iq.student.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

/** الصورة المصوّرة والمقصوصة تنتقل بين الشاشات هنا (بدل تمريرها بالتنقل) */
object ScanSession {
    var raw: Bitmap? = null
    var cropped: Bitmap? = null
    var subjectId: String? = null
    var typedText: String = ""
}

/** يحمّل صورة بحجم مناسب (أطول ضلع 1800px) ويصحح الدوران من EXIF */
fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 1800): Bitmap? {
    val cr = ctx.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val decoded = cr.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val orientation = cr.openInputStream(uri)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
    }
    val scale = maxSide.toFloat() / max(decoded.width, decoded.height)
    if (scale < 1f) m.postScale(scale, scale)
    return if (m.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
}

/** قص بنسب 0..1 من الصورة */
fun cropFractions(src: Bitmap, l: Float, t: Float, r: Float, b: Float): Bitmap {
    val x = (l * src.width).toInt().coerceIn(0, src.width - 2)
    val y = (t * src.height).toInt().coerceIn(0, src.height - 2)
    val w = ((r - l) * src.width).toInt().coerceIn(2, src.width - x)
    val h = ((b - t) * src.height).toInt().coerceIn(2, src.height - y)
    return Bitmap.createBitmap(src, x, y, w, h)
}
