package photoglyph.app

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import photoglyph.core.PipelineParams
import photoglyph.core.geom.Polyline
import photoglyph.core.img.BinaryGrid
import photoglyph.core.img.InkComponent
import photoglyph.core.img.PixelBounds
import photoglyph.core.img.connectedInkComponents
import photoglyph.core.img.cropBinaryGrid
import photoglyph.core.model.GlyphSetNormalizationOptions
import photoglyph.core.model.RasterFontMetrics
import photoglyph.core.model.RasterGlyphInput
import photoglyph.core.model.normalizeGlyphSet
import photoglyph.core.vectorizeBinary
import java.io.IOException
import java.time.Instant
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/** Manual crop-and-review path; it never claims connected components are recognized letters. */
@Composable
internal fun AlphabetBuilderScreen(
    binary: BinaryGrid,
    store: LocalGlyphSetStore,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    val pageBitmap = remember(binary) { binaryBitmap(binary) }
    var selection by remember(binary) { mutableStateOf<PixelBounds?>(null) }
    var pendingGlyph by remember { mutableStateOf<PendingGlyph?>(null) }
    var pendingBaseline by remember { mutableFloatStateOf(0f) }
    var pendingLabel by remember { mutableStateOf("") }
    var glyphs by remember { mutableStateOf<List<ReviewedGlyph>>(emptyList()) }
    var setName by remember { mutableStateOf("") }
    var capHeightReference by remember { mutableStateOf<String?>(null) }
    var xHeightReference by remember { mutableStateOf<String?>(null) }
    var selectedScript by remember { mutableStateOf(HandwritingScript.CYRILLIC) }
    var selectedCharacter by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    val savedLabels = remember(glyphs) { glyphs.mapTo(mutableSetOf()) { it.input.char } }
    val capHeightPx = glyphs.firstOrNull { it.input.char == capHeightReference }?.input?.baselineY
    val xHeightPx = glyphs.firstOrNull { it.input.char == xHeightReference }?.input?.baselineY
    val metricsValid = capHeightPx != null && xHeightPx != null && xHeightPx > 0.0 && capHeightPx > xHeightPx
    val validPendingLabel = isSupportedGfontLabel(pendingLabel) && glyphs.none { it.input.char == pendingLabel }
    val canSave = !busy && pendingGlyph == null && setName.isNotBlank() && glyphs.isNotEmpty() && metricsValid

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(BUILDER_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(BUILDER_GAP_DP.dp),
    ) {
        OutlinedButton(enabled = !busy, onClick = onBack) { Text(stringResource(R.string.alphabet_cancel)) }
        Text(stringResource(R.string.alphabet_builder_title))
        Text(stringResource(R.string.alphabet_builder_note))
        LetterCasePicker(
            state =
                LetterCasePickerState(
                    script = selectedScript,
                    selectedCharacter = selectedCharacter,
                    savedLabels = savedLabels,
                    enabled = !busy,
                ),
            actions =
                LetterCasePickerActions(
                    onScriptChange = { script ->
                        selectedScript = script
                        selectedCharacter = null
                        if (pendingGlyph != null) pendingLabel = ""
                    },
                    onCharacterSelected = { character ->
                        selectedCharacter = character
                        if (pendingGlyph != null) pendingLabel = character
                    },
                ),
        )
        Text(stringResource(R.string.alphabet_select_hint))
        PageGlyphSelectionCanvas(
            binary = binary,
            bitmap = pageBitmap,
            selection = selection,
            enabled = !busy && pendingGlyph == null,
            onSelection = { selection = it },
        )
        Button(
            enabled = !busy && pendingGlyph == null && selection != null,
            onClick = {
                val bounds = selection ?: return@Button
                busy = true
                message = null
                scope.launchCropGlyphReview(
                    binary = binary,
                    bounds = bounds,
                    actions =
                        GlyphCropActions(
                            onSuccess = { cropped ->
                                pendingGlyph = cropped
                                pendingBaseline = cropped.binary.height.toFloat()
                                pendingLabel = selectedCharacter.orEmpty()
                            },
                            onFailure = { message = R.string.alphabet_crop_failed },
                            onComplete = { busy = false },
                        ),
                )
            },
        ) {
            Text(stringResource(if (busy) R.string.processing else R.string.alphabet_crop_button))
        }
        message?.let { Text(stringResource(it)) }

        PendingGlyphReviewSection(
            state =
                PendingGlyphSectionState(
                    pending = pendingGlyph,
                    baselineY = pendingBaseline,
                    label = pendingLabel,
                    labelIsUnique = validPendingLabel,
                    busy = busy,
                ),
            actions =
                PendingGlyphSectionActions(
                    onBaselineChange = { pendingBaseline = it },
                    onLabelChange = {
                        pendingLabel = it
                        selectedCharacter = null
                    },
                    onAdd = { pending ->
                        val input =
                            RasterGlyphInput(
                                char = pendingLabel,
                                paths = pending.paths,
                                originX = 0.0,
                                baselineY = pendingBaseline.toDouble(),
                                advancePx = pending.binary.width.toDouble(),
                            )
                        glyphs = glyphs + ReviewedGlyph(input, pending.binary.width, pending.binary.height, pending.binary)
                        pendingGlyph = null
                        pendingLabel = ""
                        selectedCharacter = null
                        selection = null
                        message = null
                    },
                    onDiscard = { pendingGlyph = null },
                ),
        )

        Text(stringResource(R.string.alphabet_glyph_count, glyphs.size))
        if (glyphs.isEmpty()) Text(stringResource(R.string.alphabet_empty_draft))
        ReviewedGlyphList(
            state =
                ReviewedGlyphListState(
                    glyphs = glyphs,
                    capHeightReference = capHeightReference,
                    xHeightReference = xHeightReference,
                    enabled = !busy,
                ),
            actions =
                ReviewedGlyphListActions(
                    onBaselineChange = { character, baseline ->
                        glyphs =
                            glyphs.map { current ->
                                if (current.input.char == character) {
                                    current.copy(input = current.input.copy(baselineY = baseline.toDouble()))
                                } else {
                                    current
                                }
                            }
                    },
                    onCapReference = { capHeightReference = it },
                    onXHeightReference = { xHeightReference = it },
                    onRemove = { character ->
                        glyphs = glyphs.filterNot { it.input.char == character }
                        if (capHeightReference == character) capHeightReference = null
                        if (xHeightReference == character) xHeightReference = null
                    },
                ),
        )
        AlphabetSetSaveControls(
            state =
                AlphabetSetSaveControlsState(
                    setName = setName,
                    capHeightReference = capHeightReference,
                    capHeightPx = capHeightPx,
                    xHeightReference = xHeightReference,
                    xHeightPx = xHeightPx,
                    metricsValid = metricsValid,
                    canSave = canSave,
                    busy = busy,
                ),
            actions =
                AlphabetSetSaveControlsActions(
                    onSetNameChange = { setName = it },
                    onSave = {
                        val heightCap = checkNotNull(capHeightPx)
                        val heightX = checkNotNull(xHeightPx)
                        busy = true
                        message = null
                        scope.launchSaveGlyphSet(
                            request =
                                GlyphSetSaveRequest(
                                    glyphs = glyphs.map { it.input },
                                    name = setName.trim(),
                                    xHeightPx = heightX,
                                    capHeightPx = heightCap,
                                ),
                            store = store,
                            actions =
                                GlyphSetSaveActions(
                                    onSaved = onSaved,
                                    onFailure = { message = R.string.alphabet_save_failed },
                                    onComplete = { busy = false },
                                ),
                        )
                    },
                ),
        )
    }
}

