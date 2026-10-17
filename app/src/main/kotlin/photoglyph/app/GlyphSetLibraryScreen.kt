package photoglyph.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import photoglyph.core.export.GfontCodec
import photoglyph.core.model.GlyphSet
import java.io.IOException

/** Lists independent partial sets and exports each one separately; no set is completed or merged here. */
@Composable
internal fun GlyphSetLibraryScreen(
    store: LocalGlyphSetStore,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var glyphSets by remember { mutableStateOf<List<GlyphSet>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<Int?>(null) }
    var exportInProgress by remember { mutableStateOf(false) }
    var exportingSet by remember { mutableStateOf<GlyphSet?>(null) }
    val createDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_TYPE_ZIP)) { destination ->
            val glyphSet = exportingSet
            exportingSet = null
            if (destination != null && glyphSet != null) {
                exportInProgress = true
                exportMessage = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val output = context.contentResolver.openOutputStream(destination, WRITE_MODE)
                                ?: throw IOException("Could not open export destination")
                            output.use { it.write(GfontCodec.encode(glyphSet)) }
                        }
                        exportMessage = R.string.gfont_export_success
                    } catch (_: IOException) {
                        exportMessage = R.string.gfont_export_failed
                    } catch (_: SecurityException) {
                        exportMessage = R.string.gfont_export_failed
                    } catch (_: IllegalArgumentException) {
                        exportMessage = R.string.gfont_export_failed
                    } finally {
                        exportInProgress = false
                    }
                }
            }
        }

    LaunchedEffect(store) {
        try {
            glyphSets = withContext(Dispatchers.IO) { store.list() }
        } catch (_: IOException) {
            loadFailed = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(LIBRARY_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_GAP_DP.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) { Text(stringResource(R.string.library_back)) }
        }
        Text(stringResource(R.string.glyph_library_title))
        exportMessage?.let { Text(stringResource(it)) }
        when {
            loadFailed -> Text(stringResource(R.string.glyph_library_load_failed))
            glyphSets == null -> Text(stringResource(R.string.glyph_library_loading))
            glyphSets.isNullOrEmpty() -> Text(stringResource(R.string.glyph_library_empty))
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(LIBRARY_ITEM_GAP_DP.dp)) {
                    items(checkNotNull(glyphSets), key = GlyphSet::id) { glyphSet ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(LIBRARY_CARD_PADDING_DP.dp),
                                verticalArrangement = Arrangement.spacedBy(LIBRARY_TEXT_GAP_DP.dp),
                            ) {
                                Text(glyphSet.name)
                                Text(stringResource(R.string.glyph_count, glyphSet.glyphs.size))
                                val availableCharacters =
                                    glyphSet.glyphs.keys
                                        .sorted()
                                        .joinToString(" ")
                                Text(availableCharacters)
                                Button(
                                    enabled = !exportInProgress,
                                    onClick = {
                                        exportingSet = glyphSet
                                        createDocument.launch(suggestedGfontFilename(glyphSet.name))
                                    },
                                ) {
                                    Text(
                                        stringResource(
                                            if (exportInProgress) R.string.gfont_exporting else R.string.gfont_export,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun suggestedGfontFilename(name: String): String {
    val safeName = name.trim().replace(INVALID_FILENAME_CHARACTERS, "_").trim('_').ifBlank { DEFAULT_EXPORT_NAME }
    return "$safeName$GFONT_EXTENSION"
}

private const val MIME_TYPE_ZIP = "application/zip"
private const val WRITE_MODE = "w"
private const val LIBRARY_PADDING_DP = 20
private const val LIBRARY_ITEM_GAP_DP = 12
private const val LIBRARY_CARD_PADDING_DP = 16
private const val LIBRARY_TEXT_GAP_DP = 6
private const val DEFAULT_EXPORT_NAME = "PhotoGlyph"
private const val GFONT_EXTENSION = ".gfont"
private val INVALID_FILENAME_CHARACTERS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")
