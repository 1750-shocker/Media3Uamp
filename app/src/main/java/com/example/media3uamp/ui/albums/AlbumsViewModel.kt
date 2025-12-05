package com.example.media3uamp.ui.albums

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.media3uamp.playback.PlaybackClient
import kotlinx.coroutines.launch

class AlbumsViewModel(app: Application) : AndroidViewModel(app) {
    private val _albums = MutableLiveData<List<MediaItem>>()
    val albums: LiveData<List<MediaItem>> = _albums

    fun load() {
        viewModelScope.launch {
            val browser = PlaybackClient.getBrowser(getApplication())
            val result = browser.getChildren("ALBUMS", /*page=*/0, /*pageSize=*/Int.MAX_VALUE)
            _albums.value = result.value ?: emptyList()
        }
    }
}

