package com.example.media3uamp.ui.player

import android.os.Bundle
import android.animation.ObjectAnimator
import android.view.animation.LinearInterpolator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.example.media3uamp.databinding.FragmentPlayerBinding
import com.example.media3uamp.playback.PlaybackClient
import com.example.media3uamp.data.CatalogRepository
import com.example.media3uamp.data.toMediaItem
import com.example.media3uamp.ui.view.PlayerViewController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private var player: Player? = null
    private var playerListener: Player.Listener? = null
    private var progressJob: Job? = null
    private var coverAnimator: ObjectAnimator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initCoverAnimator()
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
            _binding?.playerController?.setDurations(controller.currentPosition, controller.duration)
            updateCoverRotation(controller.isPlaying)
            playerListener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateMetadata(player)
                    _binding?.playerController?.setDurations(player.currentPosition, player.duration)
                    _binding?.playerController?.setPlaying(player.isPlaying)
                    updateCoverRotation(player.isPlaying)
                }
            }
            controller.addListener(playerListener!!)
            binding.playerController.setControllerListener(object :
                PlayerViewController.PlayerControllerListener {
                override fun onPlayToggle() { if (controller.isPlaying) controller.pause() else controller.play(); updateCoverRotation(controller.isPlaying) }
                override fun onPreviousClick() { controller.seekToPrevious() }
                override fun onNextClick() { controller.seekToNext() }
                override fun onSeekTo(progress: Int) { controller.seekTo(progress.toLong()) }
            })
            binding.playerController.setPlaying(controller.isPlaying)

            progressJob?.cancel()
            progressJob = CoroutineScope(Dispatchers.Main).launch {
                while (_binding != null) {
                    binding.playerController.setDurations(controller.currentPosition, controller.duration)
                    delay(1000)
                }
            }
        }
    }

    private fun initCoverAnimator() {
        if (coverAnimator == null) {
            coverAnimator = ObjectAnimator.ofFloat(binding.cover, "rotation", 0f, 360f)
            coverAnimator?.duration = 20000
            coverAnimator?.interpolator = LinearInterpolator()
            coverAnimator?.repeatCount = ObjectAnimator.INFINITE
            coverAnimator?.repeatMode = ObjectAnimator.RESTART
        }
    }

    private fun updateCoverRotation(isPlaying: Boolean) {
        val animator = coverAnimator ?: return
        if (isPlaying) {
            if (!animator.isStarted) animator.start() else animator.resume()
        } else {
            if (animator.isRunning) animator.pause()
        }
    }

    private fun updateMetadata(player: Player) {
        val b = _binding ?: return
        val md = player.mediaMetadata
        b.title.text = md.title ?: ""
        b.artist.text = md.artist ?: ""
        md.artworkUri?.let { Glide.with(b.cover).load(it).circleCrop().placeholder(com.example.media3uamp.R.drawable.album_placeholder).into(b.cover) }
    }

    override fun onDestroyView() {
        coverAnimator?.cancel()
        coverAnimator = null
        playerListener?.let { listener -> player?.removeListener(listener) }
        playerListener = null
        progressJob?.cancel()
        progressJob = null
        _binding = null
        super.onDestroyView()
    }
}

