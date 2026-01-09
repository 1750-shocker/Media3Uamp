package com.example.media3uamp.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.media3uamp.R
import com.example.media3uamp.databinding.FragmentAlbumsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlbumsFragment : Fragment() {
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private val vm: AlbumsViewModel by viewModels()
    private lateinit var adapter: AlbumsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AlbumsAdapter { album, cover, title, artist ->
            val albumId = album.mediaId.substringAfter(":")
            val action = R.id.action_to_albumDetail
            val extras = FragmentNavigatorExtras(
                cover to "album_${albumId}_cover",
                title to "album_${albumId}_title",
                artist to "album_${albumId}_artist",
            )
            findNavController().navigate(
                action,
                Bundle().apply { putString("albumId", albumId) },
                null,
                extras
            )
        }
        binding.recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { vm.refresh() }
        vm.albums.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.swipeRefresh.isRefreshing = false
        }
        vm.load()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

