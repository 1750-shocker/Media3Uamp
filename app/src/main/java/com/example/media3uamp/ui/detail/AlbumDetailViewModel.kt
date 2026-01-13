package com.example.media3uamp.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import com.example.media3uamp.playback.PlaybackConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val playbackConnectionManager: PlaybackConnectionManager,
) : ViewModel() {
    private val _tracks = MutableLiveData<List<MediaItem>>()
    val tracks: LiveData<List<MediaItem>> = _tracks
    private val _header = MutableLiveData<MediaItem?>()
    val header: LiveData<MediaItem?> = _header

    fun load(albumId: String) {
        viewModelScope.launch {
            val browser = playbackConnectionManager.getBrowser()
            val header = browser.getItem("album:$albumId").await().value
            _header.value = header
            val children = browser.getChildren("album:$albumId", 0, Int.MAX_VALUE, null).await()
            _tracks.value = children.value ?: emptyList()
        }
    }
}

