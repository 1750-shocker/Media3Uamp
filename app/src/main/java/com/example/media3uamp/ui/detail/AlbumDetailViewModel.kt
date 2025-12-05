package com.example.media3uamp.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.toMediaItem
import kotlinx.coroutines.launch

class AlbumDetailViewModel(app: Application) : AndroidViewModel(app) {
    private val _tracks = MutableLiveData<List<MediaItem>>()
    val tracks: LiveData<List<MediaItem>> = _tracks
    private val _header = MutableLiveData<MediaItem?>()
    val header: LiveData<MediaItem?> = _header

    fun load(albumId: String) {
        viewModelScope.launch {
            val repo = CatalogRepository(getApplication())
            val album = repo.getAlbums().firstOrNull { it.id == albumId }
            val albumItem = album?.toMediaItem()
            _header.value = albumItem
            val tracks = repo.getTracks(albumId).map { it.toMediaItem(albumId) }
            _tracks.value = tracks
        }
    }
}