private data class ReviewedGlyphListState(
    val glyphs: List<ReviewedGlyph>,
    val capHeightReference: String?,
    val xHeightReference: String?,
    val enabled: Boolean,
)

private data class ReviewedGlyphListActions(
    val onBaselineChange: (String, Float) -> Unit,
    val onCapReference: (String) -> Unit,
    val onXHeightReference: (String) -> Unit,
    val onRemove: (String) -> Unit,
)

@Composable
private fun ReviewedGlyphList(
    state: ReviewedGlyphListState,
    actions: ReviewedGlyphListActions,
) {
    state.glyphs.forEach { glyph ->
        val character = glyph.input.char
        ReviewedGlyphCard(
            glyph = glyph,
            state =
                ReviewedGlyphCardState(
                    isCapReference = character == state.capHeightReference,
                    isXHeightReference = character == state.xHeightReference,
                    enabled = state.enabled,
                ),
            actions =
                ReviewedGlyphCardActions(
                    onBaselineChange = { actions.onBaselineChange(character, it) },
                    onCapReference = { actions.onCapReference(character) },
                    onXHeightReference = { actions.onXHeightReference(character) },
                    onRemove = { actions.onRemove(character) },
                ),
        )
    }
}

private data class AlphabetSetSaveControlsState(
    val setName: String,
    val capHeightReference: String?,
    val capHeightPx: Double?,
    val xHeightReference: String?,
    val xHeightPx: Double?,
    val metricsValid: Boolean,
    val canSave: Boolean,
    val busy: Boolean,
)

