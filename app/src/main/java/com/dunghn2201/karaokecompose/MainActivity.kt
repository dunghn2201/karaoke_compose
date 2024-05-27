package com.dunghn2201.karaokecompose

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dunghn2201.karaokecompose.ui.theme.Blue
import com.dunghn2201.karaokecompose.ui.theme.KaraokeComposeTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.round

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HideStatusBar(window)
            KaraokeComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    KaraokeComponent()
                }
            }
        }
    }
}

@Composable
fun KaraokeComponent() {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var totalDuration by remember { mutableIntStateOf(0) }
    var forceRefresh by remember { mutableStateOf(false) }
    var currentTimeProgress by remember { mutableFloatStateOf(0f) }
    var lyric by remember { mutableStateOf(Lyric(emptyList())) }
    var textActiveIndex by remember { mutableIntStateOf(0) }
    var resumeSaved by remember {
        mutableIntStateOf(0)
    }
    val playIconResource = if (mediaPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (resumeSaved == 0) {
            mediaPlayer.playMusic(context)
            lyric = context.readAssetsFile("lyrics.json")
        }
    }

    LaunchedEffect(key1 = mediaPlayer.isPlaying, key2 = forceRefresh) {
        if (mediaPlayer.isPlaying) {
            totalDuration = mediaPlayer.duration
            while (true) {
                currentTimeProgress = mediaPlayer.currentPosition.toFloat()
                delay(10)
            }
        }
    }
    LaunchedEffect(textActiveIndex) {
        if (textActiveIndex != 0) {
            listState.animateScrollToItem(textActiveIndex)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Blue),
        verticalArrangement = Arrangement.Center
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp), state = listState
        ) {
            items(lyric.data.size) { index ->
                val item = lyric.data[index]
                val textDisplay = item.lineLyric.joinToString(" ") { it.text }
                val timesCharacterF =
                    item.lineLyric.map { it.time.toDouble().roundBigDecimalToFloatWithPrecision() }
                val timesCharacterL = item.lineLyric.map {
                    it.time.convertToMilliseconds()
                }
                val currentTime =
                    (currentTimeProgress / 1000).toDouble().roundBigDecimalToFloatWithPrecision()
                val textActive = item.lineLyric.filter {
                    it.time.toFloat() in 0F..(currentTime)
                }
                if (textActive.isNotEmpty()) {
                    textActiveIndex = index
                }
                val shouldStart =
                    currentTime in timesCharacterF.first()..timesCharacterF.last()
                SmoothKaraokeText(
                    lineLyrics = item.lineLyric,
                    isPlaying = shouldStart && mediaPlayer.isPlaying
                )
            }
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .padding(top = 50.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = currentTimeProgress.toLong().toMinutesSecondsText(), color = Color.White)
            Image(painterResource(id = playIconResource),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clickableWithoutRipple {
                        val updatedState = !mediaPlayer.isPlaying
                        if (updatedState) {
                            mediaPlayer.resumeMusic(resumeSaved)
                            forceRefresh = true
                        } else {
                            mediaPlayer.pauseMusic { resumeSaved = it }
                            forceRefresh = false
                        }

                    })
            Text(text = totalDuration.toLong().toMinutesSecondsText(), color = Color.White)
        }

        Slider(
            modifier = Modifier.padding(horizontal = 20.dp),
            value = currentTimeProgress,
            onValueChange = {

            },
            valueRange = 0f..totalDuration.toFloat()
        )
    }
}

fun String.convertToMilliseconds(): Long {
    val parts = this.split(".")
    val minutes = parts[0].toLong()
    val milliseconds = parts[1].padEnd(3, '0').take(3).toLong()
    return ((minutes * 1000) + milliseconds)
}

fun Long.toMinutesSecondsText(): String {
    val second = this / 1000
    val minutes = second / 60
    val remainingSecond = second % 60
    return String.format("%02d:%02d", minutes, remainingSecond)
}

fun MediaPlayer.playMusic(context: Context) {
    try {
        setDataSource(context.getString(R.string.url_ve_dau_mai_toc_nguoi_thuong_instrument))
        prepare()
        start()
    } catch (e: Exception) {
        release()
        println("playMusic ex $e")
    }
}

