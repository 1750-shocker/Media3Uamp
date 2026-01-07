package com.example.media3uamp.ui.detail

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.example.media3uamp.R
import com.example.media3uamp.databinding.FragmentAlbumDetailBinding

class AlbumDetailFragment : Fragment() {
    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!
    private val vm: AlbumDetailViewModel by viewModels()
    private lateinit var adapter: AlbumDetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        sharedElementReturnTransition = TransitionInflater.from(requireContext())
            .inflateTransition(android.R.transition.move)
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val albumId = requireArguments().getString("albumId") ?: return
        ViewCompat.setTransitionName(binding.cover, "album_${albumId}_cover")
        ViewCompat.setTransitionName(binding.title, "album_${albumId}_title")
        ViewCompat.setTransitionName(binding.artist, "album_${albumId}_artist")
        view.postDelayed({ startPostponedEnterTransition() }, 500)

        adapter = AlbumDetailAdapter { index, _ ->
            val args = Bundle().apply {
                putString("albumId", albumId)
                putInt("trackIndex", index)
            }
            findNavController().navigate(R.id.action_to_player, args)
        }
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        vm.header.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                val md = item.mediaMetadata
                binding.title.text = md.title ?: ""
                binding.artist.text = md.artist ?: ""
                if (md.artworkUri == null) {
                    binding.cover.setImageResource(R.drawable.album_placeholder)
                    startPostponedEnterTransition()
                } else {
                    Glide.with(binding.cover)
                        .load(md.artworkUri)
                        .placeholder(R.drawable.album_placeholder)
                        .into(
                            object : DrawableImageViewTarget(binding.cover) {
                                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                                    super.onResourceReady(resource, transition)
                                    startPostponedEnterTransition()
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    super.onLoadFailed(errorDrawable)
                                    startPostponedEnterTransition()
                                }
                            },
                        )
                }
            }
        }
        vm.tracks.observe(viewLifecycleOwner) { list ->
            binding.count.text = "${list.size} 首歌曲"
            adapter.submitList(list)
        }
        vm.load(albumId)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
