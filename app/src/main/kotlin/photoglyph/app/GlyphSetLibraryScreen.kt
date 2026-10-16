package photoglyph.app

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import photoglyph.core.model.GlyphSet
import java.io.IOException

/** Lists independently stored partial sets; no set is completed or merged here. */
@Composable
internal fun GlyphSetLibraryScreen(
    store: LocalGlyphSetStore,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var glyphSets by remember { mutableStateOf<List<GlyphSet>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

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
                                Text(glyphSet.glyphs.keys.sorted().joinToString(" "))
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val LIBRARY_PADDING_DP = 20
private const val LIBRARY_ITEM_GAP_DP = 12
private const val LIBRARY_CARD_PADDING_DP = 16
private const val LIBRARY_TEXT_GAP_DP = 6