private data class AlphabetSetSaveControlsActions(
    val onSetNameChange: (String) -> Unit,
    val onSave: () -> Unit,
)

@Composable
private fun AlphabetSetSaveControls(
    state: AlphabetSetSaveControlsState,
    actions: AlphabetSetSaveControlsActions,
) {
    OutlinedTextField(
        value = state.setName,
        onValueChange = actions.onSetNameChange,
        label = { Text(stringResource(R.string.alphabet_set_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.alphabet_reference_hint))
    Text(
        stringResource(
            R.string.alphabet_metrics_summary,
            state.capHeightReference ?: EMPTY_REFERENCE,
            state.capHeightPx?.roundToInt() ?: 0,
            state.xHeightReference ?: EMPTY_REFERENCE,
            state.xHeightPx?.roundToInt() ?: 0,
        ),
    )
    if (state.capHeightReference != null && state.xHeightReference != null && !state.metricsValid) {
        Text(stringResource(R.string.alphabet_metrics_invalid))
    }
    Button(enabled = state.canSave, onClick = actions.onSave) {
        Text(stringResource(if (state.busy) R.string.alphabet_saving else R.string.alphabet_save))
    }
}

private data class GlyphCropActions(
    val onSuccess: (PendingGlyph) -> Unit,
    val onFailure: () -> Unit,
    val onComplete: () -> Unit,
)

private fun CoroutineScope.launchCropGlyphReview(
    binary: BinaryGrid,
    bounds: PixelBounds,
    actions: GlyphCropActions,
) {
    launch {
        try {
            val pending =
                withContext(Dispatchers.Default) {
                    val looseCrop = cropBinaryGrid(binary, bounds)
                    val components = connectedInkComponents(looseCrop)
                    require(components.isNotEmpty()) { "The selected area contains no ink" }
                    val tightBounds = componentUnionBounds(components)
                    val glyphCrop = cropBinaryGrid(looseCrop, tightBounds)
                    val extracted =
                        vectorizeBinary(
                            glyphCrop,
                            PipelineParams(minComponentAreaPx = 1, pruningIterations = 0),
                        )
                    require(extracted.strokes.polylines.isNotEmpty()) { "No centerline could be extracted" }
                    PendingGlyph(glyphCrop, extracted.strokes.polylines)
                }
            actions.onSuccess(pending)
        } catch (_: IllegalArgumentException) {
            actions.onFailure()
        } finally {
            actions.onComplete()
        }
    }
}

private data class PendingGlyphSectionState(
    val pending: PendingGlyph?,
    val baselineY: Float,
    val label: String,
    val labelIsUnique: Boolean,
    val busy: Boolean,
)

private data class PendingGlyphSectionActions(
    val onBaselineChange: (Float) -> Unit,
    val onLabelChange: (String) -> Unit,
    val onAdd: (PendingGlyph) -> Unit,
    val onDiscard: () -> Unit,
)

@Composable
private fun PendingGlyphReviewSection(
    state: PendingGlyphSectionState,
    actions: PendingGlyphSectionActions,
) {
    val pending = state.pending ?: return
    PendingGlyphReviewCard(
        pending = pending,
        state =
            PendingGlyphReviewState(
                baselineY = state.baselineY,
                label = state.label,
                labelIsValid = isSupportedGfontLabel(state.label),
                labelIsUnique = state.labelIsUnique,
                busy = state.busy,
            ),
        actions =
            PendingGlyphReviewActions(
                onBaselineChange = actions.onBaselineChange,
                onLabelChange = actions.onLabelChange,
                onAdd = { actions.onAdd(pending) },
                onDiscard = actions.onDiscard,
            ),
    )
}

private data class PendingGlyphReviewState(
    val baselineY: Float,
    val label: String,
    val labelIsValid: Boolean,
    val labelIsUnique: Boolean,
    val busy: Boolean,
)

private data class PendingGlyphReviewActions(
    val onBaselineChange: (Float) -> Unit,
    val onLabelChange: (String) -> Unit,
    val onAdd: () -> Unit,
    val onDiscard: () -> Unit,
)

@Composable
private fun PendingGlyphReviewCard(
    pending: PendingGlyph,
    state: PendingGlyphReviewState,
    actions: PendingGlyphReviewActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BUILDER_CARD_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(BUILDER_GAP_DP.dp),
        ) {
            Text(stringResource(R.string.alphabet_crop_review))
            GlyphRasterPreview(
                bitmap = remember(pending.binary) { binaryBitmap(pending.binary) },
                rasterWidth = pending.binary.width,
                rasterHeight = pending.binary.height,
                paths = pending.paths,
                baselineY = state.baselineY,
            )
            Text(stringResource(R.string.alphabet_baseline_hint, state.baselineY.roundToInt()))
            Slider(
                value = state.baselineY,
                enabled = !state.busy,
                onValueChange = actions.onBaselineChange,
                valueRange = 0f..pending.binary.height.toFloat(),
            )
            OutlinedTextField(
                value = state.label,
                enabled = !state.busy,
                onValueChange = actions.onLabelChange,
                label = { Text(stringResource(R.string.alphabet_character_label)) },
                singleLine = true,
            )
            if (state.label.isNotEmpty() && !state.labelIsValid) {
                Text(stringResource(R.string.alphabet_invalid_label))
            } else if (state.label.isNotEmpty() && !state.labelIsUnique) {
                Text(stringResource(R.string.alphabet_duplicate_label))
            }
            Text(stringResource(R.string.alphabet_stroke_count, pending.paths.size))
            Row(horizontalArrangement = Arrangement.spacedBy(BUILDER_BUTTON_GAP_DP.dp)) {
                Button(
                    enabled = !state.busy && state.labelIsValid && state.labelIsUnique && state.baselineY > 0f,
                    onClick = actions.onAdd,
                ) {
                    Text(stringResource(R.string.alphabet_add_glyph))
                }
                OutlinedButton(enabled = !state.busy, onClick = actions.onDiscard) {
                    Text(stringResource(R.string.alphabet_discard_crop))
                }
            }
        }
    }
}

