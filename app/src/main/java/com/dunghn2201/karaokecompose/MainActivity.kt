package com.dunghn2201.karaokecompose

import android.content.Context
import android.content.res.AssetManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HideStatusBar(window)
            KaraokeComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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
    val mediaPlayer by lazy { MediaPlayer() }
    var totalDuration by remember { mutableStateOf(0) }
    var currentTimeProgress by remember { mutableStateOf(0f) }
    var lyric by remember { mutableStateOf(Lyric(emptyList())) }
    var textActiveIndex by remember { mutableStateOf(0) }

    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        mediaPlayer.playMusic(context)
        totalDuration = mediaPlayer.duration
        lyric = context.readAssetsFile("lyrics.json")
        while (true) {
            currentTimeProgress = mediaPlayer.currentPosition.toFloat()
            delay(300)
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
                .height(300.dp),
            state = listState
        ) {
            items(lyric.data.size) { index ->
                val item = lyric.data[index]
                val textDisplay = item.lineLyric.joinToString(" ") { it.text }
                val textActive = item.lineLyric.filter {
                    it.time.toFloat() in 0F..(currentTimeProgress / 1000)
                }
                if (textActive.isNotEmpty()) {
                    textActiveIndex = index
                }
                val targetWord = textActive.joinToString(" ") { it.text }
                val startIndex = 0
                val endIndex =
                    (textDisplay.indexOf(targetWord) + targetWord.length).takeUnless { it == -1 }
                        ?: 0
                MultiStyleText(
                    text = textDisplay,
                    StyleRange(
                        startIndex,
                        endIndex,
                        Color.Red,
                        30f,
                        FontWeight.Bold
                    ),
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
            Text(text = totalDuration.toLong().toMinutesSecondsText(), color = Color.White)
        }
        Slider(
            modifier = Modifier.padding(horizontal = 20.dp),
            value = currentTimeProgress,
            onValueChange = {
                // Do nothing
            },
            valueRange = 0f..totalDuration.toFloat()
        )
    }

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
        println(e)
    }
}

@Composable
fun MultiStyleText(text: String, vararg styleRanges: StyleRange) {
    val annotatedString = buildAnnotatedString {
        var currentPosition = 0

        styleRanges.forEach { range ->
            val style = SpanStyle(
                color = range.textColor,
                fontSize = range.textSizeSp.sp,
                fontWeight = range.fontWeight
            )
            withStyle(style) {
                append(text.substring(currentPosition, range.endIndex))
            }
            currentPosition = range.endIndex
        }

        // Append the remaining text with the default style
        withStyle(SpanStyle()) {
            append(text.substring(currentPosition))
        }
    }

    Text(
        modifier = Modifier.fillMaxWidth(),
        text = annotatedString, style = TextStyle(
            fontSize = 30.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    )
}

fun Context.readAssetsFile(fileName: String): Lyric {
    val assets = assets.readAssetsFile(fileName)
    return Gson().fromJson(assets, Lyric::class.java)
}


private fun AssetManager.readAssetsFile(fileName: String): String =
    open(fileName).bufferedReader().use { it.readText() }

data class Lyric(
    @SerializedName("data")
    val data: List<Data>,
) {
    data class Data(
        @SerializedName("line_lyric")
        val lineLyric: List<LineLyric>,
    ) {
        data class LineLyric(
            @SerializedName("time")
            val time: String,
            @SerializedName("text")
            val text: String,
        )
    }
}

data class StyleRange(
    val startIndex: Int,
    val endIndex: Int,
    val textColor: Color,
    val textSizeSp: Float,
    val fontWeight: FontWeight
)k

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