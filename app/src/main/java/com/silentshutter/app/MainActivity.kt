package com.silentshutter.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silentshutter.app.hack.ShizukuSystemHacker
import com.silentshutter.app.manager.ShutterMuteManager
import com.silentshutter.app.service.FloatingHudService
import com.silentshutter.app.updater.CyberSnowUpdateDialog
import com.silentshutter.app.ui.theme.CyberBg
import com.silentshutter.app.ui.theme.CyberCard
import com.silentshutter.app.ui.theme.CyberCardBorder
import com.silentshutter.app.ui.theme.CyberSurface
import com.silentshutter.app.ui.theme.NeonCyan
import com.silentshutter.app.ui.theme.NeonGreen
import com.silentshutter.app.ui.theme.NeonPurple
import com.silentshutter.app.ui.theme.NeonRed
import com.silentshutter.app.ui.theme.NeonYellow
import com.silentshutter.app.ui.theme.SilentShutterTheme
import com.silentshutter.app.ui.theme.TextMuted
import com.silentshutter.app.ui.theme.TextPrimary
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import com.silentshutter.app.ui.splash.UnitySplashScreen
import com.silentshutter.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val requestRecordAudioLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "マイク権限を許可しました（通話ハードウェアロック有効）", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilentShutterTheme {
                var isSplashFinished by remember { mutableStateOf(false) }

                Crossfade(
                    targetState = isSplashFinished,
                    animationSpec = tween(400),
                    label = "SplashCrossfade"
                ) { finished ->
                    if (!finished) {
                        UnitySplashScreen(
                            onSplashFinished = {
                                isSplashFinished = true
                            }
                        )
                    } else {
                        MainDashboardScreen(
                            onOpenAccessibilitySettings = { openAccessibilitySettings() },
                            onOpenDndSettings = { openDndSettings() },
                            onOpenOverlaySettings = { openOverlaySettings() },
                            onRequestMicPermission = { requestRecordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            onToggleFloatingHud = { enabled -> toggleFloatingHudService(enabled) },
                            onLaunchDefaultCamera = { ShutterMuteManager.launchDefaultCamera() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuSystemHacker.checkShizukuState()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "ユーザー補助設定を開けませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openDndSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "通知へのアクセス設定を開けませんでした", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "オーバーレイ設定を開けませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleFloatingHudService(enable: Boolean) {
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                openOverlaySettings()
                Toast.makeText(this, "他のアプリの上に重ねて表示を許可してください", Toast.LENGTH_LONG).show()
                return
            }
            val intent = Intent(this, FloatingHudService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            ShutterMuteManager.setFloatingHudEnabled(true)
        } else {
            stopService(Intent(this, FloatingHudService::class.java))
            ShutterMuteManager.setFloatingHudEnabled(false)
        }
    }
}

@Composable
fun MainDashboardScreen(
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onToggleFloatingHud: (Boolean) -> Unit,
    onLaunchDefaultCamera: () -> Unit
) {
    val isMasterEnabled by ShutterMuteManager.isMasterEnabled.collectAsState()
    val isAlwaysForceMute by ShutterMuteManager.isAlwaysForceMute.collectAsState()
    val isAudioHijackEnabled by ShutterMuteManager.isAudioHijackEnabled.collectAsState()
    val isShutterTapInterceptEnabled by ShutterMuteManager.isShutterTapInterceptEnabled.collectAsState()
    val isCurrentlyMuted by ShutterMuteManager.isCurrentlyMuted.collectAsState()
    val isAccessibilityConnected by ShutterMuteManager.isAccessibilityServiceConnected.collectAsState()
    val detectedDefaultCamera by ShutterMuteManager.detectedDefaultCamera.collectAsState()
    val isFloatingHudEnabled by ShutterMuteManager.isFloatingHudEnabled.collectAsState()
    val targetPackages by ShutterMuteManager.targetPackages.collectAsState()
    val logs by ShutterMuteManager.logs.collectAsState()

    val isShizukuAvailable by ShizukuSystemHacker.isShizukuAvailable.collectAsState()
    val hasShizukuPermission by ShizukuSystemHacker.hasShizukuPermission.collectAsState()
    val hasSecureSettingsPermission by ShizukuSystemHacker.hasSecureSettingsPermission.collectAsState()

    val context = LocalContext.current
    var hasDndPermission by remember { mutableStateOf(ShutterMuteManager.hasDndPermission()) }
    var hasMicPermission by remember { mutableStateOf(ShutterMuteManager.hasRecordAudioPermission()) }
    var showAdbDialog by remember { mutableStateOf(false) }
    var showAddPkgDialog by remember { mutableStateOf(false) }
    var showTargetList by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasDndPermission = ShutterMuteManager.hasDndPermission()
        hasMicPermission = ShutterMuteManager.hasRecordAudioPermission()
        ShizukuSystemHacker.checkShizukuState()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberBg
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                CyberHeader(
                    isMasterEnabled = isMasterEnabled,
                    isMuted = isCurrentlyMuted,
                    onOpenUpdate = { showUpdateDialog = true }
                )
            }

            // ⚠️ Diagnostic Alert Banner (Shows if key permissions are missing)
            item {
                DiagnosticAlertBanner(
                    isAccessibilityConnected = isAccessibilityConnected,
                    hasDndPermission = hasDndPermission,
                    hasMicPermission = hasMicPermission,
                    onOpenAccessibility = onOpenAccessibilitySettings,
                    onOpenDnd = onOpenDndSettings,
                    onRequestMic = onRequestMicPermission
                )
            }

            // 🚨 Always Force Mute Mode (100% Silence Guaranteed)
            item {
                AlwaysForceMuteCard(
                    isAlwaysForceMute = isAlwaysForceMute,
                    onToggle = { ShutterMuteManager.setAlwaysForceMute(it) }
                )
            }

            // Mega Power Switch
            item {
                MegaPowerSwitch(
                    isEnabled = isMasterEnabled,
                    isMuted = isCurrentlyMuted,
                    onToggle = { ShutterMuteManager.setMasterEnabled(!isMasterEnabled) }
                )
            }

            // In-App Update Radar Card (Snow animation & cool spinner)
            item {
                CyberUpdateRadarCard(
                    onCheckUpdate = { showUpdateDialog = true }
                )
            }

            // Target Default Camera & Test Launch Card
            item {
                TargetDefaultCameraCard(
                    defaultCameraPkg = detectedDefaultCamera,
                    onLaunch = onLaunchDefaultCamera
                )
            }

            // Core Hack Modules
            item {
                HackModulesCard(
                    isAudioHijackEnabled = isAudioHijackEnabled,
                    isShutterTapEnabled = isShutterTapInterceptEnabled,
                    onToggleAudioHijack = { ShutterMuteManager.setAudioHijackEnabled(it) },
                    onToggleShutterTap = { ShutterMuteManager.setShutterTapInterceptEnabled(it) }
                )
            }

            // Shizuku & System Setting Deep Hacker
            item {
                ShizukuHackerCard(
                    isShizukuAvailable = isShizukuAvailable,
                    hasShizukuPermission = hasShizukuPermission,
                    hasSecureSettings = hasSecureSettingsPermission,
                    onRequestShizuku = { ShizukuSystemHacker.requestShizukuPermission() },
                    onInjectHacks = {
                        val results = ShizukuSystemHacker.injectAllSystemHacks()
                        Toast.makeText(context, results.joinToString("\n"), Toast.LENGTH_LONG).show()
                    },
                    onShowAdb = { showAdbDialog = true }
                )
            }

            // Engine Radar & Permission Status
            item {
                StatusRadarCard(
                    isAccessibilityConnected = isAccessibilityConnected,
                    hasDndPermission = hasDndPermission,
                    hasMicPermission = hasMicPermission,
                    onOpenAccessibility = onOpenAccessibilitySettings,
                    onOpenDnd = onOpenDndSettings,
                    onRequestMic = onRequestMicPermission
                )
            }

            // Quick Control Center (Tiles & HUD)
            item {
                QuickControlsCard(
                    isCurrentlyMuted = isCurrentlyMuted,
                    isFloatingHudEnabled = isFloatingHudEnabled,
                    onInstantMuteToggle = { ShutterMuteManager.toggleInstantMute() },
                    onToggleFloatingHud = onToggleFloatingHud
                )
            }

            // Target Apps Management
            item {
                TargetAppsCard(
                    targetCount = targetPackages.size,
                    isExpanded = showTargetList,
                    onToggleExpand = { showTargetList = !showTargetList },
                    onAddClick = { showAddPkgDialog = true }
                )
            }

            if (showTargetList) {
                items(targetPackages.toList()) { pkg ->
                    TargetAppRow(
                        packageName = pkg,
                        onDelete = { ShutterMuteManager.removeTargetPackage(pkg) }
                    )
                }
            }

            // Live Cyber Console Log
            item {
                CyberConsoleCard(logs = logs)
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "SilentShutter v2.3.0 • Default Camera Shutter Hacker",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ADB Guide Dialog
    if (showAdbDialog) {
        AdbGuideDialog(
            guideText = ShizukuSystemHacker.getAdbCommandGuide(),
            onDismiss = { showAdbDialog = false },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("ADB Command", ShizukuSystemHacker.getAdbCommandGuide()))
                Toast.makeText(context, "コマンドをクリップボードにコピーしました", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Add Target Package Dialog
    if (showAddPkgDialog) {
        AddPackageDialog(
            onDismiss = { showAddPkgDialog = false },
            onAdd = { pkg ->
                if (pkg.isNotBlank()) {
                    ShutterMuteManager.addTargetPackage(pkg.trim())
                    Toast.makeText(context, "対象アプリを追加しました", Toast.LENGTH_SHORT).show()
                }
                showAddPkgDialog = false
            }
        )
    }

    // In-App Cyber Snow Update Dialog
    if (showUpdateDialog) {
        CyberSnowUpdateDialog(
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
fun CyberUpdateRadarCard(
    onCheckUpdate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, NeonPurple.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "❄️",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IN-APP GITHUB UPDATER",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonPurple.copy(alpha = 0.2f))
                        .border(1.dp, NeonPurple, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AUTO-DL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "GitHubの最新リリースを確認し、アプリ内で直接APKをダウンロードして更新できます。(確認時は3秒間の雪の深々演出＆サイバースピナーが表示されます)",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onCheckUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "❄️ アプリ内で更新状況を確認する",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun CyberHeader(
    isMasterEnabled: Boolean,
    isMuted: Boolean,
    onOpenUpdate: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "SILENT SHUTTER",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = NeonCyan
            )
            Text(
                text = "DEFAULT CAMERA SHUTTER HACKER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Update Check Button
            Button(
                onClick = onOpenUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple.copy(alpha = 0.2f),
                    contentColor = NeonPurple
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "❄️ UPDATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            val badgeText = when {
                !isMasterEnabled -> "STANDBY"
                isMuted -> "HIJACKED"
                else -> "ARMED"
            }
            val badgeColor = when {
                !isMasterEnabled -> TextMuted
                isMuted -> NeonGreen
                else -> NeonCyan
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor
                    )
                }
            }
        }
    }
}

@Composable
fun MegaPowerSwitch(
    isEnabled: Boolean,
    isMuted: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activeColor = if (isMuted) NeonGreen else NeonCyan
    val ringColor by animateColorAsState(
        targetValue = if (isEnabled) activeColor else Color(0xFF334155),
        label = "ringColor"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Giant Neon Glowing Switch Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (isEnabled) pulseScale else 1.0f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ringColor.copy(alpha = if (isEnabled) 0.35f else 0.05f),
                                CyberCard.copy(alpha = 0.8f),
                                CyberBg
                            )
                        )
                    )
                    .border(3.dp, ringColor, CircleShape)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onToggle
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Master Power",
                        tint = ringColor,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnabled) "HACK ON" else "STANDBY",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = ringColor,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isEnabled) {
                    if (isMuted) "デフォルトカメラをハイジャック中: シャッター音遮断" else "監視中: 純正カメラ起動時に自動消音ハック発動"
                } else {
                    "停止中: タップしてハッカーエンジンを起動"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) TextPrimary else TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TargetDefaultCameraCard(
    defaultCameraPkg: String,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, NeonCyan.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TARGET DEFAULT CAMERA",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x3300F0FF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "DETECTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = defaultCameraPkg,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = NeonGreen
            )

            Text(
                text = "端末標準の純正カメラアプリです。下のボタンで起動してシャッター音の無音化をテストできます。",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = CyberBg
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Launch,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "デフォルトカメラを起動してテスト",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun HackModulesCard(
    isAudioHijackEnabled: Boolean,
    isShutterTapEnabled: Boolean,
    onToggleAudioHijack: (Boolean) -> Unit,
    onToggleShutterTap: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "HACK MODULES (ROOTLESS)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Module 1: Audio Route Hijacker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "通話モード & オーディオルーティング乗っ取り",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "カメラ起動中、OSの警告音パイプラインを通話専用パスへ迂回させスピーカ出力を遮断",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isAudioHijackEnabled,
                    onCheckedChange = onToggleAudioHijack,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberBg
                    )
                )
            }

            HorizontalDivider(color = Color(0x1A00F0FF), modifier = Modifier.padding(vertical = 12.dp))

            // Module 2: Shutter Tap Interceptor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "シャッター操作検知 ミリ秒パルス",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "純正カメラ内のシャッターボタンが押された瞬間を超高速検知し、瞬時消音パルスを発動",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isShutterTapEnabled,
                    onCheckedChange = onToggleShutterTap,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = NeonGreen.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberBg
                    )
                )
            }
        }
    }
}