@Composable
private fun PageGlyphSelectionCanvas(
    binary: BinaryGrid,
    bitmap: ImageBitmap,
    selection: PixelBounds?,
    enabled: Boolean,
    onSelection: (PixelBounds?) -> Unit,
) {
    var dragStart by remember(binary) { mutableStateOf<Offset?>(null) }
    var dragEnd by remember(binary) { mutableStateOf<Offset?>(null) }
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(SELECTION_PREVIEW_HEIGHT_DP.dp)
                .pointerInput(binary, enabled) {
                    if (enabled) {
                        detectDragGestures(
                            onDragStart = { point ->
                                val raster = viewToRaster(point, size.width.toFloat(), size.height.toFloat(), binary.width, binary.height)
                                onSelection(null)
                                dragStart = raster
                                dragEnd = raster
                            },
                            onDrag = { change, _ ->
                                dragEnd = viewToRaster(change.position, size.width.toFloat(), size.height.toFloat(), binary.width, binary.height)
                                change.consume()
                            },
                            onDragEnd = {
                                val first = dragStart
                                val second = dragEnd
                                onSelection(if (first != null && second != null) selectionBounds(first, second, binary.width, binary.height) else null)
                            },
                        )
                    }
                },
    ) {
        val scale = minOf(size.width / binary.width, size.height / binary.height)
        val imageWidth = binary.width * scale
        val imageHeight = binary.height * scale
        val left = (size.width - imageWidth) / 2f
        val top = (size.height - imageHeight) / 2f
        drawImage(
            bitmap,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(imageWidth.roundToInt(), imageHeight.roundToInt()),
        )
        selection?.let { bounds ->
            drawRect(
                color = Color(SELECTION_COLOR),
                topLeft = Offset(left + bounds.left * scale, top + bounds.top * scale),
                size =
                    androidx.compose.ui.geometry
                        .Size(bounds.width * scale, bounds.height * scale),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(SELECTION_STROKE_PX),
            )
        }
        val start = dragStart
        val end = dragEnd
        if (start != null && end != null && selection == null) {
            drawRect(
                color = Color(SELECTION_COLOR),
                topLeft = Offset(left + minOf(start.x, end.x) * scale, top + minOf(start.y, end.y) * scale),
                size =
                    androidx.compose.ui.geometry
                        .Size(kotlin.math.abs(end.x - start.x) * scale, kotlin.math.abs(end.y - start.y) * scale),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(SELECTION_STROKE_PX),
            )
        }
    }
}

