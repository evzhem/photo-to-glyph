package photoglyph.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.mirrorHorizontal
import photoglyph.core.img.resizeToMaxEdge
import photoglyph.core.img.rotateQuarterTurns

/** Android decoding/I/O stays out of core. Persisted images/font storage are not M2. */
internal fun importImage(context: Context, uri: Uri): ArgbImage {
    val bounds = BitmapFactory.Options().also { it.inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { stream ->
        requireNotNull(stream) { "Missing image stream" }
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unsupported image" }
    var sample = 1
    while (bounds.outWidth.toLong() / sample * (bounds.outHeight / sample) > MAX_DECODE_PIXELS) sample *= 2
    val options = BitmapFactory.Options().also { it.inSampleSize = sample }
    val bitmap = context.contentResolver.openInputStream(uri).use { stream ->
        requireNotNull(stream)
        requireNotNull(BitmapFactory.decodeStream(stream, null, options)) { "Image decode failed" }
    }
    val image = try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        ArgbImage(bitmap.width, bitmap.height, pixels)
    } finally {
        bitmap.recycle()
    }
    val orientation = context.contentResolver.openInputStream(uri).use { stream ->
        if (stream == null) ExifInterface.ORIENTATION_NORMAL else ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }
    val turns = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 1
        ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 2
        ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 3
        else -> 0
    }
    var oriented = rotateQuarterTurns(image, turns)
    if (orientation in MIRRORED_ORIENTATIONS) {
        oriented = mirrorHorizontal(oriented)
    }
    return resizeToMaxEdge(oriented, PREVIEW_MAX_EDGE_PX)
}

private val MIRRORED_ORIENTATIONS = setOf(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, ExifInterface.ORIENTATION_FLIP_VERTICAL, ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_TRANSVERSE)
private const val MAX_DECODE_PIXELS = 4_000_000L
private const val PREVIEW_MAX_EDGE_PX = 960