@Composable
fun ShizukuHackerCard(
    isShizukuAvailable: Boolean,
    hasShizukuPermission: Boolean,
    hasSecureSettings: Boolean,
    onRequestShizuku: () -> Unit,
    onInjectHacks: () -> Unit,
    onShowAdb: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SHIZUKU & SYSTEM INJECTOR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple,
                        letterSpacing = 1.sp
                    )
                }

                val statusText = when {
                    hasShizukuPermission -> "SHIZUKU READY"
                    isShizukuAvailable -> "PERMISSION NEEDED"
                    hasSecureSettings -> "ADB SECURE ACTIVE"
                    else -> "STANDBY"
                }
                val statusColor = if (hasShizukuPermission || hasSecureSettings) NeonGreen else NeonPurple

                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Shizuku または ADB を通じて、Galaxy等の純正カメラシャッター音フラグ（CSCキー）やOEM設定をroot化なしで直接書き換えます。",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isShizukuAvailable && !hasShizukuPermission) {
                    Button(
                        onClick = onRequestShizuku,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Shizukuを許可", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onInjectHacks,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasShizukuPermission || hasSecureSettings) NeonPurple else Color(0x338B5CF6)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("システム設定を注入", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onShowAdb,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberSurface,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ADB案内", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatusRadarCard(
    isAccessibilityConnected: Boolean,
    hasDndPermission: Boolean,
    hasMicPermission: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenDnd: () -> Unit,
    onRequestMic: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PERMISSIONS & RADAR",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Accessibility Item
            PermissionStatusRow(
                title = "ユーザー補助サービス (必須)",
                subtitle = "純正カメラの起動＆シャッター操作をリアルタイム検知",
                isGranted = isAccessibilityConnected,
                buttonText = if (isAccessibilityConnected) "稼働中" else "設定を開く",
                onClick = onOpenAccessibility
            )

            HorizontalDivider(color = Color(0x1A00F0FF), modifier = Modifier.padding(vertical = 10.dp))

            // DND Item
            PermissionStatusRow(
                title = "マナーモード / DND 制御権限",
                subtitle = "システム警告音ストリーム(7)の完全消音に必要",
                isGranted = hasDndPermission,
                buttonText = if (hasDndPermission) "許可済み" else "許可する",
                onClick = onOpenDnd
            )

            HorizontalDivider(color = Color(0x1A00F0FF), modifier = Modifier.padding(vertical = 10.dp))

            // Microphone Permission Item
            PermissionStatusRow(
                title = "マイク権限 (通話ハードウェアロック)",
                subtitle = "Androidのスピーカー保護による強制シャッター音の完全遮断に必要",
                isGranted = hasMicPermission,
                buttonText = if (hasMicPermission) "許可済み" else "許可する",
                onClick = onRequestMic
            )
        }
    }
}

