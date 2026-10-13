package com.silentshutter.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.silentshutter.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Unity / ゲームスタジオ起動風 スプラッシュ画面
 * 高速かつ滑らかな点灯（Fade In 350ms）→ 鑑賞（250ms）→ 消灯（Fade Out 300ms）。
 * 画面タップで0秒即時スキップ。
 * セーフティタイマー（1200ms）により画面が消えない・固まる不具合を物理防止。
 */
@Composable
fun UnitySplashScreen(
    onSplashFinished: () -> Unit
) {
    var hasFinished by remember { mutableStateOf(false) }

    fun finishSplash() {
        if (!hasFinished) {
            hasFinished = true
            onSplashFinished()
        }
    }

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.96f) }

    // メインアニメーションフロー (合計約900msでスピーディに完了)
    LaunchedEffect(Unit) {
        try {
            // 1. スムーズ点灯・フェードイン (350ms)
            coroutineScope {
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                    )
                }
                launch {
                    alphaAnim.animateTo(
                        targetValue = 1.0f,
                        animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
                    )
                }
            }

            // 2. スタジオロゴ静止鑑賞時間 (250ms)
            delay(250)

            // 3. スムーズ消灯・フェードアウト (300ms)
            coroutineScope {
                launch {
                    scaleAnim.animateTo(
                        targetValue = 1.03f,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    alphaAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    )
                }
            }
        } catch (_: Exception) {
            // 例外やキャンセル時も確実にスキップ
        } finally {
            finishSplash()
        }
    }

    // 🔒 セーフティタイムアウト: 最大1200ms経過で強制的にメイン画面へ遷移（画面が残る問題を徹底防止）
    LaunchedEffect(Unit) {
        delay(1200)
        finishSplash()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020408)) // Unityのような漆黒のスタジオ背景
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // 画面タップで即座に0秒スキップして即起動
                finishSplash()
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = "YouYouBoyDragon Studio Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
                .padding(16.dp)
        )
    }
}
