@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.media3uamp.data

import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    val music: List<Track>
)

@Serializable
data class Track(
    val id: String,
    val title: String,
    val album: String,
    val artist: String,
    val genre: String? = null,
    val source: String,
    val image: String,
    val trackNumber: Int? = null,
    val totalTrackCount: Int? = null,
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