@Composable
private fun GlyphRasterPreview(
    bitmap: ImageBitmap,
    rasterWidth: Int,
    rasterHeight: Int,
    paths: List<Polyline>,
    baselineY: Float,
) {
    Canvas(modifier = Modifier.fillMaxWidth().height(GLYPH_PREVIEW_HEIGHT_DP.dp)) {
        val scale = minOf(size.width / rasterWidth, size.height / rasterHeight)
        val imageWidth = rasterWidth * scale
        val imageHeight = rasterHeight * scale
        val left = (size.width - imageWidth) / 2f
        val top = (size.height - imageHeight) / 2f
        drawImage(
            bitmap,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(imageWidth.roundToInt(), imageHeight.roundToInt()),
        )
        paths.forEach { path ->
            if (path.points.size == 1) {
                val point = path.points.single()
                drawCircle(
                    color = Color(CENTERLINE_COLOR),
                    radius = CENTERLINE_POINT_RADIUS_PX,
                    center = Offset(left + point.x.toFloat() * scale, top + point.y.toFloat() * scale),
                )
            } else {
                path.points.zipWithNext().forEach { (first, second) ->
                    drawLine(
                        color = Color(CENTERLINE_COLOR),
                        start = Offset(left + first.x.toFloat() * scale, top + first.y.toFloat() * scale),
                        end = Offset(left + second.x.toFloat() * scale, top + second.y.toFloat() * scale),
                        strokeWidth = CENTERLINE_STROKE_PX,
                    )
                }
            }
        }
        val lineY = top + baselineY * scale
        drawLine(Color(REFERENCE_LINE_COLOR), Offset(left, lineY), Offset(left + imageWidth, lineY), strokeWidth = REFERENCE_LINE_STROKE_PX)
    }
}

