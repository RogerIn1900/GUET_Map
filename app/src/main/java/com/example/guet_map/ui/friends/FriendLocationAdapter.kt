package com.example.guet_map.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemFriendLocationBinding
import com.example.guet_map.model.FriendInfo
import com.example.guet_map.model.FriendLocation

data class FriendWithLocation(
    val friend: FriendInfo,
    val location: FriendLocation?
)

class FriendLocationAdapter(
    private val onItemClick: (FriendInfo, FriendLocation) -> Unit
) : ListAdapter<FriendWithLocation, FriendLocationAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFriendLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemFriendLocationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FriendWithLocation) {
            binding.tvNickname.text = item.friend.nickname

            binding.root.setOnClickListener {
                item.location?.let { location ->
                    onItemClick(item.friend, location)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FriendWithLocation>() {
        override fun areItemsTheSame(oldItem: FriendWithLocation, newItem: FriendWithLocation): Boolean {
            return oldItem.friend.userId == newItem.friend.userId
        }

        override fun areContentsTheSame(oldItem: FriendWithLocation, newItem: FriendWithLocation): Boolean {
            return oldItem == newItem
        }
    }
}
