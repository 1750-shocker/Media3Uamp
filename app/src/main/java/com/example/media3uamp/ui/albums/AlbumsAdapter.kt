package com.example.media3uamp.ui.albums

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.media3.common.MediaItem
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.media3uamp.R
import com.example.media3uamp.databinding.ItemAlbumBinding

class AlbumsAdapter(
    private val onClick: (item: MediaItem, cover: View, title: TextView, artist: TextView) -> Unit,
) :
    ListAdapter<MediaItem, AlbumsAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MediaItem) {
            val albumId = item.mediaId.substringAfter(":")
            ViewCompat.setTransitionName(binding.coverCard, "album_${albumId}_cover")
            ViewCompat.setTransitionName(binding.title, "album_${albumId}_title")
            ViewCompat.setTransitionName(binding.artist, "album_${albumId}_artist")

            val md = item.mediaMetadata
            binding.title.text = md.title ?: ""
            binding.artist.text = md.artist ?: ""
            binding.year.text = md.releaseYear?.toString() ?: ""
            Glide.with(binding.cover).load(md.artworkUri).placeholder(R.drawable.album_placeholder)
                .into(binding.cover)
            binding.card.setOnClickListener {
                onClick(
                    item,
                    binding.coverCard,
                    binding.title,
                    binding.artist
                )
            }
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

