package com.example.media3uamp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.media3uamp.R
import com.example.media3uamp.databinding.FragmentAlbumDetailBinding

class AlbumDetailFragment : Fragment() {
    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!
    private val vm: AlbumDetailViewModel by viewModels()
    private lateinit var adapter: AlbumDetailAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val albumId = requireArguments().getString("albumId") ?: return
        adapter = AlbumDetailAdapter { index, _ ->
            val args = Bundle().apply {
                putString("albumId", albumId)
                putInt("trackIndex", index)
            }
            findNavController().navigate(R.id.action_to_player, args)
        }
        binding.recycler.adapter = adapter
        vm.header.observe(viewLifecycleOwner) { item ->
            if (item != null) {
                val md = item.mediaMetadata
                binding.title.text = md.title ?: ""
                binding.artist.text = md.artist ?: ""
                Glide.with(binding.cover).load(md.artworkUri).placeholder(R.drawable.album_placeholder).into(binding.cover)
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

