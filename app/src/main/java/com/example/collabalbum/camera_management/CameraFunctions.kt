
package com.example.collabalbum.camera_management

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Lens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.app.ComponentActivity


@Composable
fun CameraView(outputDirectory: File, executor: Executor, onImageCaptured: (Uri) -> Unit, onError: (ImageCaptureException) -> Unit)
{
    val lensFacing = CameraSelector.LENS_FACING_BACK
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }

    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())

        IconButton(
            modifier = Modifier.padding(bottom = 20.dp),
            onClick = {
                Log.i("tag", "ON CLICK")
                takePhoto(
                    filenameFormat = "yyyy-MM-dd-HH-mm-ss-SSS",
                    imageCapture = imageCapture,
                    outputDirectory = outputDirectory,
                    executor = executor,
                    onImageCaptured = onImageCaptured,
                    onError = onError
                )
            },
            content = {
                Icon(imageVector = Icons.Sharp.Lens, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(100.dp).padding(1.dp).border(1.dp, Color.White, CircleShape))
                }
        )
    }
}

fun takePhoto(filenameFormat: String, imageCapture: ImageCapture, outputDirectory: File, executor: Executor, onImageCaptured: (Uri) -> Unit, onError: (ImageCaptureException) -> Unit)
{
    val photoFile: File = File(outputDirectory, SimpleDateFormat(filenameFormat, Locale.ITALY).format(System.currentTimeMillis()) + ".jpg") //create photo file with the output directory + tstamp

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build() //build output file options before passing to takePicture below

    imageCapture.takePicture(outputOptions, executor, object: ImageCapture.OnImageSavedCallback
    {
        override fun onError(exception: ImageCaptureException)
        {
            onError(exception)
        }
        override fun onImageSaved(outputFileResults: OutputFileResults)
        {
            val savedUri = Uri.fromFile(photoFile) //get photo uri
            onImageCaptured(savedUri) //and passed onto the callback of onImageCapture
        }
    })
}


suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

fun checkCameraPermission(shouldShowCamera: MutableState<Boolean>, launcher: ActivityResultLauncher<String>, activity: ComponentActivity)
{
    when
    {
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ->
        {
            shouldShowCamera.value = true
        }

        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> {}

        else -> launcher.launch(Manifest.permission.CAMERA)
    }
}
