package com.example.media3uamp.ui.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.toMediaItem
import kotlinx.coroutines.launch

class AlbumsViewModel(app: Application) : AndroidViewModel(app) {
    private val _albums = MutableLiveData<List<MediaItem>>()
    val albums: LiveData<List<MediaItem>> = _albums
    private val _fromNetwork = MutableLiveData<Boolean>()
    val fromNetwork: LiveData<Boolean> = _fromNetwork
    private val repo = CatalogRepository(getApplication())

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            val albums = repo.getAlbums(force).map { it.toMediaItem() }
            _albums.value = albums
            _fromNetwork.value = repo.wasLastLoadFromNetwork()
        }
    }

    fun refresh() = load(force = true)
}

