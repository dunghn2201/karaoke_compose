package com.dunghn2201.karaokecompose.model

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.ui.graphics.Color

data class SmoothTextItem(
    val text: String,
    val start: Float,
    val end: Float,
    val singleChars: List<SingleChar>,
) {

    data class SingleChar(
        val parentText: String,
        val char: Char,
        val duration: Long,
        val animatable: Animatable<Color, AnimationVector4D>,
    )
}