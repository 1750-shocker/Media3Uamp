package com.example.media3uamp.ui.albums

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.session.SessionCommand
import com.example.media3uamp.playback.PlaybackClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import android.os.Bundle
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _albums = MutableLiveData<List<MediaItem>>()
    val albums: LiveData<List<MediaItem>> = _albums

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            if (force) {
                refreshInternal()
            } else {
                loadInternal()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshInternal()
        }
    }

    private suspend fun refreshInternal() {
        val browser = PlaybackClient.getBrowser(context)
        browser.sendCustomCommand(
            SessionCommand("refresh_catalog", Bundle.EMPTY),
            Bundle.EMPTY
        ).await()
        loadInternal()
    }

    private suspend fun loadInternal() {
        val browser = PlaybackClient.getBrowser(context)
        val result = browser.getChildren("root", 0, Int.MAX_VALUE, null).await()
        _albums.value = result.value ?: emptyList()
    }
}

