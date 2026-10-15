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

internal data class PagePreparationUiState(
    val current: ArgbImage?,
    val corners: PageQuad?,
    val prepared: PreparedImage?,
    val offset: Float,
    val a4: Boolean,
    val busy: Boolean,
)

internal data class PagePreparationActions(
    val onQuadChange: (PageQuad) -> Unit,
    val onRotate: () -> Unit,
    val onDetectCorners: () -> Unit,
    val onCrop: () -> Unit,
    val onOffsetChange: (Float) -> Unit,
    val onA4Change: (Boolean) -> Unit,
    val onPrepare: () -> Unit,
)

internal data class ContourModeUiState(
    val current: ArgbImage?,
    val result: ArgbImage?,
    val threshold: Float,
    val busy: Boolean,
)

internal data class ContourModeActions(
    val onThresholdChange: (Float) -> Unit,
    val onFindContours: () -> Unit,
)

@Composable
internal fun PagePreparationContent(
    state: PagePreparationUiState,
    actions: PagePreparationActions,
) {
    val current = state.current
    val corners = state.corners
    if (current != null && corners != null) {
        Text(stringResource(R.string.corner_drag_hint))
        CornerPreview(current, corners, actions.onQuadChange)
        Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp)) {
            OutlinedButton(enabled = !state.busy, onClick = actions.onRotate) { Text(stringResource(R.string.rotate_picture)) }
            OutlinedButton(enabled = !state.busy, onClick = actions.onDetectCorners) { Text(stringResource(R.string.detect_corners)) }
        }
        OutlinedButton(enabled = !state.busy, onClick = actions.onCrop) { Text(stringResource(R.string.crop_picture)) }
        Text(stringResource(R.string.threshold_offset, state.offset.roundToInt()))
        Slider(value = state.offset, enabled = !state.busy, onValueChange = actions.onOffsetChange, valueRange = 0f..MAX_OFFSET)
        Row {
            Checkbox(checked = state.a4, enabled = !state.busy, onCheckedChange = actions.onA4Change)
            Text(stringResource(R.string.a4_grid_ratio))
        }
        Button(enabled = !state.busy, onClick = actions.onPrepare) {
            Text(stringResource(if (state.busy) R.string.processing else R.string.rectify_picture))
        }
        state.prepared?.let { data ->
            Text(stringResource(R.string.binary_ready, data.binary.width, data.binary.height))
            val bitmap =
                remember(data) {
                    val pixels = IntArray(data.binary.width * data.binary.height) { if (data.binary[it]) BLACK else WHITE }
                    Bitmap.createBitmap(pixels, data.binary.width, data.binary.height, Bitmap.Config.ARGB_8888).asImageBitmap()
                }
            Image(bitmap, contentDescription = stringResource(R.string.binary_description), modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp))
        }
    }
}

@Composable
internal fun ContourModeContent(
    state: ContourModeUiState,
    actions: ContourModeActions,
) {
    val current = state.current
    if (current == null) {
        Text(stringResource(R.string.contour_import_hint))
    } else {
        Text(stringResource(R.string.contour_source))
        val sourceBitmap = remember(current) { Bitmap.createBitmap(current.copyPixels(), current.width, current.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
        Image(sourceBitmap, contentDescription = stringResource(R.string.contour_source_description), modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp))
        Text(stringResource(R.string.contour_threshold, state.threshold.roundToInt()))
        Slider(value = state.threshold, enabled = !state.busy, onValueChange = actions.onThresholdChange, valueRange = MIN_CONTOUR_THRESHOLD..MAX_CONTOUR_THRESHOLD)
        Button(enabled = !state.busy, onClick = actions.onFindContours) {
            Text(stringResource(if (state.busy) R.string.processing else R.string.find_contours))
        }
        state.result?.let { image ->
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
