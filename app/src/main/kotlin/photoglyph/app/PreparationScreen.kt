package photoglyph.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import photoglyph.core.img.blackContourImage
import photoglyph.core.img.cropImage
import photoglyph.core.img.detectPageQuad
import photoglyph.core.img.prepareImage
import photoglyph.core.img.rotateQuarterTurns
import photoglyph.core.img.suggestedRectifiedSize
import java.io.File
import kotlin.math.roundToInt

/** Camera-first page preparation, a manual crop-and-label builder, and independent picture contours; OCR is absent. */
@Composable
internal fun PreparationScreen(
    store: LocalGlyphSetStore,
    onOpenLibrary: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var image by remember { mutableStateOf<ArgbImage?>(null) }
    var quad by remember { mutableStateOf<PageQuad?>(null) }
    var prepared by remember { mutableStateOf<PreparedImage?>(null) }
    var contourResult by remember { mutableStateOf<ArgbImage?>(null) }
    var mode by remember { mutableStateOf(PreparationMode.PAGE) }
    var contourThreshold by remember { mutableStateOf(PipelineParams().contourEdgeThreshold.toFloat()) }
    var busy by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(false) }
    var showAlphabetBuilder by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    var a4 by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(PipelineParams().adaptiveOffset.toFloat()) }

    fun loaded(value: ArgbImage) {
        image = value
        quad = detectPageQuad(value) ?: PageQuad.fullImage(value)
        prepared = null
        contourResult = null
        showAlphabetBuilder = false
        message = null
    }

    fun load(
        uri: Uri,
        temporary: File? = null,
    ) {
        scope.launch {
            busy = true
            try {
                loaded(withContext(Dispatchers.IO) { importImage(context, uri) })
            } catch (_: IllegalArgumentException) {
                message = R.string.import_failed
            } catch (_: java.io.IOException) {
                message = R.string.import_failed
            } finally {
                withContext(Dispatchers.IO) { temporary?.delete() }
                busy = false
            }
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) load(uri) }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) camera = true else message = R.string.camera_denied
        }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            camera = true
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }
    if (camera) {
        CameraCaptureDialog(onImage = { uri, file ->
            camera = false
            load(uri, file)
        }, onDismiss = { camera = false })
    }

    val preparedPage = prepared
    if (showAlphabetBuilder && preparedPage != null) {
        AlphabetBuilderScreen(
            binary = preparedPage.binary,
            store = store,
            onBack = { showAlphabetBuilder = false },
            onSaved = {
                showAlphabetBuilder = false
                onOpenLibrary()
            },
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PAGE_PADDING_DP.dp), verticalArrangement = Arrangement.spacedBy(SECTION_GAP_DP.dp)) {
        Text(stringResource(R.string.app_name))
        Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp)) {
            if (mode == PreparationMode.PAGE) {
                Button(enabled = !busy, onClick = { mode = PreparationMode.PAGE }) { Text(stringResource(R.string.page_mode)) }
            } else {
                OutlinedButton(enabled = !busy, onClick = { mode = PreparationMode.PAGE }) { Text(stringResource(R.string.page_mode)) }
            }
            if (mode == PreparationMode.CONTOURS) {
                Button(enabled = !busy, onClick = { mode = PreparationMode.CONTOURS }) { Text(stringResource(R.string.contour_mode)) }
            } else {
                OutlinedButton(enabled = !busy, onClick = { mode = PreparationMode.CONTOURS }) { Text(stringResource(R.string.contour_mode)) }
            }
        }
        OutlinedButton(enabled = !busy, onClick = onOpenLibrary) { Text(stringResource(R.string.my_sets)) }
        Text(stringResource(if (mode == PreparationMode.PAGE) R.string.preparation_title else R.string.contour_title))
        Text(stringResource(if (mode == PreparationMode.PAGE) R.string.preparation_note else R.string.contour_note))
        Row(horizontalArrangement = Arrangement.spacedBy(BUTTON_GAP_DP.dp)) {
            Button(enabled = !busy, onClick = { picker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }) { Text(stringResource(R.string.import_picture)) }
            Button(enabled = !busy, onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    camera = true
                } else {
                    permission.launch(Manifest.permission.CAMERA)
                }
            }) { Text(stringResource(R.string.camera_open)) }
        }
        message?.let { Text(stringResource(it)) }
        val current = image
        val corners = quad
        if (mode == PreparationMode.PAGE) {
            PagePreparationContent(
                state = PagePreparationUiState(current, corners, prepared, offset, a4, busy),
                actions =
                    PagePreparationActions(
                        onQuadChange = { value ->
                            quad = value
                            prepared = null
                        },
                        onRotate = { current?.let { loaded(rotateQuarterTurns(it, 1)) } },
                        onDetectCorners = {
                            current?.let { source ->
                                val detected = detectPageQuad(source)
                                if (detected == null) {
                                    message = R.string.corner_not_found
                                } else {
                                    quad = detected
                                    prepared = null
                                    message = null
                                }
                            }
                        },
                        onCrop = {
                            if (current != null && corners != null) {
                                val left =
                                    corners.corners
                                        .minOf { it.x }
                                        .toInt()
                                        .coerceAtLeast(0)
                                val top =
                                    corners.corners
                                        .minOf { it.y }
                                        .toInt()
                                        .coerceAtLeast(0)
                                val right =
                                    corners.corners
                                        .maxOf { it.x }
                                        .roundToInt()
                                        .coerceAtMost(current.width)
                                val bottom =
                                    corners.corners
                                        .maxOf { it.y }
                                        .roundToInt()
                                        .coerceAtMost(current.height)
                                loaded(cropImage(current, left, top, right - left, bottom - top))
                            }
                        },
                        onOffsetChange = {
                            offset = it
                            prepared = null
                        },
                        onA4Change = {
                            a4 = it
                            prepared = null
                        },
                        onPrepare = {
                            if (current != null && corners != null) {
                                val source = current
                                val frame = corners
                                val useA4 = a4
                                val thresholdOffset = offset.toDouble()
                                scope.launch {
                                    busy = true
                                    try {
                                        prepared =
                                            withContext(Dispatchers.Default) {
                                                val dimensions = if (useA4) OUTPUT_WIDTH to OUTPUT_HEIGHT else suggestedRectifiedSize(frame, OUTPUT_MAX_EDGE)
                                                prepareImage(source, frame, dimensions.first, dimensions.second, PipelineParams(adaptiveOffset = thresholdOffset))
                                            }
                                        message = null
                                    } catch (_: IllegalArgumentException) {
                                        message = R.string.prepare_failed
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                    ),
            )
            if (prepared != null) {
                Button(enabled = !busy, onClick = { showAlphabetBuilder = true }) {
                    Text(stringResource(R.string.build_handwriting_alphabet))
                }
            }
        } else {
            ContourModeContent(
                state = ContourModeUiState(current, contourResult, contourThreshold, busy),
                actions =
                    ContourModeActions(
                        onThresholdChange = {
                            contourThreshold = it
                            contourResult = null
                        },
                        onFindContours = {
                            current?.let { source ->
                                val sensitivity = contourThreshold.toDouble()
                                scope.launch {
                                    busy = true
                                    try {
                                        contourResult =
                                            withContext(Dispatchers.Default) {
                                                blackContourImage(source, PipelineParams(contourEdgeThreshold = sensitivity))
                                            }
                                        message = null
                                    } catch (_: IllegalArgumentException) {
                                        message = R.string.contour_failed
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                    ),
            )
        }
        }
    }
}

@Composable
internal fun CornerPreview(
    image: ArgbImage,
    quad: PageQuad,
    onQuad: (PageQuad) -> Unit,
) {
    val bitmap = remember(image) { Bitmap.createBitmap(image.copyPixels(), image.width, image.height, Bitmap.Config.ARGB_8888).asImageBitmap() }
    val latestQuad by rememberUpdatedState(quad)
    Canvas(
        Modifier.fillMaxWidth().height(PREVIEW_HEIGHT_DP.dp).pointerInput(image) {
            var selected = -1

            fun raster(position: Offset): Vec2 {
                val scale = minOf(size.width.toDouble() / image.width, size.height.toDouble() / image.height)
                return Vec2((position.x / scale).coerceIn(0.0, image.width.toDouble()), (position.y / scale).coerceIn(0.0, image.height.toDouble()))
            }
            detectDragGestures(onDragStart = { point -> selected = latestQuad.corners.indices.minBy { latestQuad.corners[it].distanceTo(raster(point)) } }, onDrag = { change, _ ->
                if (selected >= 0) {
                    val points = latestQuad.corners.toMutableList().also { it[selected] = raster(change.position) }
                    try {
                        onQuad(PageQuad(points[0], points[1], points[2], points[BOTTOM_LEFT_CORNER_INDEX]))
                    } catch (_: IllegalArgumentException) {
                        // Keep last valid polygon.
                    }
                    change.consume()
                }
            })
        },
    ) {
        val scale = minOf(size.width / image.width, size.height / image.height)
        drawImage(
            bitmap,
            dstSize =
                androidx.compose.ui.unit
                    .IntSize((image.width * scale).roundToInt(), (image.height * scale).roundToInt()),
        )
        for (index in quad.corners.indices) {
            val first = quad.corners[index]
            val next = quad.corners[(index + 1) % quad.corners.size]
            drawLine(Color(QUAD_COLOR), Offset((first.x * scale).toFloat(), (first.y * scale).toFloat()), Offset((next.x * scale).toFloat(), (next.y * scale).toFloat()), strokeWidth = QUAD_STROKE_PX)
            drawCircle(Color(QUAD_COLOR), radius = HANDLE_RADIUS_PX, center = Offset((first.x * scale).toFloat(), (first.y * scale).toFloat()))
        }
    }
}

private enum class PreparationMode { PAGE, CONTOURS }

private const val OUTPUT_MAX_EDGE = 800
private const val OUTPUT_WIDTH = 420
private const val OUTPUT_HEIGHT = 594
private const val PREVIEW_HEIGHT_DP = 320
private const val PAGE_PADDING_DP = 20
private const val SECTION_GAP_DP = 12
private const val BUTTON_GAP_DP = 8
private const val QUAD_COLOR = 0xff2a6554
private const val QUAD_STROKE_PX = 2f
private const val HANDLE_RADIUS_PX = 7f

private const val BOTTOM_LEFT_CORNER_INDEX = 3
