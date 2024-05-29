package com.dunghn2201.karaokecompose.model

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.ui.graphics.Color

data class SmoothTextItem(
    val text: String,
    val initial: Float,
    val start: Float,
    val end: Float,
    val totalTime: Long,
    val singleChars: List<SingleChar>,
) {

    fun isInCurrentTimeRange(currentTime: Float): Boolean = currentTime in start..end
    data class SingleChar(
        val parentText: String,
        val char: Char,
        val duration: Long,
        val animatable: Animatable<Color, AnimationVector4D>,
    )
}