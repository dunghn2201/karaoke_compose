package com.dunghn2201.karaokecompose.model

import com.google.gson.annotations.SerializedName

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