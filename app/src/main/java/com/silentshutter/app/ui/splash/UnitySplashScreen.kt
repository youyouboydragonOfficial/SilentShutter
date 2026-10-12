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

/**
 * Unity / ゲームスタジオ起動風 スプラッシュ画面
 * ゆっくり点灯（Fade In）し、ゆっくり消灯（Fade Out）してメイン画面へシームレスに遷移する。
 * 全体所要時間: 約2.6秒（「5秒以内に起動してほしい」の要件を確実に満たす）。
 * 画面タップによる即時スキップにも対応。
 */
@Composable
fun UnitySplashScreen(
    onSplashFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.95f) }

    LaunchedEffect(Unit) {
        // 1. ゆっくり点灯・フェードイン (1000ms)
        coroutineScope {
            launch {
                scaleAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                )
            }
            launch {
                alphaAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
                )
            }
        }

        // 2. スタジオロゴ静止鑑賞時間 (700ms)
        delay(700)

        // 3. ゆっくり消灯・フェードアウト (900ms)
        coroutineScope {
            launch {
                scaleAnim.animateTo(
                    targetValue = 1.04f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                )
            }
            launch {
                alphaAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                )
            }
        }

        // 4. メイン画面起動へコールバック (合計約2.6秒で完全起動)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020408)) // Unityのような漆黒のスタジオ背景
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                // タップで即座にスキップして起動可能
                onSplashFinished()
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
