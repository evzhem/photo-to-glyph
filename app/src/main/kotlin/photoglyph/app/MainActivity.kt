package photoglyph.app

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import photoglyph.core.VectorizationResult
import photoglyph.core.img.ArgbImage
import photoglyph.core.vectorize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FoundationScreen()
                }
            }
        }
    }
}

@Composable
private fun FoundationScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<VectorizationResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(PAGE_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(SECTION_GAP.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.milestone_label), style = MaterialTheme.typography.labelMedium)
        Text(stringResource(R.string.headline), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.intro))
        Button(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true
                    failed = false
                    try {
                        result = withContext(Dispatchers.Default) {
                            context.assets.open(DEMO_ASSET).use { stream ->
                                val bitmap = checkNotNull(BitmapFactory.decodeStream(stream))
                                try {
                                    val pixels = IntArray(bitmap.width * bitmap.height)
                                    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                                    vectorize(ArgbImage(bitmap.width, bitmap.height, pixels))
                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }
                    } catch (_: IllegalStateException) {
                        failed = true
                    } catch (_: java.io.IOException) {
                        failed = true
                    } finally {
                        busy = false
                    }
                }
            },
        ) {
            Text(stringResource(if (busy) R.string.running_demo else R.string.run_demo))
        }
        if (failed) Text(stringResource(R.string.demo_failed))
        result?.let { data ->
            Text(
                stringResource(
                    R.string.demo_result,
                    data.strokes.polylines.size,
                    data.strokes.polylines.count { it.isClosed },
                ),
            )
            CorePreview(data)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(CARD_PADDING.dp), verticalArrangement = Arrangement.spacedBy(SECTION_GAP.dp)) {
                Text(stringResource(R.string.library_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.library_empty))
                Text(stringResource(R.string.export_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CorePreview(result: VectorizationResult) {
    // M0 raster coordinates are Y down; this is not normalized Glyph/em-box data.
    Canvas(modifier = Modifier.fillMaxWidth().height(PREVIEW_HEIGHT.dp)) {
        val scale = minOf(size.width / result.cleaned.width, size.height / result.cleaned.height)
        for (path in result.strokes.polylines) {
            if (path.points.size == 1) {
                val point = path.points.single()
                val offset = Offset(point.x.toFloat() * scale, point.y.toFloat() * scale)
                drawLine(Color.Black, offset, offset, strokeWidth = PREVIEW_STROKE_WIDTH, cap = StrokeCap.Round)
            }
            for ((start, end) in path.points.zipWithNext()) {
                drawLine(
                    Color.Black,
                    Offset(start.x.toFloat() * scale, start.y.toFloat() * scale),
                    Offset(end.x.toFloat() * scale, end.y.toFloat() * scale),
                    strokeWidth = PREVIEW_STROKE_WIDTH,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private const val DEMO_ASSET = "1046.png"
private const val PAGE_PADDING = 24
private const val CARD_PADDING = 16
private const val SECTION_GAP = 12
private const val PREVIEW_HEIGHT = 200
private const val PREVIEW_STROKE_WIDTH = 2.0f
