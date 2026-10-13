package photoglyph.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import photoglyph.core.PipelineParams
import photoglyph.core.geom.Vec2
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.PageQuad
import photoglyph.core.img.PreparedImage
import photoglyph.core.img.cropImage
import photoglyph.core.img.detectPageQuad
import photoglyph.core.img.prepareImage
import photoglyph.core.img.rotateQuarterTurns
import photoglyph.core.img.suggestedRectifiedSize
import java.io.File
import kotlin.math.roundToInt

/** M2 raster preparation. Deliberately does not claim character/outline recognition. */
@Composable
internal fun PreparationScreen(onDemo: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var image by remember { mutableStateOf<ArgbImage?>(null) }
    var quad by remember { mutableStateOf<PageQuad?>(null) }
    var prepared by remember { mutableStateOf<PreparedImage?>(null) }
    var busy by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var a4 by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(PipelineParams().adaptiveOffset.toFloat()) }

    fun loaded(value: ArgbImage) {
        image = value
        quad = detectPageQuad(value) ?: PageQuad.fullImage(value)
        prepared = null
        message = null
    }
    fun load(uri: Uri, temporary: File? = null) {
        scope.launch {
            busy = true
            try { loaded(withContext(Dispatchers.IO) { importImage(context, uri) }) }
            catch (_: IllegalArgumentException) { message = R.string.import_failed }
            catch (_: java.io.IOException) { message = R.string.import_failed }
            finally { withContext(Dispatchers.IO) { temporary?.delete() }; busy = false }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) load(uri) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) camera = true else message = R.string.camera_denied
    }
    if (camera) CameraCaptureDialog(onImage = { uri, file -> camera = false; load(uri, file) }, onDismiss = { camera = false })

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PAGE_PADDING_DP.dp), verticalArrangement = Arrangement.spacedBy(SECTION_GAP_DP.dp)) {
        Text(stringResource(R.string.app_name))
        Text(stringResource(R.string.preparation_title))
        Text(stringResource(R.string.preparation_note))
        Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp)) {
            Button(enabled = !busy, onClick = { picker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }) { Text(stringResource(R.string.import_picture)) }
            Button(enabled = !busy, onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera = true
                else permission.launch(Manifest.permission.CAMERA)
            }) { Text(stringResource(R.string.camera_open)) }
        }
        OutlinedButton(enabled = !busy, onClick = {
            scope.launch {
                busy = true
                try {
                    loaded(withContext(Dispatchers.IO) {
                        context.assets.open(SYNTHETIC_PAGE_ASSET).use { stream ->
                            val bitmap = checkNotNull(BitmapFactory.decodeStream(stream))
                            try { ArgbImage(bitmap.width, bitmap.height, IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }) }
                            finally { bitmap.recycle() }
                        }
                    })
                } catch (_: java.io.IOException) { message = R.string.import_failed }
                catch (_: IllegalStateException) { message = R.string.import_failed }
                finally { busy = false }
            }
        }) { Text(stringResource(R.string.synthetic_page)) }
        message?.let { Text(stringResource(it)) }
        val current = image
        val corners = quad
        if (current != null && corners != null) {
            Text(stringResource(R.string.corner_drag_hint))
            CornerPreview(current, corners) { value -> quad = value; prepared = null }
            Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp)) {
                OutlinedButton(enabled = !busy, onClick = { loaded(rotateQuarterTurns(current, 1)) }) { Text(stringResource(R.string.rotate_picture)) }
                OutlinedButton(enabled = !busy, onClick = {
                    val detected = detectPageQuad(current)
                    if (detected == null) message = R.string.corner_not_found else { quad = detected; prepared = null; message = null }
                }) { Text(stringResource(R.string.detect_corners)) }
            }
            OutlinedButton(enabled = !busy, onClick = {
                val left = corners.corners.minOf { it.x }.toInt().coerceAtLeast(0)
                val top = corners.corners.minOf { it.y }.toInt().coerceAtLeast(0)
                val right = corners.corners.maxOf { it.x }.roundToInt().coerceAtMost(current.width)
                val bottom = corners.corners.maxOf { it.y }.roundToInt().coerceAtMost(current.height)
                loaded(cropImage(current, left, top, right - left, bottom - top))
            }) { Text(stringResource(R.string.crop_picture)) }
            Text(stringResource(R.string.threshold_offset, offset.roundToInt()))
            Slider(value = offset, enabled = !busy, onValueChange = { offset = it; prepared = null }, valueRange = 0f..MAX_OFFSET)
            Row {
                Checkbox(checked = a4, enabled = !busy, onCheckedChange = { a4 = it; prepared = null })
                Text(stringResource(R.string.a4_grid_ratio))
            }
            Button(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        prepared = withContext(Dispatchers.Default) { val dimensions = if (a4) OUTPUT_WIDTH to OUTPUT_HEIGHT else suggestedRectifiedSize(corners, OUTPUT_MAX_EDGE)
                            prepareImage(current, corners, dimensions.first, dimensions.second, PipelineParams(adaptiveOffset = offset.toDouble())) }
                        message = null
                    } catch (_: IllegalArgumentException) { message = R.string.prepare_failed }
                    finally { busy = false }
                }
            }) { Text(stringResource(if (busy) R.string.running_demo else R.string.rectify_picture)) }
            prepared?.let { data ->
                Text(stringResource(R.string.binary_ready, data.binary.width, data.binary.height))
                val bitmap = remember(data) { Bitmap.createBitmap(IntArray(data.binary.width * data.binary.height) { if (data.binary[it]) BLACK else WHITE }, data.binary.width, data.binary.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
                Image(bitmap, contentDescription = stringResource(R.string.binary_description), modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp))
            }
        }
        Text(stringResource(R.string.outlines_planned))
        OutlinedButton(onClick = onDemo) { Text(stringResource(R.string.open_core_demo)) }
    }
}

