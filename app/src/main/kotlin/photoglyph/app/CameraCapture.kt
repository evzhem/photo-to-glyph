package photoglyph.app

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/** CameraX only captures a raster. All preparation remains Kotlin core. No plotter connection. */
@Composable
internal fun CameraCaptureDialog(
    onImage: (Uri, File) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    DisposableEffect(owner) {
        var disposed = false
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            if (!disposed) {
                try {
                    val cameras = future.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    cameras.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    provider = cameras
                    ready = true
                } catch (_: java.util.concurrent.ExecutionException) {
                    failed = true
                } catch (_: IllegalArgumentException) {
                    failed = true
                } catch (_: IllegalStateException) {
                    failed = true
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            provider?.unbindAll()
        }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(CAMERA_GAP_DP.dp)) {
                Text(stringResource(R.string.camera_title))
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxWidth().height(CAMERA_PREVIEW_DP.dp))
                if (failed) Text(stringResource(R.string.camera_failed))
                Button(enabled = ready && !busy, onClick = {
                    busy = true
                    takePhoto(context, capture, onSuccess = { uri, file ->
                        busy = false
                        onImage(uri, file)
                    }, onFailure = {
                        busy = false
                        failed = true
                    })
                }) { Text(stringResource(if (busy) R.string.camera_capturing else R.string.camera_capture)) }
                Button(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    capture: ImageCapture,
    onSuccess: (Uri, File) -> Unit,
    onFailure: () -> Unit,
) {
    val file = File.createTempFile("capture-", ".jpg", context.cacheDir)
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    capture.takePicture(
        output,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                onSuccess(Uri.fromFile(file), file)
            }

            override fun onError(exception: ImageCaptureException) {
                file.delete()
                onFailure()
            }
        },
    )
}

private const val CAMERA_PREVIEW_DP = 320
private const val CAMERA_GAP_DP = 12
