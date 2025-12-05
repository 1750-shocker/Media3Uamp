package com.example.media3uamp.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

private fun safeUri(s: String?): Uri? {
    if (s.isNullOrBlank()) return null
    val fixed = s.replace(" ", "%20")
    return try { Uri.parse(fixed) } catch (_: Exception) { null }
}

fun Track.toMediaItem(albumId: String): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(safeUri(image))
        .build()
    return MediaItem.Builder()
        .setMediaId("track:$id")
        .setUri(safeUri(source))
        .setMediaMetadata(metadata)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(safeUri(source)).build())
        .build()
}

fun Album.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(title)
        .setArtworkUri(safeUri(artwork))
        .build()
    return MediaItem.Builder()
        .setMediaId("album:$id")
        .setMediaMetadata(metadata)
        .build()
}

