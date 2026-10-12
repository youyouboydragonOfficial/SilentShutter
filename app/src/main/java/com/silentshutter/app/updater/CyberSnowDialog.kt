package com.silentshutter.app.updater

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.silentshutter.app.ui.theme.CyberBg
import com.silentshutter.app.ui.theme.CyberCard
import com.silentshutter.app.ui.theme.CyberCardBorder
import com.silentshutter.app.ui.theme.NeonCyan
import com.silentshutter.app.ui.theme.NeonGreen
import com.silentshutter.app.ui.theme.NeonPurple
import com.silentshutter.app.ui.theme.NeonRed
import com.silentshutter.app.ui.theme.TextMuted
import com.silentshutter.app.ui.theme.TextPrimary
import com.silentshutter.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class Snowflake(
    var x: Float,
    var y: Float,
    val sizeSp: Float,
    val speed: Float,
    val driftSpeed: Float,
    val rotationSpeed: Float,
    var rotation: Float,
    val alpha: Float,
    val colorHex: Int
)

@Composable
fun CyberSnowUpdateDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateState by AppUpdateManager.state.collectAsState()

    // Initialize checking when dialog opens
    LaunchedEffect(Unit) {
        AppUpdateManager.checkForUpdates()
    }

    Dialog(
        onDismissRequest = {
            if (updateState !is AppUpdateManager.UpdateState.Checking &&
                updateState !is AppUpdateManager.UpdateState.Downloading) {
                AppUpdateManager.resetState()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE6080C14)),
            contentAlignment = Alignment.Center
        ) {
            // ❄️ Deep Snow Falling Animation Background (Canvas)
            DeepSnowfallCanvas()

            // Main Dialog Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CyberCard.copy(alpha = 0.95f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val state = updateState) {
                        is AppUpdateManager.UpdateState.Checking -> {
                            CheckingUpdatesView()
                        }
                        is AppUpdateManager.UpdateState.Available -> {
                            UpdateAvailableView(
                                release = state,
                                onUpdateClick = {
                                    scope.launch {
                                        AppUpdateManager.downloadAndInstallApk(context, state.apkUrl)
                                    }
                                },
                                onCancel = {
                                    AppUpdateManager.resetState()
                                    onDismiss()
                                }
                            )
                        }
                        is AppUpdateManager.UpdateState.UpToDate -> {
                            UpToDateView(
                                version = state.currentVersion,
                                onClose = {
                                    AppUpdateManager.resetState()
                                    onDismiss()
                                }
                            )
                        }
                        is AppUpdateManager.UpdateState.Downloading -> {
                            DownloadingView(progress = state.progress)
                        }
                        is AppUpdateManager.UpdateState.ReadyToInstall -> {
                            ReadyToInstallView(
                                onInstall = { AppUpdateManager.installApk(context, state.apkFile) },
                                onClose = {
                                    AppUpdateManager.resetState()
                                    onDismiss()
                                }
                            )
                        }
                        is AppUpdateManager.UpdateState.Error -> {
                            ErrorView(
                                errorMsg = state.message,
                                onRetry = {
                                    scope.launch { AppUpdateManager.checkForUpdates() }
                                },
                                onClose = {
                                    AppUpdateManager.resetState()
                                    onDismiss()
                                }
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

/**
 * ❄️ Authentic ❄ Snowflake Particle Falling Canvas
 * Renders beautiful ❄ emoji snowflakes with authentic ice-crystal colors,
 * gentle horizontal sine-wave drift, and soft rotational flutter.
 */
@Composable
fun DeepSnowfallCanvas() {
    val crystalColors = remember {
        listOf(
            0xFFF0F9FF.toInt(), // Pure Snow White
            0xFFBAE6FD.toInt(), // Soft Powder Ice
            0xFF7DD3FC.toInt(), // Sky Frost Crystal
            0xFF38BDF8.toInt(), // Vibrant Ice Blue
            0xFF67E8F9.toInt(), // Luminous Aqua Cyan
            0xFFE0F7FA.toInt()  // Shimmering Frost
        )
    }

    val snowflakes = remember {
        List(45) {
            Snowflake(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                sizeSp = Random.nextFloat() * 12f + 14f, // 14sp ~ 26sp
                speed = Random.nextFloat() * 0.0028f + 0.0012f,
                driftSpeed = Random.nextFloat() * 2f + 1f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 1.5f,
                rotation = Random.nextFloat() * 360f,
                alpha = Random.nextFloat() * 0.55f + 0.45f,
                colorHex = crystalColors.random()
            )
        }
    }

    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT
        }
    }

    var frameTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                frameTime = time
                snowflakes.forEach { flake ->
                    flake.y += flake.speed
                    flake.rotation += flake.rotationSpeed
                    if (flake.y > 1.05f) {
                        flake.y = -0.05f
                        flake.x = Random.nextFloat()
                    }
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val t = frameTime / 1_000_000_000.0f
        val nativeCanvas = drawContext.canvas.nativeCanvas

        snowflakes.forEach { flake ->
            val drift = sin((t * flake.driftSpeed + flake.y * 10f).toDouble()).toFloat() * 18f
            val px = flake.x * width + drift
            val py = flake.y * height

            val pixelSize = flake.sizeSp * density
            paint.textSize = pixelSize
            paint.color = flake.colorHex
            paint.alpha = (flake.alpha * 255).toInt().coerceIn(0, 255)

            nativeCanvas.save()
            nativeCanvas.translate(px, py)
            nativeCanvas.rotate(flake.rotation)
            // Draw ❄ snowflake emoji centered
            nativeCanvas.drawText("❄", 0f, pixelSize * 0.35f, paint)
            nativeCanvas.restore()
        }
    }
}

/**
 * 🌀 Cool Multi-Ring Cyberpunk Spinner
 */
@Composable
fun CyberSpinner(
    modifier: Modifier = Modifier,
    sizeDp: Int = 100
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyber_spinner")

    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer"
    )

    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Neon Cyan Arc Ring
        Canvas(modifier = Modifier.fillMaxSize().rotate(outerAngle)) {
            drawArc(
                color = NeonCyan,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Inner Matrix Green Arc Ring
        Canvas(modifier = Modifier.size((sizeDp * 0.65f).dp).rotate(innerAngle)) {
            drawArc(
                color = NeonGreen,
                startAngle = 45f,
                sweepAngle = 220f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Center Pulsing Core Dot
        Box(
            modifier = Modifier
                .size((sizeDp * 0.25f * pulseScale).dp)
                .clip(CircleShape)
                .background(NeonCyan)
        )
    }
}

@Composable
fun CheckingUpdatesView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        // Cool Cyber Spinner
        CyberSpinner(sizeDp = 110)

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "CHECKING FOR UPDATES...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "GitHub リポジトリの最新リリースを確認中\n(雪が深々と降る演出をお楽しみください)",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun UpdateAvailableView(
    release: AppUpdateManager.UpdateState.Available,
    onUpdateClick: () -> Unit,
    onCancel: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.2f))
                .border(2.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "新しいアップデートがあります！",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Text(
            text = "新バージョン: v${release.version}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 📦 次回更新容量バッジ
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x2200F0FF))
                .border(1.dp, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📦 次回更新容量: ",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = release.apkSize,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 日本語更新内容ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📝 日本語更新内容",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Release notes box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyberBg)
                .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = release.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = release.notes,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onUpdateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGreen,
                contentColor = CyberBg
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "アプリ内で今すぐ更新 (APK自動DL)",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onCancel) {
            Text("あとで", color = TextSecondary)
        }
    }
}

