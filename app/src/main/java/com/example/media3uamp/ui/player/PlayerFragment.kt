package com.example.media3uamp.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.media3uamp.databinding.FragmentPlayerBinding
import com.example.media3uamp.playback.PlaybackClient
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.guava.await

class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private var player: Player? = null
    private var playerListener: Player.Listener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val albumId = requireArguments().getString("albumId") ?: return
        var index = requireArguments().getInt("trackIndex")
        CoroutineScope(Dispatchers.Main).launch {
            val controller = PlaybackClient.getController(requireContext())
            player = controller
            val repo = CatalogRepository(requireContext())
            val tracks = repo.getTracks(albumId).map { it.toMediaItem(albumId) }
            if (tracks.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "该专辑暂无曲目或数据加载失败", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (index !in tracks.indices) index = 0
            controller.setMediaItems(tracks)
            controller.seekToDefaultPosition(index)
            controller.prepare()
            controller.play()
            updateMetadata(controller)
            _binding?.let {
                it.timebar.setDuration(controller.duration)
                it.timebar.setPosition(controller.currentPosition)
            }
            playerListener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateMetadata(player)
                    _binding?.let { b ->
                        b.timebar.setDuration(player.duration)
                        b.timebar.setPosition(player.currentPosition)
                    }
                }
            }
            controller.addListener(playerListener!!)
            binding.btnPlay.setOnClickListener {
                if (controller.isPlaying) controller.pause() else controller.play()
            }
            binding.btnPrev.setOnClickListener { controller.seekToPrevious() }
            binding.btnNext.setOnClickListener { controller.seekToNext() }
            binding.btnShuffle.setOnClickListener { controller.shuffleModeEnabled = !controller.shuffleModeEnabled }
            binding.btnRepeat.setOnClickListener {
                val mode = (controller.repeatMode + 1) % 3
                controller.repeatMode = mode
            }
        }
    }

    private fun updateMetadata(player: Player) {
        val b = _binding ?: return
        val md = player.mediaMetadata
        b.title.text = md.title ?: ""
        b.artist.text = md.artist ?: ""
        md.artworkUri?.let { Glide.with(b.cover).load(it).placeholder(com.example.media3uamp.R.drawable.album_placeholder).into(b.cover) }
    }

    override fun onDestroyView() {
        playerListener?.let { listener -> player?.removeListener(listener) }
        playerListener = null
        _binding = null
        super.onDestroyView()
    }
}

