package com.example.guet_map.ui.friends

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.guet_map.databinding.ItemFriendBinding
import com.example.guet_map.model.FriendInfo

class FriendAdapter(
    private val onItemClick: (FriendInfo) -> Unit,
    private val onItemLongClick: (FriendInfo) -> Unit = {}
) : ListAdapter<FriendInfo, FriendAdapter.FriendViewHolder>(FriendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: FriendInfo) {
            binding.tvNickname.text = friend.nickname
            binding.tvEmail.text = friend.email

            // TODO: 加载头像图片
            // if (!friend.avatar.isNullOrEmpty()) {
            //     Glide.with(binding.ivAvatar).load(friend.avatar).into(binding.ivAvatar)
            // }

            binding.root.setOnClickListener {
                onItemClick(friend)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(friend)
                true
            }
        }
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<FriendInfo>() {
        override fun areItemsTheSame(oldItem: FriendInfo, newItem: FriendInfo): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: FriendInfo, newItem: FriendInfo): Boolean {
            return oldItem == newItem
        }
    }
}
