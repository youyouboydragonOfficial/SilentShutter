package com.silentshutter.app

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
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import com.silentshutter.app.manager.ShutterMuteManager
import com.silentshutter.app.service.FloatingHudService
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
import com.silentshutter.app.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SilentShutterTheme {
                MainDashboardScreen(
                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onOpenDndSettings = { openDndSettings() },
                    onOpenOverlaySettings = { openOverlaySettings() },
                    onToggleFloatingHud = { enabled -> toggleFloatingHudService(enabled) }
                )
            }
        }
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
    onToggleFloatingHud: (Boolean) -> Unit
) {
    val isMasterEnabled by ShutterMuteManager.isMasterEnabled.collectAsState()
    val isCurrentlyMuted by ShutterMuteManager.isCurrentlyMuted.collectAsState()
    val isAccessibilityConnected by ShutterMuteManager.isAccessibilityServiceConnected.collectAsState()
    val activeCameraPackage by ShutterMuteManager.activeCameraPackage.collectAsState()
    val isFloatingHudEnabled by ShutterMuteManager.isFloatingHudEnabled.collectAsState()
    val targetPackages by ShutterMuteManager.targetPackages.collectAsState()
    val logs by ShutterMuteManager.logs.collectAsState()

    val context = LocalContext.current
    var hasDndPermission by remember { mutableStateOf(ShutterMuteManager.hasDndPermission()) }
    var showAdbDialog by remember { mutableStateOf(false) }
    var showAddPkgDialog by remember { mutableStateOf(false) }
    var showTargetList by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasDndPermission = ShutterMuteManager.hasDndPermission()
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
                    isMuted = isCurrentlyMuted
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

            // Engine Status & Permission Radar Card
            item {
                StatusRadarCard(
                    isAccessibilityConnected = isAccessibilityConnected,
                    hasDndPermission = hasDndPermission,
                    onOpenAccessibility = onOpenAccessibilitySettings,
                    onOpenDnd = onOpenDndSettings
                )
            }

            // Quick Control Center (Tiles & HUD & Testing)
            item {
                QuickControlsCard(
                    isCurrentlyMuted = isCurrentlyMuted,
                    isFloatingHudEnabled = isFloatingHudEnabled,
                    onInstantMuteToggle = { ShutterMuteManager.toggleInstantMute() },
                    onToggleFloatingHud = onToggleFloatingHud,
                    onShowAdbGuide = { showAdbDialog = true }
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
                    text = "SilentShutter v1.0.0 • Non-Root Camera Silencer",
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
            onDismiss = { showAdbDialog = false },
            onApplyCsc = {
                val success = ShutterMuteManager.toggleSamsungCscShutterSound(true)
                if (success) {
                    Toast.makeText(context, "Galaxy シャッター音を強制無効化しました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "書き込み失敗: ADBで権限を付与してください", Toast.LENGTH_LONG).show()
                }
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
}

@Composable
fun CyberHeader(
    isMasterEnabled: Boolean,
    isMuted: Boolean
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
                text = "NON-ROOT CAMERA SILENCER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
        }

        val badgeText = when {
            !isMasterEnabled -> "STANDBY"
            isMuted -> "MUTED"
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
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CyberCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Giant Neon Glowing Switch Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
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
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnabled) "ON" else "OFF",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = ringColor,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isEnabled) {
                    if (isMuted) "撮影中: シャッター音消音発動中" else "監視中: カメラ起動時に自動消音"
                } else {
                    "停止中: タップして無音化エンジンを起動"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) TextPrimary else TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatusRadarCard(
    isAccessibilityConnected: Boolean,
    hasDndPermission: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenDnd: () -> Unit
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
                    text = "ENGINE RADAR & PERMISSIONS",
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
                subtitle = "カメラの起動/終了を自動検知して消音します",
                isGranted = isAccessibilityConnected,
                buttonText = if (isAccessibilityConnected) "稼働中" else "設定を開く",
                onClick = onOpenAccessibility
            )

            HorizontalDivider(color = Color(0x1A00F0FF), modifier = Modifier.padding(vertical = 10.dp))

            // DND Item
            PermissionStatusRow(
                title = "マナーモード / DND 制御権限",
                subtitle = "シャッター音・システム音の完全消音に必要です",
                isGranted = hasDndPermission,
                buttonText = if (hasDndPermission) "許可済み" else "許可する",
                onClick = onOpenDnd
            )
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
                    fontSize = 14.sp,
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
    onToggleFloatingHud: (Boolean) -> Unit,
    onShowAdbGuide: () -> Unit
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
                        text = "即時消音テスト (Instant Mute)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isCurrentlyMuted) "現在ミュート中 (タップで復元)" else "タップして手動で即座に消音テスト",
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "カメラ起動中も画面の隅から瞬時に消音トグル",
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

            HorizontalDivider(color = Color(0x1A00F0FF), modifier = Modifier.padding(vertical = 12.dp))

            // Galaxy / ADB CSC Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Samsung Galaxy / ADB直接無音化",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "CSCカメラ設定を直接無効化するADBコマンド手順",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onShowAdbGuide,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple.copy(alpha = 0.2f),
                        contentColor = NeonPurple
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "案内/適用", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
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
                        text = "TARGET CAMERA APPS ($targetCount)",
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
                    text = "LIVE SYSTEM CONSOLE",
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
                    text = "> 待機中... カメライベントを検知するとここに表示されます",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    logs.take(8).forEach { entry ->
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
    onDismiss: () -> Unit,
    onApplyCsc: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberCard,
        title = {
            Text(
                text = "Galaxy & ADB 直接消音",
                color = NeonPurple,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Samsung Galaxyなどの端末では、ADBコマンドでカメラ設定の強制シャッター音フラグを直接OFFにできます（root化不要）。",
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
                        text = "adb shell pm grant com.silentshutter.app android.permission.WRITE_SECURE_SETTINGS\n\nまたは直接設定:\nadb shell settings put system csc_pref_camera_forced_shuttersound_key 0",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonCyan
                    )
                }
                Text(
                    text = "権限付与済みの場合は下のボタンで直接設定を適用できます。",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApplyCsc,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("直接消音を適用")
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