@Composable
private fun ReviewedGlyphCard(
    glyph: ReviewedGlyph,
    state: ReviewedGlyphCardState,
    actions: ReviewedGlyphCardActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(BUILDER_CARD_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(BUILDER_GAP_DP.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(BUILDER_BUTTON_GAP_DP.dp)) {
                Image(
                    bitmap = remember(glyph.binary) { binaryBitmap(glyph.binary) },
                    contentDescription = stringResource(R.string.alphabet_crop_description, glyph.input.char),
                    modifier = Modifier.size(GLYPH_THUMBNAIL_DP.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(BUILDER_TEXT_GAP_DP.dp)) {
                    Text(stringResource(R.string.alphabet_glyph_summary, glyph.input.char, glyph.input.paths.size, glyph.rasterWidth, glyph.rasterHeight))
                    Text(stringResource(R.string.alphabet_baseline_hint, glyph.input.baselineY.roundToInt()))
                }
            }
            Slider(
                value = glyph.input.baselineY.toFloat(),
                enabled = state.enabled,
                onValueChange = actions.onBaselineChange,
                valueRange = 0f..glyph.rasterHeight.toFloat(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(BUILDER_BUTTON_GAP_DP.dp)) {
                OutlinedButton(enabled = state.enabled, onClick = actions.onCapReference) {
                    Text(stringResource(if (state.isCapReference) R.string.alphabet_cap_reference_selected else R.string.alphabet_cap_reference))
                }
                OutlinedButton(enabled = state.enabled, onClick = actions.onXHeightReference) {
                    Text(stringResource(if (state.isXHeightReference) R.string.alphabet_xheight_reference_selected else R.string.alphabet_xheight_reference))
                }
            }
            OutlinedButton(enabled = state.enabled, onClick = actions.onRemove) {
                Text(stringResource(R.string.alphabet_remove_glyph))
            }
        }
    }
}

private data class GlyphSetSaveRequest(
    val glyphs: List<RasterGlyphInput>,
    val name: String,
    val xHeightPx: Double,
    val capHeightPx: Double,
)

private data class GlyphSetSaveActions(
    val onSaved: () -> Unit,
    val onFailure: () -> Unit,
    val onComplete: () -> Unit,
)

private fun CoroutineScope.launchSaveGlyphSet(
    request: GlyphSetSaveRequest,
    store: LocalGlyphSetStore,
    actions: GlyphSetSaveActions,
) {
    launch {
        try {
            val normalized =
                withContext(Dispatchers.Default) {
                    normalizeGlyphSet(
                        glyphs = request.glyphs,
                        rasterMetrics = RasterFontMetrics(xHeightPx = request.xHeightPx, capHeightPx = request.capHeightPx),
                        options =
                            GlyphSetNormalizationOptions(
                                id = UUID.randomUUID().toString(),
                                name = request.name,
                                defaultStrokeWidthMm = DEFAULT_STROKE_WIDTH_MM,
                                createdAt = Instant.now().toString(),
                                targetCapHeightUnits = TARGET_CAP_HEIGHT_UNITS,
                            ),
                    )
                }
            withContext(Dispatchers.IO) { store.save(normalized) }
            actions.onSaved()
        } catch (_: IOException) {
            actions.onFailure()
        } catch (_: IllegalArgumentException) {
            actions.onFailure()
        } finally {
            actions.onComplete()
        }
    }
}

private data class ReviewedGlyphCardState(
    val isCapReference: Boolean,
    val isXHeightReference: Boolean,
    val enabled: Boolean,
)

private data class ReviewedGlyphCardActions(
    val onBaselineChange: (Float) -> Unit,
    val onCapReference: () -> Unit,
    val onXHeightReference: () -> Unit,
    val onRemove: () -> Unit,
)

private data class PendingGlyph(
    val binary: BinaryGrid,
    val paths: List<Polyline>,
)

private data class ReviewedGlyph(
    val input: RasterGlyphInput,
    val rasterWidth: Int,
    val rasterHeight: Int,
    val binary: BinaryGrid,
)

private fun binaryBitmap(binary: BinaryGrid): ImageBitmap {
    val pixels = IntArray(binary.width * binary.height) { index -> if (binary[index]) BLACK else WHITE }
    return Bitmap.createBitmap(pixels, binary.width, binary.height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private fun componentUnionBounds(components: List<InkComponent>): PixelBounds =
    PixelBounds(
        left = components.minOf { it.bounds.left },
        top = components.minOf { it.bounds.top },
        rightExclusive = components.maxOf { it.bounds.rightExclusive },
        bottomExclusive = components.maxOf { it.bounds.bottomExclusive },
    )

private fun viewToRaster(
    position: Offset,
    viewWidth: Float,
    viewHeight: Float,
    rasterWidth: Int,
    rasterHeight: Int,
): Offset {
    val scale = minOf(viewWidth / rasterWidth, viewHeight / rasterHeight)
    val left = (viewWidth - rasterWidth * scale) / 2f
    val top = (viewHeight - rasterHeight * scale) / 2f
    return Offset(
        ((position.x - left) / scale).coerceIn(0f, rasterWidth.toFloat()),
        ((position.y - top) / scale).coerceIn(0f, rasterHeight.toFloat()),
    )
}

private fun selectionBounds(
    first: Offset,
    second: Offset,
    rasterWidth: Int,
    rasterHeight: Int,
): PixelBounds? {
    val left = floor(minOf(first.x, second.x)).toInt().coerceIn(0, rasterWidth - 1)
    val top = floor(minOf(first.y, second.y)).toInt().coerceIn(0, rasterHeight - 1)
    val right = ceil(maxOf(first.x, second.x)).toInt().coerceIn(left + 1, rasterWidth)
    val bottom = ceil(maxOf(first.y, second.y)).toInt().coerceIn(top + 1, rasterHeight)
    return if (right > left && bottom > top) PixelBounds(left, top, right, bottom) else null
}

private fun isSupportedGfontLabel(value: String): Boolean {
    if (value.isBlank() || value.codePointCount(0, value.length) != 1) return false
    val codePoint = value.codePointAt(0)
    return codePoint in MIN_BMP_CODE_POINT..MAX_BMP_CODE_POINT &&
        !Character.isSurrogate(codePoint.toChar()) &&
        !Character.isWhitespace(codePoint) &&
        !Character.isISOControl(codePoint)
}

private const val BUILDER_PADDING_DP = 20
private const val BUILDER_GAP_DP = 12
private const val BUILDER_BUTTON_GAP_DP = 8
private const val BUILDER_TEXT_GAP_DP = 4
private const val BUILDER_CARD_PADDING_DP = 16
private const val SELECTION_PREVIEW_HEIGHT_DP = 320
private const val GLYPH_PREVIEW_HEIGHT_DP = 180
private const val GLYPH_THUMBNAIL_DP = 52
private const val SELECTION_STROKE_PX = 3f
private const val REFERENCE_LINE_STROKE_PX = 2f
private const val SELECTION_COLOR = 0xffffb300
private const val REFERENCE_LINE_COLOR = 0xffd32f2f
private const val CENTERLINE_COLOR = 0xff00796b
private const val CENTERLINE_STROKE_PX = 2f
private const val CENTERLINE_POINT_RADIUS_PX = 2f
private const val MIN_BMP_CODE_POINT = 1
private const val MAX_BMP_CODE_POINT = 0xffff
private const val DEFAULT_STROKE_WIDTH_MM = 0.35
private const val TARGET_CAP_HEIGHT_UNITS = 700.0
private const val EMPTY_REFERENCE = "—"
private const val BLACK = 0xff000000.toInt()
private const val WHITE = 0xffffffff.toInt()
