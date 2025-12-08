package com.example.media3uamp.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.os.Bundle

private fun safeUri(s: String?): Uri? {
    if (s.isNullOrBlank()) return null
    val fixed = s.replace(" ", "%20")
    return try { Uri.parse(fixed) } catch (_: Exception) { null }
}

fun Track.toMediaItem(albumId: String): MediaItem {
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(safeUri(image))
        .setTrackNumber(trackNumber ?: 0)
        .setTotalTrackCount(totalTrackCount ?: 0)
    val extras = Bundle().apply {
        putInt("durationSec", (duration ?: 0L).toInt())
    }
    metadataBuilder.setExtras(extras)
    val metadata = metadataBuilder.build()
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

