package com.example.media3uamp.data

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

fun Track.toMediaItem(albumId: String): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(Uri.parse(image))
        .build()
    return MediaItem.Builder()
        .setMediaId("track:$id")
        .setUri(Uri.parse(source))
        .setMediaMetadata(metadata)
        .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(Uri.parse(source)).build())
        .build()
}

fun Album.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(title)
        .setArtworkUri(artwork?.let { Uri.parse(it) })
        .build()
    return MediaItem.Builder()
        .setMediaId("album:$id")
        .setMediaMetadata(metadata)
        .build()
}

