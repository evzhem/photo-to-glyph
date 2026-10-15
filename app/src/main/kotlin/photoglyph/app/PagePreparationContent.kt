package photoglyph.app

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.PageQuad
import photoglyph.core.img.PreparedImage
import kotlin.math.roundToInt

@Composable
internal fun PagePreparationContent(
    current: ArgbImage?,
    corners: PageQuad?,
    prepared: PreparedImage?,
    offset: Float,
    a4: Boolean,
    busy: Boolean,
    onQuadChange: (PageQuad) -> Unit,
    onRotate: () -> Unit,
    onDetectCorners: () -> Unit,
    onCrop: () -> Unit,
    onOffsetChange: (Float) -> Unit,
    onA4Change: (Boolean) -> Unit,
    onPrepare: () -> Unit,
) {
    if (current != null && corners != null) {
        Text(stringResource(R.string.corner_drag_hint))
        CornerPreview(current, corners, onQuadChange)
        Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp)) {
            OutlinedButton(enabled = !busy, onClick = onRotate) { Text(stringResource(R.string.rotate_picture)) }
            OutlinedButton(enabled = !busy, onClick = onDetectCorners) { Text(stringResource(R.string.detect_corners)) }
        }
        OutlinedButton(enabled = !busy, onClick = onCrop) { Text(stringResource(R.string.crop_picture)) }
        Text(stringResource(R.string.threshold_offset, offset.roundToInt()))
        Slider(value = offset, enabled = !busy, onValueChange = onOffsetChange, valueRange = 0f..MAX_OFFSET)
        Row {
            Checkbox(checked = a4, enabled = !busy, onCheckedChange = onA4Change)
            Text(stringResource(R.string.a4_grid_ratio))
        }
        Button(enabled = !busy, onClick = onPrepare) {
            Text(stringResource(if (busy) R.string.processing else R.string.rectify_picture))
        }
        prepared?.let { data ->
            Text(stringResource(R.string.binary_ready, data.binary.width, data.binary.height))
            val bitmap = remember(data) {
                val pixels = IntArray(data.binary.width * data.binary.height) { if (data.binary[it]) BLACK else WHITE }
                Bitmap.createBitmap(pixels, data.binary.width, data.binary.height, Bitmap.Config.ARGB_8888).asImageBitmap()
            }
            Image(bitmap, contentDescription = stringResource(R.string.binary_description), modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp))
        }
    }
}

@Composable
internal fun ContourModeContent(
    current: ArgbImage?,
    result: ArgbImage?,
    threshold: Float,
    busy: Boolean,
    onThresholdChange: (Float) -> Unit,
    onFindContours: () -> Unit,
) {
    if (current == null) {
        Text(stringResource(R.string.contour_import_hint))
    } else {
        Text(stringResource(R.string.contour_source))
        val sourceBitmap = remember(current) { Bitmap.createBitmap(current.copyPixels(), current.width, current.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
        Image(sourceBitmap, contentDescription = stringResource(R.string.contour_source_description), modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp))
        Text(stringResource(R.string.contour_threshold, threshold.roundToInt()))
        Slider(value = threshold, enabled = !busy, onValueChange = onThresholdChange, valueRange = MIN_CONTOUR_THRESHOLD..MAX_CONTOUR_THRESHOLD)
        Button(enabled = !busy, onClick = onFindContours) {
            Text(stringResource(if (busy) R.string.processing else R.string.find_contours))
        }
        result?.let { image ->
            Text(stringResource(R.string.contour_ready, image.width, image.height))
            val bitmap = remember(image) { Bitmap.createBitmap(image.copyPixels(), image.width, image.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
            Image(bitmap, contentDescription = stringResource(R.string.contour_result_description), modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp))
        }
    }
}

private const val PREVIEW_HEIGHT_DP = 320
private const val BUTTON_GAP_DP = 8
private const val MAX_OFFSET = 50f
private const val MIN_CONTOUR_THRESHOLD = 0f
private const val MAX_CONTOUR_THRESHOLD = 255f
private const val BLACK = 0xff000000.toInt()
private const val WHITE = 0xffffffff.toInt()
