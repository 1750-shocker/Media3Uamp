package com.example.media3uamp.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.media3uamp.databinding.ItemTrackBinding

class AlbumDetailAdapter(private val onClick: (index: Int, item: MediaItem) -> Unit) :
    ListAdapter<MediaItem, AlbumDetailAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(position, getItem(position))
    }

    inner class VH(private val binding: ItemTrackBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(index: Int, item: MediaItem) {
            val md = item.mediaMetadata
            binding.index.text = (index + 1).toString()
            binding.title.text = md.title ?: ""
            binding.artist.text = md.artist ?: ""
            binding.duration.text = "" // 无准确时长，留空
            binding.card.setOnClickListener { onClick(index, item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
                oldItem.mediaId == newItem.mediaId

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
                oldItem == newItem
        }
    }
}

