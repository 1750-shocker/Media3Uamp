package com.example.media3uamp.data

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

class BrowseTree(
    musicSource: MusicSource,
    recentMediaId: String? = null
) {
    private val childrenByParent = mutableMapOf<String, MutableList<MediaItem>>()
    private val itemById = mutableMapOf<String, MediaItem>()
    private val albumArtistByTitle = mutableMapOf<String, String>()

    val rootItem: MediaItem =
        MediaItem.Builder()
            .setMediaId(MEDIA_ID_ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("UAMP Library")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    init {
        itemById[rootItem.mediaId] = rootItem
        childrenByParent.getOrPut(MEDIA_ID_ROOT) { mutableListOf() }
        val rootChildren = childrenByParent.getValue(MEDIA_ID_ROOT)

        musicSource.forEach { trackItem ->
            val md = trackItem.mediaMetadata
            val albumTitle = md.albumTitle?.toString().orEmpty()
            val artist = md.artist?.toString().orEmpty()
            val albumArtist = albumArtistByTitle.getOrPut(albumTitle) { artist }
            val albumId = buildAlbumId(albumTitle, albumArtist)
            val albumMediaId = "$MEDIA_ID_ALBUM_PREFIX$albumId"

            val albumItem = itemById[albumMediaId] ?: run {
                val albumMetadata = MediaMetadata.Builder()
                    .setTitle(albumTitle)
                    .setArtist(albumArtist)
                    .setAlbumTitle(albumTitle)
                    .setArtworkUri(md.artworkUri)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
                MediaItem.Builder()
                    .setMediaId(albumMediaId)
                    .setMediaMetadata(albumMetadata)
                    .build()
            }.also { created ->
                if (!itemById.containsKey(albumMediaId)) {
                    itemById[albumMediaId] = created
                    rootChildren.add(created)
                }
            }

            val albumChildren = childrenByParent.getOrPut(albumItem.mediaId) { mutableListOf() }
            albumChildren.add(trackItem)
            itemById[trackItem.mediaId] = trackItem
            if (recentMediaId != null && trackItem.mediaId == recentMediaId) {
                itemById[MEDIA_ID_RECENT] = trackItem
            }
        }

        rootChildren.sortWith(compareBy({ it.mediaMetadata.title?.toString().orEmpty() }, { it.mediaMetadata.artist?.toString().orEmpty() }))
    }

    fun getChildren(parentId: String): List<MediaItem>? = childrenByParent[parentId]
    fun getItem(mediaId: String): MediaItem? = itemById[mediaId]
    fun isKnownId(id: String): Boolean = id == MEDIA_ID_ROOT || itemById.containsKey(id) || childrenByParent.containsKey(id)

    private fun buildAlbumId(albumTitle: String, artist: String): String =
        (albumTitle + "_" + artist).lowercase().replace(" ", "_")
}

const val MEDIA_ID_ROOT = "root"
const val MEDIA_ID_ALBUM_PREFIX = "album:"
const val MEDIA_ID_TRACK_PREFIX = "track:"
const val MEDIA_ID_RECENT = "recent"
