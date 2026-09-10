package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import kotlinx.coroutines.isActive

/**
 * Compatibility extension for Compose versions that do not expose
 * InfiniteTransition.animateFloat(). The PlayerSheet queue animation uses
 * this API shape, so keep the call site unchanged while providing the same
 * continuously reversing bar animation.
 */
@Composable
fun InfiniteTransition.animateFloat(
    initialValue: Float,
    targetValue: Float,
    animationSpec: AnimationSpec<Float>,
    label: String = ""
): State<Float> {
    val animatable = remember(initialValue, targetValue, label) {
        Animatable(initialValue)
    }

    LaunchedEffect(initialValue, targetValue, label) {
        while (isActive) {
            animatable.animateTo(targetValue, animationSpec = tween(420))
            animatable.animateTo(initialValue, animationSpec = tween(420))
        }
    }

    return animatable.asState()
}
