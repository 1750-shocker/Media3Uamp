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
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.DrawableImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.color.MaterialColors
import com.example.media3uamp.R
import com.example.media3uamp.databinding.FragmentAlbumDetailBinding
import com.example.media3uamp.ui.player.PlayerFragment

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
        ViewCompat.setTransitionName(binding.coverCard, "album_${albumId}_cover")
        ViewCompat.setTransitionName(binding.title, "album_${albumId}_title")
        ViewCompat.setTransitionName(binding.artist, "album_${albumId}_artist")
        view.postDelayed({ startPostponedEnterTransition() }, 500)

        adapter = AlbumDetailAdapter(albumId) { index, item, title, artist ->
            val args = Bundle().apply {
                putString("albumId", albumId)
                putInt("trackIndex", index)
                putString("trackTitle", item.mediaMetadata.title?.toString())
                putString("trackArtist", item.mediaMetadata.artist?.toString())
            }
            val extras = FragmentNavigatorExtras(
                title to "track_${albumId}_${index}_title",
                artist to "track_${albumId}_${index}_artist",
            )
            val rootLoc = IntArray(2)
            val coverLoc = IntArray(2)
            binding.root.getLocationInWindow(rootLoc)
            binding.cover.getLocationInWindow(coverLoc)
            val left = (coverLoc[0] - rootLoc[0]).toFloat()
            val top = (coverLoc[1] - rootLoc[1]).toFloat()
            val right = left + binding.cover.width.toFloat()
            val bottom = top + binding.cover.height.toFloat()
            val fillColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)
            PlayerFragment.setBackgroundSnapshot(
                binding.root,
                listOf(
                    PlayerFragment.Companion.RoundedRectMask(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        radius = binding.coverCard.radius,
                        fillColor = fillColor,
                    ),
                ),
            )
            findNavController().navigate(R.id.action_to_player, args, null, extras)
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
