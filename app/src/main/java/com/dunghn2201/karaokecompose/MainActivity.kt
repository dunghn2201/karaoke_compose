package com.dunghn2201.karaokecompose

import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextPaint
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dunghn2201.karaokecompose.model.Lyric
import com.dunghn2201.karaokecompose.model.SmoothTextItem
import com.dunghn2201.karaokecompose.ui.theme.Blue
import com.dunghn2201.karaokecompose.ui.theme.KaraokeComposeTheme
import com.dunghn2201.karaokecompose.ui.utils.HideStatusBar
import com.dunghn2201.karaokecompose.ui.utils.clickableWithoutRipple
import com.dunghn2201.karaokecompose.ui.utils.convertToMilliseconds
import com.dunghn2201.karaokecompose.ui.utils.pauseMusic
import com.dunghn2201.karaokecompose.ui.utils.playMusic
import com.dunghn2201.karaokecompose.ui.utils.readAssetsFile
import com.dunghn2201.karaokecompose.ui.utils.resumeMusic
import com.dunghn2201.karaokecompose.ui.utils.roundBigDecimalToFloatWithPrecision
import com.dunghn2201.karaokecompose.ui.utils.toMinutesSecondsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
                val timesCharacterF =
                    item.lineLyric.map { it.time.toDouble().roundBigDecimalToFloatWithPrecision() }
                val currentTime =
                    (currentTimeProgress / 1000).toDouble().roundBigDecimalToFloatWithPrecision()
                val textActive = item.lineLyric.filter {
                    it.time.toFloat() in 0F..(currentTime)
                }
                if (textActive.isNotEmpty()) {
                    textActiveIndex = index
                }
                val shouldStart =
                    currentTime in timesCharacterF.first()..(timesCharacterF.last() + 400F)
                SmoothKaraokeTextTest(
                    currentTime = currentTime,
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KaraokeComposeTheme {
        KaraokeComponent()
    }
}