fun MediaPlayer.pauseMusic(onSaveResumePoint: (Int) -> Unit) {
    try {
        println("///check 2 ${isPlaying}")
        pause()
        onSaveResumePoint.invoke(currentPosition)
    } catch (e: Exception) {
        release()
        println("pauseMusic ex $e")

    }
}

fun MediaPlayer.resumeMusic(resumePoint: Int) {
    try {
        seekTo(resumePoint)
        start()
    } catch (e: Exception) {
        release()
        println("resumeMusic ex $e")
    }
}

fun Context.readAssetsFile(fileName: String): Lyric {
    val assets = assets.readAssetsFile(fileName)
    return Gson().fromJson(assets, Lyric::class.java)
}

private fun AssetManager.readAssetsFile(fileName: String): String =
    open(fileName).bufferedReader().use { it.readText() }

data class Lyric(
    @SerializedName("data") val data: List<Data>,
) {
    data class Data(
        @SerializedName("line_lyric") val lineLyric: List<LineLyric>,
    ) {
        data class LineLyric(
            @SerializedName("time") val time: String,
            @SerializedName("text") val text: String,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KaraokeComposeTheme {
        KaraokeComponent()
    }
}

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

@Composable
fun SmoothKaraokeText(
    text: String,
    baseColor: Color = Color.White,
    highlightColor: Color = Color.Red,
    animationDuration: Long = 5000L,
    shouldStart: Boolean = false
) {
    val animatedColors = remember(text) {
        text.map { Animatable(baseColor) }
    }

    LaunchedEffect(key1 = text, key2 = shouldStart) {
        if (shouldStart) animatedColors.forEachIndexed { index, animatable ->
            launch {
                val singleCharDuration = animationDuration / text.length
                delay(index * singleCharDuration)
                animatable.animateTo(
                    targetValue = highlightColor,
                    animationSpec = tween(durationMillis = singleCharDuration.toInt())
                )
            }
        }
    }

    val animatedString = buildAnnotatedString {
        text.forEachIndexed { index, char ->
            withStyle(style = SpanStyle(color = animatedColors[index].value)) {
                append(char)
            }
        }
    }

    Text(
        text = animatedString,
        modifier = Modifier
            .wrapContentSize()
            .padding(3.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun SmoothKaraokeText(
    lineLyrics: List<Lyric.Data.LineLyric>,
    baseColor: Color = Color.White,
    highlightColor: Color = Color.Red,
    isPlaying: Boolean = false
) {
    val data = lineLyrics.mapIndexed { index, lyric ->
        val nextLyric = lineLyrics.getOrNull(index + 1)
        val totalTime = (nextLyric?.time?.convertToMilliseconds()
            ?: (lineLyrics.last().time.convertToMilliseconds() + 300L)) - lyric.time.convertToMilliseconds()
        "${lyric.text} " to totalTime
    }.map {
        val singleCharDuration = it.second / it.first.length
        it.first.map { text -> text to if (text == ' ') 0 else singleCharDuration }
    }.flatten()


    val text = data.map { it.first }.joinToString("").trim()

    val totalDuration =
        lineLyrics.last().time.convertToMilliseconds() - lineLyrics.first().time.convertToMilliseconds()
    val animatedColors = remember(text) {
        text.map { Animatable(baseColor) }
    }
    LaunchedEffect(Unit) {
        val log = data.map { it.second }.count().toLong()
        val log2 = data.map { it.first }.joinToString("").trim().length
        println("/// log $log")
        println("/// log2 $log2")
        println("/// totalDuration $totalDuration == text $text; ${text.length}")
    }
    LaunchedEffect(key1 = text, key2 = isPlaying) {
        if (isPlaying) animatedColors.forEachIndexed { index, animatable ->
            val targetData = data[index]
            launch {
                //  val singleCharDuration = totalDuration / text.length
                val singleCharDuration = targetData.second
                delay(index * singleCharDuration)
                animatable.animateTo(
                    targetValue = highlightColor,
                    animationSpec = tween(durationMillis = singleCharDuration.toInt())
                )
            }
        }
    }

    val animatedString = buildAnnotatedString {
        text.forEachIndexed { index, char ->
            withStyle(
                style = SpanStyle(
                    color = animatedColors[index].value
                )
            ) {
                append(char)
            }
        }
    }

    Text(
        text = animatedString,
        modifier = Modifier
            .fillMaxWidth()
            .padding(3.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        textAlign = TextAlign.Center
    )
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