/**
 * 🚨 Always Force Mute Mode Card
 * ユーザーがONにすると、カメラ起動検知に関係なく端末全体を100%完全無音化する。
 */
@Composable
fun AlwaysForceMuteCard(
    isAlwaysForceMute: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlwaysForceMute) Color(0x2E00E676) else Color(0x1AFF0055)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.5.dp,
            if (isAlwaysForceMute) NeonGreen else NeonRed.copy(alpha = 0.8f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAlwaysForceMute) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = if (isAlwaysForceMute) NeonGreen else NeonRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAlwaysForceMute) "常時強制無音化 【発動中】" else "常時強制無音化モード (最強消音)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAlwaysForceMute) NeonGreen else TextPrimary
                        )
                        Text(
                            text = if (isAlwaysForceMute) "全オーディオ・シャッター音ストリームを完全封殺中" else "タップしてONにするとカメラの音が100%鳴りません",
                            fontSize = 11.sp,
                            color = if (isAlwaysForceMute) NeonGreen.copy(alpha = 0.9f) else TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isAlwaysForceMute,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = NeonGreen.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberBg
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isAlwaysForceMute)
                    "✅ 現在、カメラ自動検知に関係なく端末の全強制音（STREAM 7）とスピーカーが完全遮断されています。写真を撮り終えたらOFFにしてください。"
                else
                    "💡 「カメラのシャッター音が消えない」「自動検知が効かない」ときは、このスイッチをONにしてください！カメラ起動の検知なしで、端末のシャッター音を物理的に100%確実に消音します。",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * ⚠️ Diagnostic Alert Banner
 * 権限不足で音が鳴ってしまう原因をユーザーに通知し、ワンタップで許可させる。
 */
