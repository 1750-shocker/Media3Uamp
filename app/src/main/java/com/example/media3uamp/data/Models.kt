package com.example.media3uamp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    @SerialName("music") val music: List<Track>
)

@Serializable
data class Track(
    val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val genre: String? = null,
    @SerialName("source") val source: String,
    @SerialName("image") val image: String,
    @SerialName("trackNumber") val trackNumber: Int? = null,
    @SerialName("totalTrackCount") val totalTrackCount: Int? = null,
    val duration: Long? = null,
    val site: String? = null,
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val artwork: String? = null,
    val tracks: List<Track>
)

