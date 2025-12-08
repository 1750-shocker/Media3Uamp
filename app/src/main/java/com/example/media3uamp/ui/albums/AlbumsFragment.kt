package com.example.media3uamp.ui.albums

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.media3uamp.databinding.FragmentAlbumsBinding

class AlbumsFragment : Fragment() {
    private var _binding: FragmentAlbumsBinding? = null
    private val binding get() = _binding!!
    private val vm: AlbumsViewModel by viewModels()
    private lateinit var adapter: AlbumsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlbumsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AlbumsAdapter { album ->
            val albumId = album.mediaId.substringAfter(":")
            val action = com.example.media3uamp.R.id.action_to_albumDetail
            findNavController().navigate(action, android.os.Bundle().apply { putString("albumId", albumId) })
        }
        binding.recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { vm.refresh() }
        vm.albums.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.swipeRefresh.isRefreshing = false
        }
        /*vm.fromNetwork.observe(viewLifecycleOwner) { fromNet ->
            if (fromNet == true) {
                android.widget.Toast.makeText(requireContext(), "已加载远程数据", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(requireContext(), "当前使用本地兜底数据，请检查网络", android.widget.Toast.LENGTH_SHORT).show()
            }
        }*/
        vm.load()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