@Composable
fun DiagnosticAlertBanner(
    isAccessibilityConnected: Boolean,
    hasDndPermission: Boolean,
    hasMicPermission: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenDnd: () -> Unit,
    onRequestMic: () -> Unit
) {
    if (isAccessibilityConnected && hasDndPermission && hasMicPermission) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, NeonRed)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeonRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚠️ 【重要】音が鳴ってしまう原因と解決策",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonRed
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "以下の権限がOFFになっていると、OSがカメラシャッター音を強制再生してしまいます。ボタンを押してONにしてください：",
                fontSize = 11.sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (!isAccessibilityConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("• ユーザー補助サービス (必須)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("純正カメラアプリの起動を検知するために必要", fontSize = 10.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onOpenAccessibility,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("ONにする", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!hasDndPermission) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("• 通知の管理 / DND権限", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("システム強制音ストリームを完全遮断するために必要", fontSize = 10.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onOpenDnd,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("許可する", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!hasMicPermission) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("• マイク権限 (通話ハードウェアロック)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("通話保護仕様でスピーカー音を物理遮断するために必要", fontSize = 10.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = onRequestMic,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("許可する", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isGranted) NeonGreen else NeonYellow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0x2600E676) else NeonCyan.copy(alpha = 0.2f),
                contentColor = if (isGranted) NeonGreen else NeonCyan
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickControlsCard(
    isCurrentlyMuted: Boolean,
    isFloatingHudEnabled: Boolean,
    onInstantMuteToggle: () -> Unit,
    onToggleFloatingHud: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "QUICK CONTROLS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Instant Mute Test Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "即時ハイジャックテスト",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isCurrentlyMuted) "現在ハイジャック中 (タップで復元)" else "タップして通話モードハイジャックを即時テスト",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onInstantMuteToggle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyMuted) NeonRed.copy(alpha = 0.2f) else NeonGreen.copy(alpha = 0.2f),
                        contentColor = if (isCurrentlyMuted) NeonRed else NeonGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentlyMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCurrentlyMuted) "解除" else "テスト",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = Color(0x1A00F0FF), modifier = Modifier.padding(vertical = 12.dp))

            // Floating HUD Overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "フローティングHUD (画面端ボタン)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "画面端のボタンから瞬時に消音トグル / 長押しで純正カメラ起動",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isFloatingHudEnabled,
                    onCheckedChange = { onToggleFloatingHud(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.4f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CyberBg
                    )
                )
            }
        }
    }
}