@Composable
fun SmoothKaraokeText(
    lineLyrics: List<Lyric.Data.LineLyric>,
    baseColor: Color = Color.White,
    highlightColor: Color = Color.Red,
    isPlaying: Boolean = false
) {
    val data = remember {
        lineLyrics.mapIndexed { index, lyric ->
            val initialLyric = lineLyrics.first()
            val nextLyric = lineLyrics.getOrNull(index + 1)
            val nextLyricTime = (nextLyric?.time?.toDouble()?.roundBigDecimalToFloatWithPrecision()
                ?: (lineLyrics.last().time.toDouble().roundBigDecimalToFloatWithPrecision() + 400F))
            val initialLyricTime =
                initialLyric.time.toDouble().roundBigDecimalToFloatWithPrecision()
            val currentLyricTime = lyric.time.toDouble().roundBigDecimalToFloatWithPrecision()
            val nextLyricMillis = (nextLyric?.time?.convertToMilliseconds()
                ?: (lineLyrics.last().time.convertToMilliseconds() + 400L))
            val currentLyricTimeMillis = lyric.time.convertToMilliseconds()
            val totalTime = nextLyricMillis - currentLyricTimeMillis
            val text = "${lyric.text} "
            val singleCharDuration = totalTime / text.length
            val singleChars = text.map { char ->
                SmoothTextItem.SingleChar(
                    parentText = text,
                    char = char,
                    duration = singleCharDuration,
                    animatable = Animatable(baseColor),
                )
            }
            SmoothTextItem(
                text = text,
                initial = initialLyricTime,
                start = currentLyricTime,
                end = nextLyricTime,
                totalTime = totalTime,
                singleChars = singleChars
            )
        }
    }

    val singleChars = remember {
        data.map { it.singleChars.map { char -> char } }.flatten()
    }

    val text = remember {
        singleChars.map { it.char }.joinToString("").trim()
    }

    LaunchedEffect(key1 = text, key2 = isPlaying) {
        if (isPlaying) {
            /**
             *  Get the duration text that has the active color set to calculate the delay for the current duration text.
             * */
            singleChars.forEachIndexed { targetIndex, triple ->
                val tempLyricTime =
                    singleChars.filterIndexed { index, _ -> index < targetIndex }
                        .map { it.duration }
                        .takeIf { it.isNotEmpty() }?.reduce { acc, l -> acc + l } ?: 0L
                val animatable = triple.animatable
                launch {
                    val singleCharDuration = triple.duration
                    delay(tempLyricTime + singleCharDuration)
                    animatable.animateTo(
                        targetValue = highlightColor,
                        animationSpec = tween(durationMillis = singleCharDuration.toInt())
                    )
                }
            }
        }
    }

    val animatedString = buildAnnotatedString {
        text.forEachIndexed { index, char ->
            withStyle(
                style = SpanStyle(
                    color = singleChars[index].animatable.value
                ),

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

@Composable
fun SmoothKaraokeTextTest(
    currentTime: Float,
    lineLyrics: List<Lyric.Data.LineLyric>,
    baseColor: Color = Color.White,
    highlightColor: Color = Color.Red,
    isPlaying: Boolean = false
) {

    val data = remember {
        lineLyrics.mapIndexed { index, lyric ->
            val initialLyric = lineLyrics.first()
            val nextLyric = lineLyrics.getOrNull(index + 1)
            val nextLyricTime = (nextLyric?.time?.toDouble()?.roundBigDecimalToFloatWithPrecision()
                ?: (lineLyrics.last().time.toDouble().roundBigDecimalToFloatWithPrecision() + 400F))
            val initialLyricTime =
                initialLyric.time.toDouble().roundBigDecimalToFloatWithPrecision()
            val currentLyricTime = lyric.time.toDouble().roundBigDecimalToFloatWithPrecision()
            val nextLyricMillis = (nextLyric?.time?.convertToMilliseconds()
                ?: (lineLyrics.last().time.convertToMilliseconds() + 400L))
            val currentLyricTimeMillis = lyric.time.convertToMilliseconds()
            val totalTime = nextLyricMillis - currentLyricTimeMillis
            val text = "${lyric.text} "
            val singleCharDuration = totalTime / text.length
            val singleChars = text.map { char ->
                SmoothTextItem.SingleChar(
                    parentText = text,
                    char = char,
                    duration = singleCharDuration,
                    animatable = Animatable(baseColor)
                )
            }
            SmoothTextItem(
                text = text,
                initial = initialLyricTime,
                start = currentLyricTime,
                end = nextLyricTime,
                singleChars = singleChars,
                totalTime = totalTime,
            )
        }
    }

    val singleChars = remember {
        data.map { it.singleChars.map { char -> char } }.flatten()
    }

    val text = remember {
        singleChars.map { it.char }.joinToString("").trim()
    }

    val density = LocalDensity.current
    val textSizePx = with(density) { 14f.sp.toPx() }

    val textPaint = remember {
        TextPaint().apply {
            color = baseColor.toArgb()
            textSize = textSizePx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    fun getTargetDatasByCurrentTime(currentTime: Float): List<SmoothTextItem> {
        val targetData = data.firstOrNull { it.isInCurrentTimeRange(currentTime) }
        val currentTargetIndex = data.indexOf(targetData)
        return data.filterIndexed { index, _ -> index <= currentTargetIndex }
    }

    val totalDuration = 10_000L
    //   ((lineLyrics.last().time.convertToMilliseconds() + 300L) - lineLyrics.first().time.convertToMilliseconds())

    val transition by remember { mutableStateOf(androidx.compose.animation.core.Animatable(0f)) }

    LaunchedEffect(key1 = text, key2 = isPlaying) {
        val targetsData = getTargetDatasByCurrentTime(currentTime)
        val targetData = data.firstOrNull { it.isInCurrentTimeRange(currentTime) }
        println("/// LaunchedEffect isPlaying $isPlaying")
        if (isPlaying && targetsData.isNotEmpty() && targetData != null) {
            launch {
                val duration = targetsData.map { it.totalTime * 10L }.reduce { acc, l -> acc + l } * 2L
              //  val duration = targetData.totalTime.toFloat()
                println("/// duration $duration == targetData ${targetData.text}")
                transition.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = duration.toInt())
                )
            }


            /*       */
            /**
             *  Get the duration text that has the active color set to calculate the delay for the current duration text.
             * *//*
            singleChars.forEachIndexed { targetIndex, triple ->
                val tempLyricTime =
                    singleChars.filterIndexed { index, _ -> index < targetIndex }
                        .map { it.duration }
                        .takeIf { it.isNotEmpty() }?.reduce { acc, l -> acc + l } ?: 0L
                val animatable = triple.animatable
                launch {
                    val singleCharDuration = triple.duration
                    delay(tempLyricTime + singleCharDuration)
                    animatable.animateTo(
                        targetValue = highlightColor,
                        animationSpec = tween(durationMillis = singleCharDuration.toInt())
                    )
                }
            }*/
        } else {
            transition.snapTo(0f)
        }
    }
    val highlightProgress = transition.value

    Canvas(
        modifier = Modifier
            .padding(16.dp)
            .wrapContentSize()
            .border(1.dp, Color.Red)
    ) {
        val textWidth = textPaint.measureText(text)
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val yOffset = textHeight - textPaint.fontMetrics.descent

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(text, 0f, yOffset, textPaint)
        }
        val highlightWidth = highlightProgress * textWidth
        val targetsData = getTargetDatasByCurrentTime(currentTime)
        val activeText = targetsData.joinToString("") { it.text }
        if (isPlaying && activeText.isNotEmpty()) {
            //  println("/// activeText $activeText == $highlightProgress" )
        }
        if (isPlaying && activeText.isNotEmpty()) {
            drawIntoCanvas { canvas ->
                val clipRect = Rect(0f, 0f, highlightWidth, textHeight)
                canvas.save()
                canvas.nativeCanvas.clipRect(clipRect.toAndroidRectF())
                textPaint.color = highlightColor.toArgb()
                canvas.nativeCanvas.drawText(activeText, 0f, yOffset, textPaint)
                textPaint.color = baseColor.toArgb() // Reset the color for the next draw
                canvas.restore()
            }
        }
    }
}


@Composable
fun SmoothKaraokeTextTest(
    text: String,
    baseColor: Color = Color.Gray,
    highlightColor: Color = Color.Red,
    animationDuration: Long = 20_000L,
    fontSize: Float = 15f
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { fontSize.sp.toPx() }

    val textPaint = remember {
        TextPaint().apply {
            color = baseColor.toArgb()
            textSize = textSizePx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
    }

    val transition = rememberInfiniteTransition(label = "")
    val highlightProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration.toInt(), easing = LinearEasing)
        ), label = ""
    )
    Canvas(
        modifier = Modifier
            .padding(16.dp)
            .wrapContentSize()
            .border(1.dp, Color.Red)
    ) {
        val textWidth = textPaint.measureText(text)
        val fontMetrics = textPaint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val yOffset = textHeight - textPaint.fontMetrics.descent

        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(text, 0f, yOffset, textPaint)
        }

        val highlightWidth = highlightProgress * textWidth

        drawIntoCanvas { canvas ->
            val clipRect = Rect(0f, 0f, highlightWidth, textHeight)
            canvas.save()
            canvas.nativeCanvas.clipRect(clipRect.toAndroidRectF())
            textPaint.color = highlightColor.toArgb()
            canvas.nativeCanvas.drawText(text, 0f, yOffset, textPaint)
            textPaint.color = baseColor.toArgb() // Reset the color for the next draw
            canvas.restore()
        }
    }

}
