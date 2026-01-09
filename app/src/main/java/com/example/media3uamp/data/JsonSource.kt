package com.example.media3uamp.data

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import javax.inject.Inject


internal class JsonSource @Inject constructor(
    private val repository: CatalogRepository
) : AbstractMusicSource() {
    companion object {
        const val ORIGINAL_ARTWORK_URI_KEY = "com.example.media3uamp.ORIGINAL_ARTWORK_URI"
    }

    private var catalog: List<MediaItem> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaItem> = catalog.iterator()
    override suspend fun load() {
        updateCatalog()?.let { updatedCatalog ->
            catalog = updatedCatalog
            state = STATE_INITIALIZED
        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    override fun search(query: String, extras: Bundle): List<MediaItem> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return catalog.filter { item ->
            val md = item.mediaMetadata
            (md.title?.toString()?.lowercase()?.contains(q) == true) ||
                (md.artist?.toString()?.lowercase()?.contains(q) == true) ||
                (md.albumTitle?.toString()?.lowercase()?.contains(q) == true)
        }
    }

    private suspend fun updateCatalog(): List<MediaItem>? {
        return withContext(Dispatchers.IO) {
            val albums = try {
                repository.getAlbums(force = false)
            } catch (_: Exception) {
                return@withContext null
            }

            albums.asSequence()
                .flatMap { album -> album.tracks.asSequence() }
                .map { track -> track.toMediaItem() }
                .toList()
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val artworkRemote = safeUri(image)
        val artworkUri = artworkRemote?.let { AlbumArtContentProvider.mapUri(it) }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .setTrackNumber(trackNumber ?: 0)
            .setTotalTrackCount(totalTrackCount ?: 0)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(
                Bundle().apply {
                    putString(ORIGINAL_ARTWORK_URI_KEY, artworkRemote?.toString())
                    val durationSec = (duration ?: 0L).coerceAtLeast(0L)
                    putInt("durationSec", durationSec.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    putLong("durationMs", durationSec * 1000L)
                }
            )
            .build()

        val mediaUri = safeUri(source)
        return MediaItem.Builder()
            .setMediaId("$MEDIA_ID_TRACK_PREFIX$id")
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(mediaUri)
                    .build()
            )
            .build()
    }

    private fun safeUri(s: String?): Uri? {
        if (s.isNullOrBlank()) return null
        val fixed = s.replace(" ", "%20")
        return try {
            fixed.toUri()
        } catch (_: Exception) {
            null
        }
    }
}
