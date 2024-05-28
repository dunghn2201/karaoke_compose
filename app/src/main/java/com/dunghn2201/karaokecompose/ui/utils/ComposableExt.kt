package com.dunghn2201.karaokecompose.ui.utils

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaPlayer
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.dunghn2201.karaokecompose.R
import com.dunghn2201.karaokecompose.model.Lyric
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.gson.Gson
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale


@Composable
fun HideStatusBar(window: Window) {
    val systemUiController = rememberSystemUiController()
    SideEffect {
        with(window) {
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = true)
    }
}




inline fun Modifier.clickableWithoutRipple(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    crossinline onClick: () -> Unit,
): Modifier = composed {
    clickable(
        enabled = enabled,
        indication = null,
        onClickLabel = onClickLabel,
        role = role,
        interactionSource = remember { MutableInteractionSource() },
    ) {
        onClick()
    }
}

fun Double.roundBigDecimalToFloatWithPrecision(): Float {
    return BigDecimal(this).setScale(3, RoundingMode.HALF_EVEN).toFloat()
}


fun String.convertToMilliseconds(): Long {
    val parts = this.split(".")
    val second = parts[0].toLong()
    val milliseconds = parts[1].padEnd(3, '0').take(3).toLong()
    return ((second * 1000) + milliseconds)
}

fun Long.toMinutesSecondsText(): String {
    val second = this / 1000
    val minutes = second / 60
    val remainingSecond = second % 60
    return String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSecond)
}

fun MediaPlayer.playMusic(context: Context) {
    try {
        setDataSource(context.getString(R.string.url_ve_dau_mai_toc_nguoi_thuong_instrument))
        prepare()
        start()
    } catch (e: Exception) {
        release()
        println(e)
    }
}

fun MediaPlayer.pauseMusic(onSaveResumePoint: (Int) -> Unit) {
    try {
        pause()
        onSaveResumePoint.invoke(currentPosition)
    } catch (e: Exception) {
        release()
        println(e)

    }
}

fun MediaPlayer.resumeMusic(resumePoint: Int) {
    try {
        seekTo(resumePoint)
        start()
    } catch (e: Exception) {
        release()
        println(e)
    }
}

fun Context.readAssetsFile(fileName: String): Lyric {
    val assets = assets.readAssetsFile(fileName)
    return Gson().fromJson(assets, Lyric::class.java)
}

private fun AssetManager.readAssetsFile(fileName: String): String =
    open(fileName).bufferedReader().use { it.readText() }
