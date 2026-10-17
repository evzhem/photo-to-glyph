package photoglyph.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val glyphSetStore = LocalGlyphSetStore(File(filesDir, GLYPH_SET_DIRECTORY))
        setContent {
            var showLibrary by remember { mutableStateOf(false) }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (showLibrary) {
                        GlyphSetLibraryScreen(glyphSetStore) { showLibrary = false }
                    } else {
                        PreparationScreen(store = glyphSetStore, onOpenLibrary = { showLibrary = true })
                    }
                }
            }
        }
    }

    private companion object {
        const val GLYPH_SET_DIRECTORY = "glyph-sets"
    }
}
