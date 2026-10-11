package com.silentshutter.app.camera

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.silentshutter.app.ui.theme.CyberBg
import com.silentshutter.app.ui.theme.NeonCyan
import com.silentshutter.app.ui.theme.NeonGreen
import com.silentshutter.app.ui.theme.NeonYellow
import com.silentshutter.app.ui.theme.SilentShutterTheme
import com.silentshutter.app.ui.theme.TextMuted
import com.silentshutter.app.ui.theme.TextPrimary
import com.silentshutter.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

class SilentCameraActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilentShutterTheme {
                SilentCameraScreen(
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun SilentCameraScreen(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "完全無音撮影にはカメラ権限が必要です", Toast.LENGTH_SHORT).show()
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Surface(modifier = Modifier.fillMaxSize(), color = CyberBg) {
            Box(contentAlignment = Alignment.Center) {
                Text("カメラ権限をリクエスト中...", color = NeonCyan)
            }
        }
        return
    }

    // Camera State
    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoom by remember { mutableFloatStateOf(5f) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var showFlashEffect by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = CyberBg) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview View
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(cameraControl) {
                        detectTapGestures { offset ->
                            val cam = cameraControl ?: return@detectTapGestures
                            val factory = SurfaceOrientedMeteringPointFactory(
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                            val point = factory.createPoint(offset.x, offset.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            cam.cameraControl.startFocusAndMetering(action)
                        }
                    },
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .setFlashMode(flashMode)
                            .build()
                        imageCapture = capture

                        val selector = CameraSelector.Builder()
                            .requireLensFacing(cameraLensFacing)
                            .build()

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                ctx as androidx.lifecycle.LifecycleOwner,
                                selector,
                                preview,
                                capture
                            )
                            cameraControl = cam
                            cam.cameraInfo.zoomState.observe(ctx) { state ->
                                zoomRatio = state.zoomRatio
                                maxZoom = state.maxZoomRatio.coerceAtMost(8f)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "カメラ起動エラー: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                update = {
                    imageCapture?.flashMode = flashMode
                }
            )

            // Cyberpunk HUD Overlay Grid & Reticle
            CyberCameraHudOverlay()

            // Flash Screen Visual Feedback (Silent Shutter visual pulse)
            AnimatedVisibility(
                visible = showFlashEffect,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x9900F0FF))
                )
            }

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x80080C14))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }

                // Zero-Sound Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xB3080C14))
                        .border(1.dp, NeonGreen, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "0dB SILENT ENGAGED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Flash Mode Toggle
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x80080C14))
                ) {
                    val icon = when (flashMode) {
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        else -> Icons.Default.FlashOff
                    }
                    val tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) NeonYellow else TextSecondary
                    Icon(imageVector = icon, contentDescription = "Flash", tint = tint)
                }
            }

            // Bottom Controls Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color(0xB3080C14))
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom Slider HUD
                if (maxZoom > 1.2f) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1f", zoomRatio)}x",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(42.dp)
                        )
                        Slider(
                            value = zoomRatio,
                            onValueChange = { newZoom ->
                                zoomRatio = newZoom
                                cameraControl?.cameraControl?.setZoomRatio(newZoom)
                            },
                            valueRange = 1f..maxZoom,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color(0x3300F0FF)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Shutter & Lens Switch Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Empty placeholder or gallery icon
                    Spacer(modifier = Modifier.size(52.dp))

                    // Giant Neon Shutter Button (Completely Silent)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300F0FF))
                            .border(3.dp, NeonCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    if (isCapturing) return@detectTapGestures
                                    val capture = imageCapture ?: return@detectTapGestures

                                    isCapturing = true
                                    showFlashEffect = true

                                    scope.launch {
                                        delay(80)
                                        showFlashEffect = false
                                    }

                                    takeZeroShutterPhoto(
                                        context = context,
                                        imageCapture = capture,
                                        mainExecutor = ContextCompat.getMainExecutor(context),
                                        onSuccess = {
                                            isCapturing = false
                                            Toast.makeText(context, "無音で撮影・保存完了", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { err ->
                                            isCapturing = false
                                            Toast.makeText(context, "撮影失敗: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isCapturing) NeonGreen else NeonCyan)
                        )
                    }

                    // Flip Front/Back Camera
                    IconButton(
                        onClick = {
                            cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x66111827))
                            .border(1.dp, Color(0x4D00F0FF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CyberCameraHudOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
    ) {
        // Center Target Crosshair
        Box(
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.Center)
                .border(1.dp, Color(0x4D00F0FF), CircleShape)
        )

        // Corner Tech Brackets
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopStart)
                .border(2.dp, Color(0x6600F0FF), RoundedCornerShape(topStart = 6.dp))
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.TopEnd)
                .border(2.dp, Color(0x6600F0FF), RoundedCornerShape(topEnd = 6.dp))
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomStart)
                .border(2.dp, Color(0x6600F0FF), RoundedCornerShape(bottomStart = 6.dp))
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .border(2.dp, Color(0x6600F0FF), RoundedCornerShape(bottomEnd = 6.dp))
        )
    }
}

private fun takeZeroShutterPhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    mainExecutor: Executor,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val name = "SILENT_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SilentShutter")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    // CameraX captures image without playing shutter sound
    imageCapture.takePicture(
        outputOptions,
        mainExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSuccess()
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "撮影失敗")
            }
        }
    )
}