@Composable
fun TargetAppsCard(
    targetCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CyberCard),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TARGET CAMERA PACKAGES ($targetCount)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Pixel, Galaxy, Xperia, LINE, インスタ等",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = NeonCyan
                        )
                    }
                    Button(
                        onClick = onToggleExpand,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberSurface,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "閉じる" else "一覧表示",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TargetAppRow(
    packageName: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = packageName,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = NeonRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CyberConsoleCard(
    logs: List<ShutterMuteManager.LogEntry>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF04060A)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0x3300F0FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE HACK CONSOLE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "MONITORING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "> 待機中... 純正カメラ起動またはシャッター操作時にログが表示されます",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    logs.take(10).forEach { entry ->
                        Text(
                            text = "${entry.timestamp} ${entry.message}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (entry.isMuteEvent) NeonGreen else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdbGuideDialog(
    guideText: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCard,
        title = {
            Text(
                text = "ADB コマンド案内",
                color = NeonPurple,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PCまたはワイヤレスデバッグから以下のADBコマンドを実行すると、Galaxyの純正カメラシャッター音フラグの直接無効化と特権自己付与が行われます。",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBg)
                        .padding(10.dp)
                ) {
                    Text(
                        text = guideText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onCopy,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("コマンドをコピー")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddPackageDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var pkgInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCard,
        title = {
            Text(
                text = "対象カメラアプリを追加",
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "無音化したいアプリのパッケージ名を入力してください。(例: com.sample.camera)",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = pkgInput,
                    onValueChange = { pkgInput = it },
                    placeholder = { Text("com.example.app", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color(0x3300F0FF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(pkgInput) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBg)
            ) {
                Text("追加", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", color = TextSecondary)
            }
        }
    )
}