@Composable
fun UpToDateView(
    version: String,
    onClose: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.15f))
                .border(2.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "最新バージョンです",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Text(
            text = "現在のバージョン: v$version",
            fontSize = 13.sp,
            color = NeonCyan,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "SilentShutter は最新の状態で正常に稼働しています。",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("閉じる", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DownloadingView(
    progress: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        CyberSpinner(sizeDp = 90)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "DOWNLOADING APK...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = NeonCyan,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = NeonGreen,
            trackColor = CyberBg
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGreen,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "ダウンロード完了後、自動的にインストーラーが起動します",
            fontSize = 11.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun ReadyToInstallView(
    onInstall: () -> Unit,
    onClose: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(NeonGreen.copy(alpha = 0.2f))
                .border(2.dp, NeonGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ダウンロード完了！",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Text(
            text = "下のボタンを押してインストールを完了してください。",
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Button(
            onClick = onInstall,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = CyberBg),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("今すぐインストール", fontWeight = FontWeight.Black)
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(onClick = onClose) {
            Text("閉じる", color = TextSecondary)
        }
    }
}

@Composable
fun ErrorView(
    errorMsg: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(NeonRed.copy(alpha = 0.2f))
                .border(2.dp, NeonRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "更新チェック失敗",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Text(
            text = errorMsg,
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("再試行", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = CyberBg, contentColor = TextSecondary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("閉じる")
            }
        }
    }
}