@Composable
private fun CornerPreview(image: ArgbImage, quad: PageQuad, onQuad: (PageQuad) -> Unit) {
    val bitmap = remember(image) { Bitmap.createBitmap(image.copyPixels(), image.width, image.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
    val latestQuad by rememberUpdatedState(quad)
    Canvas(Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp).pointerInput(image) {
        var selected = -1
        fun raster(position: Offset): Vec2 {
            val scale = minOf(size.width.toDouble() / image.width, size.height.toDouble() / image.height)
            return Vec2((position.x / scale).coerceIn(0.0, image.width.toDouble()), (position.y / scale).coerceIn(0.0, image.height.toDouble()))
        }
        detectDragGestures(onDragStart = { point -> selected = latestQuad.corners.indices.minBy { latestQuad.corners[it].distanceTo(raster(point)) } }, onDrag = { change, _ ->
            if (selected >= 0) {
                val points = latestQuad.corners.toMutableList().also { it[selected] = raster(change.position) }
                try { onQuad(PageQuad(points[0], points[1], points[2], points[3])) } catch (_: IllegalArgumentException) { /* Keep last valid polygon. */ }
                change.consume()
            }
        })
    }) {
        val scale = minOf(size.width / image.width, size.height / image.height)
        drawImage(bitmap, dstSize = androidx.compose.ui.unit.IntSize((image.width * scale).roundToInt(), (image.height * scale).roundToInt()))
        for (index in quad.corners.indices) {
            val first = quad.corners[index]; val next = quad.corners[(index + 1) % quad.corners.size]
            drawLine(Color(QUAD_COLOR), Offset((first.x * scale).toFloat(), (first.y * scale).toFloat()), Offset((next.x * scale).toFloat(), (next.y * scale).toFloat()), strokeWidth = QUAD_STROKE_PX)
            drawCircle(Color(QUAD_COLOR), radius = HANDLE_RADIUS_PX, center = Offset((first.x * scale).toFloat(), (first.y * scale).toFloat()))
        }
    }
}

private const val SYNTHETIC_PAGE_ASSET = "m2/grid-yaw30-side-light.png"
private const val OUTPUT_MAX_EDGE = 800
private const val OUTPUT_WIDTH = 420
private const val OUTPUT_HEIGHT = 594
private const val PREVIEW_HEIGHT_DP = 320
private const val PAGE_PADDING_DP = 20
private const val SECTION_GAP_DP = 12
private const val BUTTON_GAP_DP = 8
private const val MAX_OFFSET = 50f
private const val BLACK = 0xff000000.toInt()
private const val WHITE = 0xffffffff.toInt()
private const val QUAD_COLOR = 0xff2a6554
private const val QUAD_STROKE_PX = 2f
private const val HANDLE_RADIUS_PX = 7f
